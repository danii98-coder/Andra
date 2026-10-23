package pe.edu.unaj.andara.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.unaj.andara.exception.NegocioException;
import pe.edu.unaj.andara.model.*;
import pe.edu.unaj.andara.repository.*;
import pe.edu.unaj.andara.util.Fechas;
import pe.edu.unaj.andara.util.Montos;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AdminService {

    public record Dia(String etiqueta, BigDecimal monto, boolean hoy) {
    }

    public record Panel(BigDecimal hoy, BigDecimal mes, List<Expediente> porAtender, List<Reserva> reservasPendientes,
                        List<Prestamo> prestamosEnRevision, List<Dia> dias, Map<String, BigDecimal> porMetodo,
                        Map<String, BigDecimal> porConcepto, List<Pago> recientes) {
    }

    public static final List<String[]> PRESETS_TRAMITES = List.of(new String[]{"today", "Hoy"},
            new String[]{"d7", "Últimos 7 días"}, new String[]{"month", "Este mes"}, new String[]{"paid", "Pago confirmado"},
            new String[]{"unpaid", "Pago pendiente"}, new String[]{"carne", "Carné"});
    public static final List<String[]> PRESETS_PAGOS = List.of(new String[]{"today", "Hoy"},
            new String[]{"d7", "Últimos 7 días"}, new String[]{"month", "Este mes"}, new String[]{"yape", "Yape"},
            new String[]{"plin", "Plin"}, new String[]{"card", "Tarjeta"});
    public static final List<String[]> PRESETS_RESERVAS = List.of(new String[]{"today", "Para hoy"},
            new String[]{"d7", "Próximos 7 días"}, new String[]{"big", "100 personas o más"}, new String[]{"aud", "Auditorios"});
    public static final List<String[]> PRESETS_PRESTAMOS = List.of(new String[]{"rev", "En revisión"},
            new String[]{"apr", "Aprobados"}, new String[]{"back", "Devueltos"}, new String[]{"lab", "Instrumental de laboratorio"});

    private final ExpedienteRepository expedientes;
    private final PagoRepository pagos;
    private final ReservaRepository reservas;
    private final PrestamoRepository prestamos;
    private final PrestamoActivoRepository activos;
    private final NotificacionService notificaciones;
    private final SeguimientoService seguimiento;
    private final ContextoService contexto;
    private final CatalogoService catalogo;
    private final ConexionService conexion;

    @Transactional(readOnly = true)
    public List<Expediente> expedientes() {
        return expedientes.findAllByOrderByIdDesc().stream().filter(e -> contexto.incluyeSede(e.getSede())).toList();
    }

    @Transactional(readOnly = true)
    public List<Pago> pagos() {
        return pagos.findAllByOrderByCreadoDesc().stream().filter(p -> contexto.incluyeSede(p.getSede())).toList();
    }

    @Transactional(readOnly = true)
    public List<Reserva> reservas() {
        return reservas.findAllByOrderByIdDesc().stream().filter(r -> contexto.incluyeSede(r.getEspacio().getSede())).toList();
    }

    @Transactional(readOnly = true)
    public List<Prestamo> prestamos() {
        return prestamos.findAllByOrderByIdDesc().stream().filter(p -> contexto.incluyeSede(p.getSede())).toList();
    }

    @Transactional(readOnly = true)
    public Optional<PrestamoActivo> custodia() {
        return activos.findFirstByOrderByIdDesc().filter(a -> !a.isDevuelto())
                .filter(a -> contexto.todasLasSedes() || "capilla".equals(contexto.codigoSede()));
    }

    @Transactional(readOnly = true)
    public Panel panel() {
        LocalDate hoy = Fechas.hoy();
        List<Pago> pagados = pagos().stream().filter(p -> p.isPagado() && p.getPagado() != null).toList();
        BigDecimal deHoy = PagoService.total(pagados.stream().filter(p -> p.getPagado().toLocalDate().equals(hoy)).toList());
        BigDecimal delMes = PagoService.total(pagados.stream().filter(p -> p.getPagado().getYear() == hoy.getYear()
                && p.getPagado().getMonth() == hoy.getMonth()).toList());
        List<Expediente> porAtender = expedientes().stream().filter(e -> e.getGrupo() == GrupoExpediente.TRAMITE
                && !e.getEstado().contains("Pago pendiente")).toList();
        List<Reserva> resP = reservas().stream().filter(r -> r.getEstado() == EstadoReserva.PENDIENTE).toList();
        List<Prestamo> preP = prestamos().stream().filter(p -> p.getEstado() == EstadoPrestamo.EN_REVISION).toList();
        List<Dia> dias = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate d = hoy.minusDays(6 - i);
            String[] partes = Fechas.corto(d).split(" ");
            dias.add(new Dia(partes[0] + " " + partes[1], PagoService.total(pagados.stream()
                    .filter(p -> p.getPagado().toLocalDate().equals(d)).toList()), i == 6));
        }
        Map<String, BigDecimal> metodo = new LinkedHashMap<>();
        metodo.put("Yape", BigDecimal.ZERO);
        metodo.put("Tarjeta", BigDecimal.ZERO);
        metodo.put("Plin", BigDecimal.ZERO);
        pagados.forEach(p -> metodo.merge(p.getMetodo().getGrupo(), p.getTotal(), BigDecimal::add));
        Map<String, BigDecimal> concepto = new LinkedHashMap<>();
        pagados.forEach(p -> concepto.merge(p.getTipo().getEtiqueta(), p.getTotal(), BigDecimal::add));
        List<Pago> recientes = pagados.stream().sorted(Comparator.comparing(Pago::getPagado).reversed()).limit(6).toList();
        return new Panel(deHoy, delMes, porAtender, resP, preP, dias, metodo, concepto, recientes);
    }

    @Transactional(readOnly = true)
    public Expediente expediente(String codigo) {
        return expedientes.findByCodigo(codigo).orElseThrow(() -> new NegocioException("El expediente no existe."));
    }

    public static boolean puedeEmitir(Expediente e) {
        return e.getGrupo() == GrupoExpediente.TRAMITE && (e.getPago() == null || e.getPago().isPagado());
    }

    @Transactional
    public Expediente emitir(String codigo) {
        Expediente e = expediente(codigo);
        if (!puedeEmitir(e)) {
            throw new NegocioException(e.getPago() != null && !e.getPago().isPagado() ? "Esperando pago" : "Ya atendido");
        }
        e.setGrupo(GrupoExpediente.LISTO);
        e.setTono("success");
        e.setDestacado(false);
        e.setEstado("Documento emitido · Listo para recoger");
        e.setPaso(4);
        e.setRotuloIcono("LISTO");
        if (e.getPago() != null && e.getTramite() != null && e.getTramite().generaDocumento()) {
            e.ponerAcciones("primary", "gen-sol", "Ver documento generado", "secondary", "rcpt-sol", "Ver comprobante",
                    "tertiary", "route-sol", "Seguimiento en vivo");
        } else {
            e.ponerAcciones("primary", "route-sol", "Seguimiento en vivo");
        }
        expedientes.save(e);
        if (e.getPersona().isPrincipal()) {
            notificaciones.notificarEstudiante("Tu trámite " + e.getCodigo() + " (" + e.getTitulo()
                    + ") está listo para recoger en Mesa de Partes.", "solicitudes");
        }
        seguimiento.registrar(e, "Andara UNAJ: tu " + e.getTitulo() + " (" + e.getCodigo() + ") está listo. Recógelo en la ventanilla "
                + SeguimientoService.ventanilla(e.getCodigo()) + " de Mesa de Partes con tu DNI.");
        return e;
    }

    @Transactional
    public Expediente observar(String codigo, String motivo) {
        if (motivo == null || motivo.trim().length() < 15) {
            throw new NegocioException("Escribe al menos 15 caracteres.");
        }
        Expediente e = expediente(codigo);
        if (e.getGrupo() != GrupoExpediente.TRAMITE) {
            throw new NegocioException("Solo se observan trámites en curso.");
        }
        String m = motivo.trim();
        e.setGrupo(GrupoExpediente.OBSERVADO);
        e.setTono("error");
        e.setDestacado(true);
        e.setEstado("Observado por Mesa de Partes");
        e.setObservacion(m);
        e.setRotuloIcono("ALERTA");
        e.setIcono("alert");
        e.ponerAcciones("primary", "fix-sol", "Subsanar Documento", "secondary", "obs-sol", "Ver Observaciones",
                "tertiary", "ask-sol", "Consultar a Secretaría");
        expedientes.save(e);
        if (e.getPersona().isPrincipal()) {
            notificaciones.notificarEstudiante("Tu trámite " + e.getCodigo() + " fue observado: " + m, "solicitudes");
        }
        seguimiento.registrar(e, "Andara UNAJ: tu trámite " + e.getCodigo() + " tiene una observación: " + m);
        return e;
    }

    @Transactional
    public Expediente entregar(String codigo) {
        Expediente e = expediente(codigo);
        e.setEstado("Entregado al solicitante");
        e.setTono("neutral");
        expedientes.save(e);
        if (e.getPersona().isPrincipal()) {
            notificaciones.notificarEstudiante("Registramos la entrega de " + e.getTitulo() + ".", "solicitudes");
        }
        seguimiento.registrar(e, "Andara UNAJ: entregamos tu " + e.getTitulo() + " (" + e.getCodigo() + "). ¡Gracias!");
        return e;
    }

    @Transactional
    public Pago reembolsar(String codigoPago) {
        Pago p = pagos.findByCodigo(codigoPago).orElseThrow(() -> new NegocioException("La orden no existe."));
        if (!p.isPagado()) {
            throw new NegocioException("Solo se reembolsan pagos confirmados.");
        }
        p.setEstado(EstadoPago.REEMBOLSADO);
        pagos.save(p);
        if (p.getPersona().isPrincipal()) {
            notificaciones.notificarEstudiante("Se reembolsó tu pago " + p.getBoleta() + " (" + Montos.soles(p.getTotal()) + ").", "pay");
        }
        return p;
    }

    @Transactional
    public Reserva aprobarReserva(Long id) {
        Reserva r = reservas.findById(id).orElseThrow(() -> new NegocioException("La reserva no existe."));
        r.setEstado(EstadoReserva.APROBADO);
        reservas.save(r);
        if (r.getPersona().isPrincipal()) {
            notificaciones.notificarEstudiante("Tu reserva de " + r.getEspacio().getNombre() + " para el "
                    + Fechas.dmy(r.getFecha()) + " fue aprobada.", "reservas");
        }
        return r;
    }

    @Transactional
    public Reserva rechazarReserva(Long id, String motivo) {
        if (motivo == null || motivo.trim().length() < 10) {
            throw new NegocioException("Escribe al menos 10 caracteres.");
        }
        Reserva r = reservas.findById(id).orElseThrow(() -> new NegocioException("La reserva no existe."));
        r.setEstado(EstadoReserva.RECHAZADO);
        r.setNota(motivo.trim());
        reservas.save(r);
        if (r.getPersona().isPrincipal()) {
            notificaciones.notificarEstudiante("Tu reserva de " + r.getEspacio().getNombre() + " fue rechazada: " + motivo.trim(), "reservas");
        }
        return r;
    }

    @Transactional
    public Prestamo aprobarPrestamo(Long id) {
        Prestamo p = prestamos.findById(id).orElseThrow(() -> new NegocioException("El préstamo no existe."));
        p.setEstado(EstadoPrestamo.APROBADO);
        prestamos.save(p);
        if (p.getPersona().isPrincipal()) {
            notificaciones.notificarEstudiante("Tu préstamo " + p.getCodigo() + " fue aprobado. Recógelo en el punto de entrega.", "equipos");
        }
        return p;
    }

    @Transactional
    public Prestamo rechazarPrestamo(Long id) {
        Prestamo p = prestamos.findById(id).orElseThrow(() -> new NegocioException("El préstamo no existe."));
        p.setEstado(EstadoPrestamo.RECHAZADO);
        prestamos.save(p);
        if (p.getPersona().isPrincipal()) {
            notificaciones.notificarEstudiante("Tu préstamo " + p.getCodigo() + " fue rechazado.", "equipos");
        }
        return p;
    }

    @Transactional
    public Prestamo devolverPrestamo(Long id) {
        Prestamo p = prestamos.findById(id).orElseThrow(() -> new NegocioException("El préstamo no existe."));
        p.setEstado(EstadoPrestamo.DEVUELTO);
        String[] partes = p.getDetalle().split("•");
        p.setDetalle("Devuelto con éxito: " + Fechas.dmy(Fechas.hoy()) + " · " + partes[partes.length - 1].trim());
        return prestamos.save(p);
    }

    @Transactional
    public PrestamoActivo devolverCustodia() {
        PrestamoActivo a = activos.findFirstByOrderByIdDesc().filter(x -> !x.isDevuelto())
                .orElseThrow(() -> new NegocioException("No hay préstamos en custodia."));
        a.setDevuelto(true);
        activos.save(a);
        prestamos.save(Prestamo.builder().codigo(a.getCodigo()).clave("h" + UUID.randomUUID().toString().substring(0, 6))
                .persona(a.getPersona()).nombre(a.getEquipo()).detalle("Devuelto con éxito: " + Fechas.dmy(Fechas.hoy()) + " • Sede Capilla")
                .estado(EstadoPrestamo.DEVUELTO).sede(catalogo.sede("capilla").orElseThrow()).build());
        notificaciones.notificarEstudiante("Registramos la devolución del " + a.getEquipo() + ".", "equipos");
        return a;
    }

    public boolean esLaboratorio(Prestamo p) {
        return catalogo.equipos().stream().anyMatch(e -> "laboratorio".equals(e.getCategoria().getCodigo())
                && p.getNombre().contains(e.getNombre().split(" ")[0]));
    }

    public Persona persona(String codigo) {
        return catalogo.persona(codigo).orElse(null);
    }

    public ConexionService conexion() {
        return conexion;
    }
}
