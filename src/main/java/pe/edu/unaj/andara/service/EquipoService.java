package pe.edu.unaj.andara.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.unaj.andara.exception.NegocioException;
import pe.edu.unaj.andara.model.*;
import pe.edu.unaj.andara.repository.EquipoRepository;
import pe.edu.unaj.andara.repository.PrestamoActivoRepository;
import pe.edu.unaj.andara.repository.PrestamoRepository;
import pe.edu.unaj.andara.util.Fechas;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

@Service
@RequiredArgsConstructor
public class EquipoService {

    public static final Map<String, String> DURACIONES = new LinkedHashMap<>();

    static {
        DURACIONES.put("2h", "2 horas lectivas");
        DURACIONES.put("4h", "4 horas lectivas");
        DURACIONES.put("day", "Jornada completa");
    }
    public static final List<String[]> DURACIONES_LARGAS = List.of(
            new String[]{"2h", "Franja corta (hasta 2 horas lectivas)"},
            new String[]{"4h", "Franja estándar (hasta 4 horas lectivas)"},
            new String[]{"day", "Jornada completa"});
    public static final List<String[]> PRESETS = List.of(new String[]{"now", "Disponibles ahora"},
            new String[]{"fav", "Mis favoritos"}, new String[]{"fac", "De una escuela"},
            new String[]{"many", "3 unidades o más"});
    public static final List<String> TIPOS_INCIDENCIA = List.of("Falla de funcionamiento", "Daño físico",
            "Pérdida o extravío de accesorio", "Otro");

    private final CatalogoService catalogo;
    private final ContextoService contexto;
    private final EquipoRepository equipos;
    private final PrestamoRepository prestamos;
    private final PrestamoActivoRepository activos;
    private final SesionService sesion;
    private final FavoritoService favoritos;
    private final ConexionService conexion;
    private final NotificacionService notificaciones;

    @Transactional(readOnly = true)
    public List<Equipo> enContexto() {
        return catalogo.equipos().stream().filter(e -> contexto.incluye(e.getSede(), e.getEscuela())).toList();
    }

    public int disponiblesEnCategoria(List<Equipo> pool, CategoriaEquipo c) {
        return pool.stream().filter(e -> e.getCategoria().getCodigo().equals(c.getCodigo()))
                .mapToInt(e -> Math.max(0, e.getStock())).sum();
    }

    public Map<String, Predicate<Equipo>> predicados() {
        java.util.Set<String> fav = favoritos.codigos(TipoFavorito.EQUIPO);
        return Map.of("now", Equipo::isDisponibleAhora, "fav", e -> fav.contains(e.getCodigo()),
                "fac", e -> e.getEscuela() != null, "many", e -> e.getStock() >= 3);
    }

    @Transactional
    public Prestamo solicitar(Equipo equipo, LocalDate fecha, String hora, String duracion, String motivo, boolean acepta) {
        if (fecha == null) {
            throw new NegocioException("Elige la fecha.");
        }
        if (fecha.isBefore(Fechas.hoy())) {
            throw new NegocioException("Elige hoy o una fecha posterior.");
        }
        if (hora == null || hora.isBlank()) {
            throw new NegocioException(equipo.isPorTurno() ? "Elige un turno." : "Elige la hora de retiro.");
        }
        if (motivo == null || motivo.trim().length() < 10) {
            throw new NegocioException("Escribe al menos 10 caracteres.");
        }
        if (!acepta) {
            throw new NegocioException("Debes aceptar el reglamento para continuar.");
        }
        Equipo e = equipos.findById(equipo.getId()).orElseThrow();
        if (!e.isPorTurno()) {
            if (e.getStock() <= 0) {
                throw new NegocioException("Sin unidades disponibles.");
            }
            e.setStock(e.getStock() - 1);
            equipos.save(e);
        }
        String codigo = nuevoCodigo();
        Prestamo p = prestamos.save(Prestamo.builder().codigo(codigo).clave("h" + UUID.randomUUID().toString().substring(0, 6))
                .persona(sesion.estudiante()).equipo(e).nombre(e.getNombre())
                .detalle("Solicitud registrada para el " + Fechas.dmy(fecha) + " (" + hora + ") • "
                        + DURACIONES.getOrDefault(duracion, "4 horas lectivas") + " • Sede " + e.getSede().getNombre())
                .estado(EstadoPrestamo.EN_REVISION).sede(e.getSede()).build());
        notificaciones.notificarAdministrador("Nueva solicitud de préstamo " + codigo + ": " + e.getNombre() + ".", "a-prestamos");
        conexion.encolar(e.isPorTurno() ? "Reservar turno: " + e.getNombre() : "Solicitar préstamo: " + e.getNombre());
        return p;
    }

