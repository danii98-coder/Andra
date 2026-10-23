package pe.edu.unaj.andara.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.unaj.andara.exception.NegocioException;
import pe.edu.unaj.andara.model.EstadoReserva;
import pe.edu.unaj.andara.model.Espacio;
import pe.edu.unaj.andara.model.Persona;
import pe.edu.unaj.andara.model.Reserva;
import pe.edu.unaj.andara.repository.ReservaRepository;
import pe.edu.unaj.andara.util.Fechas;
import pe.edu.unaj.andara.util.Hash;
import pe.edu.unaj.andara.util.Montos;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReservaService {

    private final ReservaRepository repositorio;
    private final SesionService sesion;
    private final NotificacionService notificaciones;
    private final ConexionService conexion;

    public static boolean disponibleSimulado(String nombreEspacio, long desplazamientoDias, String franja) {
        return Hash.numero(nombreEspacio + desplazamientoDias + franja) % 3 != 0;
    }

    @Transactional(readOnly = true)
    public boolean ocupadoPorReserva(Espacio espacio, LocalDate fecha, String franja) {
        return repositorio.findByEspacioAndFecha(espacio, fecha).stream()
                .anyMatch(r -> r.getHorario().startsWith(franja) && r.getEstado().isActiva());
    }

    @Transactional(readOnly = true)
    public boolean libre(Espacio espacio, long desplazamientoDias, String franja) {
        return !espacio.isFueraDeServicio() && disponibleSimulado(espacio.getNombre(), desplazamientoDias, franja)
                && !ocupadoPorReserva(espacio, Fechas.hoy().plusDays(desplazamientoDias), franja);
    }

    @Transactional(readOnly = true)
    public boolean libreEnInicio(Espacio espacio, long desplazamientoDias, String franja) {
        return disponibleSimulado(espacio.getNombre(), desplazamientoDias, franja)
                && !ocupadoPorReserva(espacio, Fechas.hoy().plusDays(desplazamientoDias), franja);
    }

    @Transactional(readOnly = true)
    public String estadoGaleria(Espacio espacio, LocalDate fecha, String horario) {
        if (espacio.isFueraDeServicio()) {
            return "Ocupado";
        }
        if (fecha != null && horario != null && !horario.isBlank() && ocupadoPorReserva(espacio, fecha, horario.substring(0, 5))) {
            return "Ocupado";
        }
        return "Disponible";
    }

    @Transactional
    public Reserva reservar(Espacio espacio, LocalDate fecha, String horario, Integer asistentes, String motivo) {
        if (fecha == null) {
            throw new NegocioException("Elige una fecha.");
        }
        if (fecha.isBefore(Fechas.hoy())) {
            throw new NegocioException("Elige hoy o una fecha posterior.");
        }
        if (horario == null || !CatalogoService.HORARIOS.contains(horario)) {
            throw new NegocioException("Elige un horario.");
        }
        if (asistentes == null || asistentes < 1) {
            throw new NegocioException("Indica cuántas personas asistirán.");
        }
        if (asistentes > espacio.getCapacidad()) {
            throw new NegocioException("El máximo permitido es " + Montos.miles(espacio.getCapacidad()) + ".");
        }
        if (motivo == null || motivo.trim().length() < 10) {
            throw new NegocioException("Escribe al menos 10 caracteres.");
        }
        if (ocupadoPorReserva(espacio, fecha, horario.substring(0, 5))) {
            throw new NegocioException("Ese horario ya está reservado. Elige otro.");
        }
        Reserva r = repositorio.save(Reserva.builder().codigo(nuevoCodigo()).persona(sesion.estudiante()).espacio(espacio)
                .fecha(fecha).horario(horario).estado(EstadoReserva.PENDIENTE).motivo(motivo.trim())
                .asistentes(asistentes).build());
        notificaciones.notificarEstudiante("Solicitud de reserva enviada: " + espacio.getNombre() + ", "
                + Fechas.dmy(fecha) + ".", "reservas");
        conexion.encolar("Reserva de " + espacio.getNombre());
        return r;
    }

    @Transactional
    public Reserva reservaRapida(Espacio espacio, LocalDate fecha, String franja) {
        String horario = CatalogoService.HORARIOS.get(CatalogoService.FRANJAS.indexOf(franja));
        if (ocupadoPorReserva(espacio, fecha, franja)) {
            throw new NegocioException("Ese horario ya está reservado. Elige otro.");
        }
        conexion.encolar("Confirmar reserva");
        return repositorio.save(Reserva.builder().codigo(nuevoCodigo()).persona(sesion.estudiante()).espacio(espacio)
                .fecha(fecha).horario(horario).estado(EstadoReserva.PENDIENTE).motivo("Reserva rápida desde Inicio")
                .asistentes(1).build());
    }

    @Transactional(readOnly = true)
    public List<Reserva> mias() {
        Persona yo = sesion.estudiante();
        return repositorio.findByPersonaOrderByIdDesc(yo);
    }

    @Transactional(readOnly = true)
    public List<Reserva> todas() {
        return repositorio.findAllByOrderByIdDesc();
    }

    @Transactional(readOnly = true)
    public Reserva obtener(Long id) {
        return repositorio.findById(id).orElseThrow(() -> new NegocioException("La reserva no existe."));
    }

    @Transactional
    public boolean eliminar(Long id) {
        Reserva r = obtener(id);
        boolean activa = r.getEstado().isActiva();
        repositorio.delete(r);
        conexion.encolar(activa ? "¿Cancelar esta reserva?" : "¿Eliminar del historial?");
        return activa;
    }

    private String nuevoCodigo() {
        return "R-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
