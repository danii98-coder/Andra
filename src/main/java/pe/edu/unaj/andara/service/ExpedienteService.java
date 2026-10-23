package pe.edu.unaj.andara.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.unaj.andara.exception.NegocioException;
import pe.edu.unaj.andara.model.*;
import pe.edu.unaj.andara.repository.ExpedienteRepository;
import pe.edu.unaj.andara.util.Fechas;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

@Service
@RequiredArgsConstructor
public class ExpedienteService {

    public static final List<String[]> PRESETS = List.of(new String[]{"pay", "Con pago pendiente"},
            new String[]{"act", "Requieren acción"}, new String[]{"rl", "Reservas y préstamos"},
            new String[]{"doc", "Documentos"});
    public static final List<String[]> PASOS = List.of(new String[]{"1", "Mesa de Partes"},
            new String[]{"2", "Validación de Escuela"}, new String[]{"3", "Firma Digital"}, new String[]{"4", "Listo"});

    public record GrupoOpciones(String grupo, List<String> opciones) {
    }

    public static final List<GrupoOpciones> TIPOS_SOLICITUD = List.of(
            new GrupoOpciones("Certificados y constancias", List.of("Certificado de estudios", "Certificado de notas",
                    "Constancia de estudios", "Constancia de matrícula", "Constancia de egresado",
                    "Constancia de orden de mérito / tercio superior", "Constancia de no adeudo")),
            new GrupoOpciones("Académicos", List.of("Récord académico", "Sílabos certificados", "Malla curricular certificada",
                    "Convalidación de cursos", "Traslado interno", "Reserva de matrícula", "Retiro de curso",
                    "Justificación de inasistencia")),
            new GrupoOpciones("Grados y títulos", List.of("Grado de bachiller", "Título profesional", "Duplicado de diploma")),
            new GrupoOpciones("Otros", List.of("Devolución de tasa", "Carné universitario", "Otro trámite")));

    private final ExpedienteRepository repositorio;
    private final SesionService sesion;
    private final CatalogoService catalogo;
    private final PagoService pagos;
    private final NotificacionService notificaciones;
    private final ConexionService conexion;

    @Transactional(readOnly = true)
    public List<Expediente> mios() {
        return repositorio.findByPersonaOrderByIdDesc(sesion.estudiante());
    }

    @Transactional(readOnly = true)
    public List<Expediente> todos() {
        return repositorio.findAllByOrderByIdDesc();
    }

    @Transactional(readOnly = true)
    public Expediente obtener(String codigo) {
        return repositorio.findByCodigo(codigo).orElseThrow(() -> new NegocioException("El expediente no existe."));
    }

    @Transactional(readOnly = true)
    public Optional<Expediente> dePago(Pago p) {
        return repositorio.findFirstByPago(p);
    }

    public static long contar(List<Expediente> lista, GrupoExpediente grupo) {
        return lista.stream().filter(e -> grupo == null || e.getGrupo() == grupo).count();
    }

    public static Map<String, Predicate<Expediente>> predicados() {
        return Map.of("pay", Expediente::isConPagoPendiente,
                "act", e -> e.getGrupo() == GrupoExpediente.OBSERVADO || e.isConPagoPendiente(),
                "rl", e -> e.getClase().esReservaOPrestamo(),
                "doc", e -> e.getClase() == ClaseExpediente.DOC);
    }

    public static String textoBusqueda(Expediente e) {
        StringBuilder sb = new StringBuilder(e.getCodigo() + " " + e.getTitulo() + " " + e.getEstado());
        e.getDatos().forEach(d -> sb.append(' ').append(d.getValor()));
        return sb.toString();
    }

    public Optional<Tramite> tramitePara(String tipo) {
        return catalogo.tramites().stream().filter(t -> t.getNombre().equals(tipo)
                || ("Certificado de notas".equals(tipo) && "t-cert".equals(t.getCodigo()))
                || ("Grado de bachiller".equals(tipo) && "t-bach".equals(t.getCodigo()))).findFirst();
    }

    private static String hoyTexto() {
        LocalDate d = Fechas.hoy();
        return "Hoy, " + d.getDayOfMonth() + " de " + Fechas.MESES[d.getMonthValue() - 1] + ", " + d.getYear();
    }

    @Transactional
    public Expediente registrar(String tipo, String unidad, String sedeCodigo, String detalle, String adjunto) {
        if (tipo == null || tipo.isBlank()) {
            throw new NegocioException("Elige el tipo de trámite.");
        }
        if (unidad == null || unidad.isBlank()) {
            throw new NegocioException("Elige la unidad académica.");
        }
        if (detalle == null || detalle.trim().length() < 15) {
            throw new NegocioException("Escribe al menos 15 caracteres.");
        }
        Sede sede = catalogo.sede(sedeCodigo).orElseGet(() -> sesion.estudiante().getSede());
        String codigo = pagos.siguienteCodigoExpediente("SOL-2026-0", 85200);
        Expediente e = Expediente.builder().codigo(codigo).persona(sesion.estudiante()).sede(sede).clase(ClaseExpediente.DOC)
                .grupo(GrupoExpediente.TRAMITE).icono("doc").rotuloIcono("EN CURSO").tono("warning")
                .estado("Registrado en Mesa de Partes").titulo(tipo).descripcion(detalle.trim()).paso(1)
                .nota(adjunto != null && !adjunto.isBlank() ? "Adjunto: " + adjunto : null).fecha(Fechas.ahora()).build();
        e.getDatos().add(new DatoExpediente("Fecha de registro", hoyTexto()));
        e.getDatos().add(new DatoExpediente("Sede de atención", sede.getNombreCompleto()));
        e.getDatos().add(new DatoExpediente("Unidad académica", unidad));
        e.ponerAcciones("primary", "route-sol", "Seguimiento en vivo", "secondary", "state-sol", "Consultar Estado",
                "danger", "desist-sol", "Desistir / Anular Trámite");
        repositorio.save(e);
        notificaciones.notificarEstudiante("Expediente " + codigo + " registrado en Mesa de Partes.", "solicitudes");
        conexion.encolar("Nueva solicitud");
        return e;
    }

