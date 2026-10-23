package pe.edu.unaj.andara.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.unaj.andara.exception.NegocioException;
import pe.edu.unaj.andara.model.*;
import pe.edu.unaj.andara.repository.ConsultaAsesorRepository;
import pe.edu.unaj.andara.util.Fechas;

@Service
@RequiredArgsConstructor
public class ConsultaAsesorService {

    public static final int CUPO = 3;

    private final ConsultaAsesorRepository repositorio;
    private final ExpedienteService expedientes;

    public static boolean disponible(Expediente s) {
        return s.getGrupo() == GrupoExpediente.TRAMITE;
    }

    public static String contexto(Expediente s) {
        SeguimientoService.SeguimientoCjsv t = SeguimientoService.calcular(s);
        String etapa = t.actual() < t.etapas().size() ? t.etapas().get(t.actual()).titulo() : "Finalizado";
        StringBuilder sb = new StringBuilder("Consulta sobre el expediente ").append(s.getCodigo()).append(" · ").append(s.getTitulo())
                .append(". Estado: ").append(s.getEstado()).append(". Etapa actual: ").append(etapa).append(". ").append(t.estimado()).append('.');
        if (s.isConPagoPendiente()) {
            sb.append(" Tiene un pago pendiente.");
        }
        return sb.toString();
    }

    @Transactional
    public ConsultaAsesor abrir(String codigo) {
        Expediente s = expedientes.obtener(codigo);
        return repositorio.findByExpediente(s).orElseGet(() -> {
            if (!disponible(s)) {
                throw new NegocioException("La consulta en línea solo está disponible para trámites en curso.");
            }
            ConsultaAsesor c = ConsultaAsesor.builder().expediente(s).usadas(0).cupo(CUPO).build();
            c.getMensajes().add(new MensajeAsesor(false, contexto(s), Fechas.ahora()));
            c.getMensajes().add(new MensajeAsesor(true, "Hola, " + s.getPersona().getPrimerNombre() + ". Ya tengo a la vista tu expediente "
                    + s.getCodigo() + " (" + s.getTitulo() + "). ¿Qué deseas consultar? Dispones de " + CUPO + " consultas.", Fechas.ahora()));
            return repositorio.save(c);
        });
    }

    @Transactional
    public ConsultaAsesor preguntar(String codigo, String texto) {
        ConsultaAsesor c = abrir(codigo);
        String t = texto == null ? "" : texto.trim();
        if (t.isEmpty()) {
            throw new NegocioException("Escribe tu consulta.");
        }
        if (c.restantes() == 0) {
            throw new NegocioException("Agotaste tus consultas. Solicita una ampliación al asesor.");
        }
        c.setUsadas(c.getUsadas() + 1);
        c.getMensajes().add(new MensajeAsesor(false, t, Fechas.ahora()));
        c.getMensajes().add(new MensajeAsesor(true, respuesta(c.getExpediente(), t, c.restantes()), Fechas.ahora()));
        return repositorio.save(c);
    }

    @Transactional
    public ConsultaAsesor solicitarAmpliacion(String codigo) {
        ConsultaAsesor c = abrir(codigo);
        if (c.restantes() > 0) {
            throw new NegocioException("Aún te quedan consultas disponibles.");
        }
        if (c.getAmpliacion() != EstadoAmpliacion.NINGUNA) {
            throw new NegocioException("Ya solicitaste una ampliación para este trámite.");
        }
        c.setAmpliacion(EstadoAmpliacion.PENDIENTE);
        c.getMensajes().add(new MensajeAsesor(false, "Solicito ampliar la consulta (3 oportunidades adicionales).", Fechas.ahora()));
        return repositorio.save(c);
    }

    @Transactional
    public ConsultaAsesor evaluarAmpliacion(String codigo) {
        ConsultaAsesor c = abrir(codigo);
        if (c.getAmpliacion() != EstadoAmpliacion.PENDIENTE) {
            return c;
        }
        if (disponible(c.getExpediente())) {
            c.setAmpliacion(EstadoAmpliacion.APROBADA);
            c.setCupo(c.getCupo() + CUPO);
            c.getMensajes().add(new MensajeAsesor(true, "Revisé tu caso y considero necesario continuar la atención. Te habilité "
                    + CUPO + " consultas adicionales.", Fechas.ahora()));
        } else {
            c.setAmpliacion(EstadoAmpliacion.RECHAZADA);
            c.getMensajes().add(new MensajeAsesor(true, "Tu trámite ya no está en curso, por lo que no es necesario ampliar la consulta.", Fechas.ahora()));
        }
        return repositorio.save(c);
    }

    static String respuesta(Expediente s, String mensaje, int restantes) {
        SeguimientoService.SeguimientoCjsv t = SeguimientoService.calcular(s);
        String m = mensaje.toLowerCase();
        String r;
        if (m.matches("(?s).*(pago|tasa|pagar|monto).*")) {
            r = s.isConPagoPendiente() ? "Tu expediente tiene un pago pendiente; puedes completarlo desde Andara Pay y el trámite continuará automáticamente."
                    : "El pago de tu expediente ya figura registrado, no necesitas abonar nada adicional.";
        } else if (m.matches("(?s).*(cu[aá]ndo|demora|tiempo|plazo|tarda|listo|recoger|entrega).*")) {
            r = t.estimado() + ". Te avisaremos por tus canales de notificación cuando cambie de etapa.";
        } else if (m.matches("(?s).*(estado|etapa|avance|va mi|c[oó]mo va).*")) {
            String etapa = t.actual() < t.etapas().size() ? t.etapas().get(t.actual()).titulo() : "Finalizado";
            r = "Tu expediente está en la etapa \"" + etapa + "\" (" + s.getEstado() + ").";
        } else if (m.matches("(?s).*(document|requisit|adjunt|pdf).*")) {
            r = "Por ahora tu expediente no registra documentos faltantes. Si Jefatura lo observa, verás el motivo en Mis Solicitudes.";
        } else if (m.matches("(?s).*(desist|anular|cancel|reemb).*")) {
            r = "Puedes desistir mientras siga en trámite; la tasa se reintegra en 5 días hábiles.";
        } else if (m.matches("(?s).*(ventanilla|d[oó]nde|sede|oficina).*")) {
            r = "Cuando esté listo lo recogerás en la ventanilla " + t.ventanilla() + " de Mesa de Partes, sede " + s.getSede().getNombre() + ".";
        } else {
            r = "Gracias por tu consulta sobre " + s.getCodigo() + ". La derivé al área encargada y te responderemos por este medio.";
        }
        return restantes == 0 ? r + " Has usado todas tus consultas; si lo necesitas, puedes solicitar una ampliación." : r;
    }
}
