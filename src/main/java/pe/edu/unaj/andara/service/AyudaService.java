package pe.edu.unaj.andara.service;

import org.springframework.stereotype.Service;
import pe.edu.unaj.andara.util.TextoPlano;

import java.util.List;

@Service
public class AyudaService {

    public record Area(String icono, String etiqueta, String titulo, String descripcion, List<String> puntos, String enlace) {
        String texto() {
            return etiqueta + " " + titulo + " " + descripcion + " " + String.join(" ", puntos) + " " + enlace;
        }
    }

    public record Guia(String paso, String titulo, List<String> pasos, String enlace, String destino) {
        String texto() {
            return paso + " " + titulo + " " + String.join(" ", pasos) + " " + enlace + " " + destino;
        }
    }

    public record Pregunta(String categoria, String pregunta, String respuesta) {
        String texto() {
            return categoria + " " + pregunta + " " + respuesta;
        }
    }

    public static final List<Area> AREAS = List.of(
            new Area("fth-file-text", "Trámites académicos", "Gestión de Expedientes y Mesa de Partes",
                    "Seguimiento de estados, plazos de atención y subsanación de carpetas observadas.",
                    List.of("Subsanación de observaciones dentro del plazo de 36 horas útiles.",
                            "Requisitos para constancias de egresado y certificaciones oficiales."),
                    "Ver procedimientos de expedientes"),
            new Area("fth-shield", "Validez y legalidad", "Firma Digital y Certificados SUNEDU",
                    "Validación de documentos electrónicos con código QR y token acreditado de secretaría.",
                    List.of("Verificación de código QR y firma digital del Secretario General.",
                            "Trámite para apostillado ministerial y legalizaciones internacionales."),
                    "Consultar validez y firma electrónica"),
            new Area("fth-calendar", "Infraestructura", "Reservas de Aulas, Talleres y Laboratorios",
                    "Laboratorios y talleres de cada escuela en las tres sedes; auditorios en la sede Capilla.",
                    List.of("Disponibilidad por sede, pabellón y escuela profesional.",
                            "Tolerancia horaria de 15 min y normativa de uso compartido."),
                    "Revisar normativa de espacios"),
            new Area("fth-box", "Equipamiento", "Préstamo de Equipos y Custodia Universitaria",
                    "Reglamento para equipos de sonido, instrumentos musicales e implementos deportivos.",
                    List.of("Verificación de entrega física y registro de código de custodia.",
                            "Penalizaciones automáticas por devolución tardía o deterioro."),
                    "Ver reglamento de préstamos"));

    public static final List<Guia> GUIAS = List.of(
            new Guia("Paso A", "Subsanar expediente observado", List.of("Ubica la alerta en la pantalla 'Mis Solicitudes'.",
                    "Pulsa 'Subsanar' y carga el documento en PDF solicitado.", "El expediente se reactivará y el plazo continuará."),
                    "Ver manual de subsanación", "solicitudes"),
            new Guia("Paso B", "Verificar validez ante SUNEDU", List.of("Descarga el certificado emitido en formato digital.",
                    "Escanea el código QR institucional con tu celular.", "Comprueba el certificado digital y firma en el portal oficial."),
                    "Ver instrucciones de verificación", "documentos"),
            new Guia("Paso C", "Desistir trámite y reembolso", List.of("Verifica que el estado aún se encuentre 'En trámite'.",
                    "Selecciona 'Desistir' antes de la aprobación de secretaría.", "Tesorería reintegrará la tasa en 5 días hábiles a tu cuenta."),
                    "Ver políticas de reembolso", "solicitudes"));

    public static final List<Pregunta> PREGUNTAS = List.of(
            new Pregunta("Expedientes y plazos", "¿Por qué mi solicitud figura como 'Observada' y cuánto tiempo tengo para subsanarla?",
                    "Un trámite es observado cuando la secretaría académica detecta que falta un requisito reglamentario (por ejemplo, visado médico institucional o tasa administrativa complementaria). Cuentas con un plazo máximo improrrogable de 36 horas útiles para adjuntar el sustento en 'Mis Solicitudes' antes de que el trámite caduque automáticamente."),
            new Pregunta("Validez legal y SUNEDU", "¿Los documentos emitidos en esta plataforma tienen validez ante SUNEDU y el extranjero?",
                    "Sí. Toda constancia y certificado digital emitido incorpora firma digital con certificado digital acreditado por Indecopi y código de verificación QR amparado por la Ley de Firmas Digitales del Perú. Para trámites en el extranjero (Apostilla de La Haya), puedes solicitar el formato con firma manuscrita legalizada ante Secretaría General."),
            new Pregunta("Reservas y custodia", "¿Puedo anular una reserva de aula o préstamo de equipo sin incurrir en penalizaciones?",
                    "Puedes cancelar cualquier reserva hasta con 30 minutos de anticipación desde el panel de 'Mis Solicitudes' o 'Reservas'. Si no te presentas sin avisar tras 15 minutos de iniciada la hora reservada, el sistema registrará una falta administrativa y bloqueará la separación de nuevos recursos por 14 días calendario."),
            new Pregunta("Pagos y tasas bancarias", "¿Cuáles son los métodos de pago aceptados para las tasas de trámites académicos?",
                    "Puedes abonar las tasas mediante tarjeta de débito o crédito (Visa, Mastercard, Diners), banca por internet de los principales bancos nacionales, o directamente con cargo a tu boleta de pensión mensual universitaria si te encuentras matriculado en el ciclo regular vigente."),
            new Pregunta("Equipos universitarios", "¿Qué debo hacer si extravié un equipo prestado o sufre algún desperfecto durante el uso?",
                    "Debes acercarte de inmediato a la jefatura de portería o pulsar 'Reportar daño / falla' en la sección Equipos. Se levantará un acta de conformidad técnica con el especialista y se evaluará si la contingencia queda cubierta por la póliza universitaria o si corresponde la reposición del implemento."));

    public static final List<String> POPULARES = List.of("Subsanación", "Constancia con QR", "Préstamo", "Devolución de tasa", "SUNEDU");

    public List<Area> areas(String q) {
        return AREAS.stream().filter(a -> TextoPlano.contiene(a.texto(), q)).toList();
    }

    public List<Guia> guias(String q) {
        return GUIAS.stream().filter(g -> TextoPlano.contiene(g.texto(), q)).toList();
    }

    public List<Pregunta> preguntas(String q) {
        return PREGUNTAS.stream().filter(p -> TextoPlano.contiene(p.texto(), q)).toList();
    }

    public static String respuestaChat(String mensaje) {
        String m = mensaje.toLowerCase();
        if (m.matches("(?s).*(subsan|observ).*")) {
            return "Para subsanar, entra a 'Mis Solicitudes' y pulsa 'Subsanar ahora'. Tienes 36 horas útiles.";
        }
        if (m.matches("(?s).*(tasa|pago|reemb).*")) {
            return "Las tasas se reintegran en 5 días hábiles tras desistir de un trámite en curso.";
        }
        return "Gracias. Un operador revisará tu consulta y te responderá por este medio.";
    }
}