    @Transactional
    public Expediente extraordinaria(String tipo, String descripcion, String adjunto) {
        if (tipo == null || tipo.isBlank()) {
            throw new NegocioException("Elige el tipo de caso.");
        }
        if (descripcion == null || descripcion.trim().length() < 20) {
            throw new NegocioException("Escribe al menos 20 caracteres.");
        }
        String codigo = pagos.siguienteCodigoExpediente("SOL-2026-EX-0", 10);
        Expediente e = Expediente.builder().codigo(codigo).persona(sesion.estudiante()).sede(sesion.estudiante().getSede())
                .clase(ClaseExpediente.DOC).grupo(GrupoExpediente.TRAMITE).icono("doc").rotuloIcono("DECANATO")
                .tono("warning").estado("En evaluación por Decanato").titulo(tipo).descripcion(descripcion.trim())
                .paso(1).nota(adjunto != null && !adjunto.isBlank() ? "Adjunto: " + adjunto : null).fecha(Fechas.ahora()).build();
        e.getDatos().add(new DatoExpediente("Fecha de registro", hoyTexto()));
        e.getDatos().add(new DatoExpediente("Tipo", "Expediente extraordinario"));
        e.getDatos().add(new DatoExpediente("Unidad", "Decanato"));
        e.ponerAcciones("primary", "route-sol", "Seguimiento en vivo", "secondary", "state-sol", "Consultar Estado",
                "danger", "desist-sol", "Desistir / Anular Trámite");
        conexion.encolar("Solicitud extraordinaria");
        return repositorio.save(e);
    }

    @Transactional
    public Expediente subsanar(String codigo, String archivo) {
        if (archivo == null || archivo.isBlank()) {
            throw new NegocioException("Adjunta el documento en PDF.");
        }
        Expediente e = obtener(codigo);
        e.setGrupo(GrupoExpediente.TRAMITE);
        e.setTono("warning");
        e.setDestacado(false);
        e.setEstado("Subsanación enviada · En revisión");
        e.setRotuloIcono("EN CURSO");
        e.setIcono("doc");
        e.setNota("Archivo enviado: " + archivo);
        e.ponerAcciones("primary", "route-sol", "Seguimiento en vivo", "secondary", "state-sol", "Consultar Estado",
                "tertiary", "ask-sol", "Consultar a Secretaría");
        if (e.getDatos().size() > 2) {
            e.ponerDato(2, "Plazo subsanación", "Subsanado dentro del plazo");
        }
        repositorio.save(e);
        notificaciones.quitarPorTexto(codigo + " fue observado");
        conexion.encolar("Subsanar documento");
        return e;
    }

    @Transactional
    public Expediente desistir(String codigo) {
        Expediente e = obtener(codigo);
        e.setGrupo(GrupoExpediente.ANULADO);
        e.setTono("neutral");
        e.setEstado("Trámite anulado por el solicitante");
        e.setRotuloIcono("ANULADO");
        e.setDestacado(false);
        e.ponerAcciones("secondary", "route-sol", "Seguimiento en vivo");
        if (e.getPago() != null) {
            pagos.anular(e.getPago());
        }
        conexion.encolar("¿Desistir de este trámite?");
        return repositorio.save(e);
    }

    @Transactional
    public Expediente cancelar(String codigo) {
        Expediente e = obtener(codigo);
        e.setGrupo(GrupoExpediente.ANULADO);
        e.setTono("neutral");
        e.setEstado(e.getClase() == ClaseExpediente.LAB ? "Reserva cancelada" : "Préstamo cancelado");
        e.ponerAcciones("secondary", "cargo-sol", "Ver Cargo");
        conexion.encolar("Cancelar");
        return repositorio.save(e);
    }

    @Transactional
    public Expediente reprogramar(String codigo, LocalDate fecha, String horario) {
        if (fecha == null) {
            throw new NegocioException("Elige una fecha.");
        }
        if (fecha.isBefore(Fechas.hoy())) {
            throw new NegocioException("Elige hoy o una fecha posterior.");
        }
        if (horario == null || horario.isBlank()) {
            throw new NegocioException("Elige un horario.");
        }
        Expediente e = obtener(codigo);
        String etiqueta = e.getDatos().isEmpty() ? "Fecha" : e.getDatos().get(0).getEtiqueta();
        e.ponerDato(0, etiqueta, fecha.getDayOfMonth() + " de " + Fechas.MESES[fecha.getMonthValue() - 1] + ", "
                + fecha.getYear() + " (" + horario + ")");
        conexion.encolar("Modificar horario");
        return repositorio.save(e);
    }

    public static List<String> horariosReprogramacion(Expediente e) {
        return e.getClase() == ClaseExpediente.LAB ? List.of("08:00 - 12:00", "12:00 - 16:00", "16:00 - 20:00")
                : List.of("10:00 - 14:00", "14:00 - 18:00");
    }
}