    private String nuevoCodigo() {
        int n = 460 + (int) prestamos.count();
        String c;
        do {
            c = "EQ-2026-0" + n++;
        } while (prestamos.existsByCodigo(c));
        return c;
    }

    @Transactional(readOnly = true)
    public List<Prestamo> historial() {
        return prestamos.findByPersonaOrderByIdDesc(sesion.estudiante());
    }

    @Transactional(readOnly = true)
    public Prestamo prestamo(Long id) {
        return prestamos.findById(id).orElseThrow(() -> new NegocioException("El préstamo no existe."));
    }

    @Transactional
    public boolean eliminar(Long id) {
        Prestamo p = prestamo(id);
        boolean activo = p.getEstado().isEnCurso();
        if (activo && p.getEquipo() != null && !p.getEquipo().isPorTurno()) {
            Equipo e = equipos.findById(p.getEquipo().getId()).orElseThrow();
            e.setStock(Math.min(e.getUnidadesIniciales(), e.getStock() + 1));
            equipos.save(e);
        }
        prestamos.delete(p);
        conexion.encolar(activo ? "¿Cancelar esta solicitud de préstamo?" : "¿Eliminar del historial?");
        return activo;
    }

    @Transactional(readOnly = true)
    public Optional<PrestamoActivo> activo() {
        return activos.findFirstByOrderByIdDesc().filter(a -> !a.isDevuelto());
    }

    @Transactional(readOnly = true)
    public Optional<PrestamoActivo> custodia() {
        return activos.findFirstByOrderByIdDesc();
    }

    @Transactional
    public PrestamoActivo renovar() {
        PrestamoActivo a = activo().orElseThrow(() -> new NegocioException("No tienes préstamos activos."));
        if (a.isRenovado()) {
            throw new NegocioException("Solo puedes renovar una vez por préstamo.");
        }
        a.setLimite(a.getLimite().plusHours(2));
        a.setRenovado(true);
        conexion.encolar("Renovar tiempo de préstamo");
        return activos.save(a);
    }

    @Transactional
    public PrestamoActivo reportar(String tipo, String descripcion) {
        if (tipo == null || tipo.isBlank()) {
            throw new NegocioException("Elige el tipo de incidencia.");
        }
        if (descripcion == null || descripcion.trim().length() < 15) {
            throw new NegocioException("Escribe al menos 15 caracteres.");
        }
        PrestamoActivo a = activo().orElseThrow(() -> new NegocioException("No tienes préstamos activos."));
        a.setIncidencia(tipo);
        conexion.encolar("Reportar daño o falla");
        return activos.save(a);
    }

    public static String textoLimite(LocalDateTime limite, LocalDateTime ahora) {
        String hh = String.format("%02d:%02d hrs", limite.getHour(), limite.getMinute());
        Duration resta = Duration.between(ahora, limite);
        if (resta.isNegative()) {
            return "Hoy, " + hh + " (vencido)";
        }
        long h = resta.toHours();
        long m = resta.toMinutesPart();
        return "Hoy, " + hh + " (Restan " + h + "h " + String.format("%02d", m) + "m)";
    }
}
