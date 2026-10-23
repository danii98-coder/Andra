package pe.edu.unaj.andara.service;

import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;
import org.apache.pdfbox.util.Matrix;
import org.springframework.stereotype.Service;
import pe.edu.unaj.andara.model.*;
import pe.edu.unaj.andara.util.Fechas;
import pe.edu.unaj.andara.util.Hash;
import pe.edu.unaj.andara.util.Montos;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DocumentoGeneradoService {

    public record Semestre(String periodo, int creditos, String promedio) {
    }

    public record DocumentoOficial(String plantilla, String titulo, DatosSolicitante solicitante, String escuela,
                                   String rol, String sede, String uso, String fecha, String verificacion,
                                   List<Semestre> semestres, String contenidoQr, byte[] foto) {
        public boolean conCodigo() {
            return solicitante.getCodigo() != null && !solicitante.getCodigo().isBlank()
                    && List.of("Estudiante", "Egresado").contains(solicitante.getTipo());
        }
    }

    private static final Color AZUL = new Color(0x00, 0x71, 0xE3);
    private static final Color TEXTO = new Color(0x1D, 0x1D, 0x1F);
    private static final Color GRIS = new Color(0x6E, 0x6E, 0x73);
    private static final Color BORDE = new Color(0xD2, 0xD2, 0xD7);

    private final CatalogoService catalogo;

    public static String verificacion(String codigoPago) {
        String b = Long.toString(Hash.numero(codigoPago), 36).toUpperCase();
        return "UNAJ-" + b.substring(0, Math.min(6, b.length()));
    }

    public DatosSolicitante solicitante(Pago p) {
        if (p.getSolicitante() != null && p.getSolicitante().getNombre() != null) {
            return p.getSolicitante();
        }
        Persona u = p.getPersona();
        return DatosSolicitante.builder().tipo(u.getTipo()).nombre(u.getNombre()).dni(u.getDni() == null ? "—" : u.getDni())
                .codigo(u.getCodigo()).escuela(u.getEscuela() == null ? null : u.getEscuela().getCodigo())
                .correo(u.getCorreo()).build();
    }

    public DocumentoOficial construir(Pago p) {
        Tramite t = p.getTramite();
        DatosSolicitante s = solicitante(p);
        String escuela = catalogo.nombreEscuela(s.getEscuela());
        String rol = switch (s.getTipo() == null ? "" : s.getTipo()) {
            case "Estudiante" -> "es estudiante de la Escuela Profesional de " + escuela;
            case "Egresado" -> "es egresado(a) de la Escuela Profesional de " + escuela;
            case "Docente" -> "es docente de la Escuela Profesional de " + escuela;
            case "Administrativo" -> "labora como personal administrativo"
                    + (s.getEscuela() != null && !s.getEscuela().isBlank() ? " en " + s.getEscuela() : "");
            default -> "ha solicitado el presente documento como público en general";
        };
        List<Semestre> semestres = new ArrayList<>();
        if (t != null && "record".equals(t.getPlantilla())) {
            String[] periodos = {"2024-2", "2025-1", "2025-2", "2026-1"};
            for (int i = 0; i < periodos.length; i++) {
                double prom = 14.2 + (Hash.numero(s.getDni() + periodos[i]) % 25) / 10.0;
                semestres.add(new Semestre(periodos[i], 20 + i % 2, String.format(java.util.Locale.US, "%.2f", prom)));
            }
        }
        String fecha = Fechas.largoSinDia((p.getPagado() != null ? p.getPagado() : Fechas.ahora()).toLocalDate());
        String uso = (p.getMotivo() == null || p.getMotivo().isBlank() ? "trámite personal" : p.getMotivo()).toLowerCase();
        return new DocumentoOficial(t == null ? "constancia" : t.getPlantilla(), t == null ? p.getConcepto().toUpperCase()
                : t.getNombre().toUpperCase(), s, escuela, rol, p.getSede().getNombre(), uso, fecha,
                verificacion(p.getCodigo()), semestres, "ANDARA-DOC|" + p.getCodigo() + "|DEMO", p.getFoto());
    }

    public byte[] pdfDocumento(Pago p) {
        DocumentoOficial d = construir(p);
        return crear(lienzo -> {
            if ("carne".equals(d.plantilla())) {
                carnePdf(lienzo, d);
                return;
            }
            lienzo.encabezadoUnaj();
            lienzo.titulo(d.titulo());
            StringBuilder parrafo = new StringBuilder("La Dirección de Servicios Académicos hace constar que ")
                    .append(d.solicitante().getNombre()).append(", identificado(a) con DNI N.° ").append(d.solicitante().getDni());
            if (d.conCodigo()) {
                parrafo.append(" y código universitario N.° ").append(d.solicitante().getCodigo());
            }
            parrafo.append(", ").append(d.rol()).append(", sede ").append(d.sede()).append(".");
            lienzo.parrafo(parrafo.toString(), 11.5f);
            if (!d.semestres().isEmpty()) {
                lienzo.tabla(new String[]{"Semestre", "Créditos", "Promedio"}, d.semestres().stream()
                        .map(x -> new String[]{x.periodo(), String.valueOf(x.creditos()), x.promedio()}).toList(), new float[]{0.5f, 0.25f, 0.25f});
            }
            if ("certificado".equals(d.plantilla())) {
                lienzo.parrafo("Se certifica que ha aprobado las asignaturas correspondientes a los semestres 2024-2 al 2026-1, conforme al detalle registrado en el sistema académico.", 11.5f);
            }
            lienzo.parrafo("Se expide el presente documento a solicitud del interesado(a) para fines de " + d.uso() + ".", 11.5f);
            lienzo.derecha("Juliaca, " + d.fecha());
            lienzo.firmaYQr(d.contenidoQr(), "Verificación: " + d.verificacion());
        });
    }

    private void carnePdf(Lienzo l, DocumentoOficial d) throws IOException {
        float x = 120, y = 520, w = 355, h = 224;
        l.rectangulo(x, y, w, h, Color.WHITE, BORDE);
        l.rectangulo(x, y + h - 52, w, 52, AZUL, null);
        l.texto("UNAJ", x + 18, y + h - 24, true, 14, Color.WHITE);
        l.texto("Carné universitario 2026-2", x + 18, y + h - 40, false, 10, Color.WHITE);
        l.rectangulo(x + 18, y + 40, 82, 102, new Color(0xE8, 0xE8, 0xED), null);
        if (d.foto() != null && d.foto().length > 0) {
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(d.foto()));
            if (img != null) {
                l.imagen(img, x + 18, y + 40, 82, 102);
            }
        }
        float ty = y + 128;
        l.texto(d.solicitante().getNombre(), x + 116, ty, true, 12, TEXTO);
        l.texto("DNI " + d.solicitante().getDni(), x + 116, ty - 18, false, 10, TEXTO);
        l.texto("Código " + (d.solicitante().getCodigo() == null ? "—" : d.solicitante().getCodigo()), x + 116, ty - 33, false, 10, TEXTO);
        l.texto("EP " + d.escuela(), x + 116, ty - 48, false, 10, TEXTO);
        l.texto("Sede " + d.sede(), x + 116, ty - 63, false, 10, TEXTO);
        l.texto("Válido hasta 31/03/2027", x + 18, y + 14, false, 9, GRIS);
        l.texto(d.verificacion(), x + w - 90, y + 14, false, 9, GRIS);
    }

    public byte[] pdfComprobante(Pago p) {
        DatosSolicitante s = solicitante(p);
        boolean pagado = p.isPagado() || p.getEstado() == EstadoPago.REEMBOLSADO;
        return crear(l -> {
            l.texto("Andara Pay", 56, l.y, true, 16, TEXTO);
            l.texto("UNAJ · tus trámites más simples", 56, l.y - 15, false, 10, GRIS);
            l.textoDerecha(pagado ? "COMPROBANTE DE PAGO" : "ORDEN DE PAGO", 540, l.y, false, 9, GRIS);
            l.textoDerecha(pagado ? p.getBoleta() : p.getCodigo(), 540, l.y - 16, true, 14, TEXTO);
            if (pagado) {
                l.textoDerecha("Op. " + p.getOperacion(), 540, l.y - 30, false, 9, GRIS);
            }
            l.y -= 44;
            l.linea();
            l.y -= 18;
            l.texto("SOLICITANTE", 56, l.y, true, 8, GRIS);
            l.texto(pagado ? "FECHA DE PAGO" : "EMITIDA", 380, l.y, true, 8, GRIS);
            l.y -= 15;
            l.texto(s.getNombre(), 56, l.y, true, 11, TEXTO);
            l.texto(pagado ? Fechas.dmyHm(p.getPagado()) : Fechas.dmy(p.getCreado().toLocalDate()), 380, l.y, true, 11, TEXTO);
            l.y -= 14;
            l.texto("DNI " + s.getDni() + (s.getCodigo() != null && !s.getCodigo().isBlank() ? " · Código " + s.getCodigo() : ""), 56, l.y, false, 10, GRIS);
            l.texto(pagado ? "MÉTODO" : "VENCE", 380, l.y - 4, true, 8, GRIS);
            l.y -= 14;
            String tipo = s.getTipo() + (catalogo.escuela(s.getEscuela()).isPresent() ? " · EP " + catalogo.nombreEscuela(s.getEscuela()) : "");
            l.texto(tipo, 56, l.y, false, 10, GRIS);
            l.texto(pagado ? p.getMetodo().getEtiqueta() : Fechas.dmy(p.getVence()), 380, l.y - 4, true, 11, TEXTO);
            l.y -= 26;
            List<String[]> filas = p.getItems().stream().map(i -> new String[]{i.getConcepto(), String.valueOf(i.getCantidad()),
                    Montos.soles(i.getPrecioUnitario()), Montos.soles(i.getImporte())}).toList();
            l.tabla(new String[]{"Concepto", "Cant.", "P. unit.", "Importe"}, filas, new float[]{0.52f, 0.12f, 0.18f, 0.18f});
            l.texto("Total a pagar", 56, l.y, true, 12, TEXTO);
            l.textoDerecha(Montos.soles(p.getTotal()), 540, l.y, true, 12, TEXTO);
            l.y -= 24;
            l.parrafo((pagado ? "Pago confirmado. " : "") + "Sede de atención: " + p.getSede().getNombreCompleto()
                    + ". Montos referenciales de demostración; no se realizan cargos reales.", 9.5f);
        });
    }

    public byte[] pdfSimple(String titulo, String meta) {
        return crear(l -> {
            l.texto("UNAJ · Andara", 56, l.y, true, 12, TEXTO);
            l.textoDerecha("PDF", 540, l.y, true, 10, new Color(0xD7, 0x00, 0x15));
            l.y -= 14;
            l.linea();
            l.y -= 24;
            l.parrafoNegrita(titulo, 16f);
            if (meta != null && !meta.isBlank()) {
                l.parrafo(meta, 10.5f);
            }
            l.parrafo("Documento de demostración generado por Andara. En el sistema real aquí estaría el contenido oficial del archivo.", 10.5f);
        });
    }

    private interface Dibujo {
        void dibujar(Lienzo l) throws IOException;
    }

    private byte[] crear(Dibujo dibujo) {
        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDPage pagina = new PDPage(PDRectangle.A4);
            doc.addPage(pagina);
            try (PDPageContentStream cs = new PDPageContentStream(doc, pagina)) {
                Lienzo l = new Lienzo(doc, cs);
                dibujo.dibujar(l);
                l.marcaDeAgua();
            }
            doc.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo generar el PDF", e);
        }
    }

    private static final class Lienzo {
        private final PDDocument doc;
        private final PDPageContentStream cs;
        private final PDFont normal = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        private final PDFont negrita = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
        private float y = 780;

        Lienzo(PDDocument doc, PDPageContentStream cs) {
            this.doc = doc;
            this.cs = cs;
        }

        void texto(String t, float x, float yy, boolean bold, float tam, Color color) throws IOException {
            cs.beginText();
            cs.setFont(bold ? negrita : normal, tam);
            cs.setNonStrokingColor(color);
            cs.newLineAtOffset(x, yy);
            cs.showText(limpiar(t));
            cs.endText();
        }

        void textoDerecha(String t, float derecha, float yy, boolean bold, float tam, Color color) throws IOException {
            float w = (bold ? negrita : normal).getStringWidth(limpiar(t)) / 1000 * tam;
            texto(t, derecha - w, yy, bold, tam, color);
        }

        void linea() throws IOException {
            cs.setStrokingColor(BORDE);
            cs.setLineWidth(1);
            cs.moveTo(56, y);
            cs.lineTo(540, y);
            cs.stroke();
        }

        void encabezadoUnaj() throws IOException {
            texto("Universidad Nacional de Juliaca", 56, y, true, 13, TEXTO);
            texto("Dirección de Servicios Académicos · Andara", 56, y - 15, false, 10, GRIS);
            y -= 26;
            cs.setStrokingColor(AZUL);
            cs.setLineWidth(2);
            cs.moveTo(56, y);
            cs.lineTo(540, y);
            cs.stroke();
            y -= 40;
        }

        void titulo(String t) throws IOException {
            float tam = 15;
            float w = negrita.getStringWidth(limpiar(t)) / 1000 * tam;
            texto(t, (595 - w) / 2, y, true, tam, TEXTO);
            y -= 34;
        }

        void derecha(String t) throws IOException {
            textoDerecha(t, 540, y, false, 11.5f, TEXTO);
            y -= 40;
        }

        void parrafo(String t, float tam) throws IOException {
            escribir(t, tam, normal);
        }

        void parrafoNegrita(String t, float tam) throws IOException {
            escribir(t, tam, negrita);
        }

        private void escribir(String t, float tam, PDFont fuente) throws IOException {
            for (String linea : partir(limpiar(t), fuente, tam, 484)) {
                texto(linea, 56, y, fuente == negrita, tam, TEXTO);
                y -= tam * 1.6f;
            }
            y -= 8;
        }

        private List<String> partir(String t, PDFont fuente, float tam, float ancho) throws IOException {
            List<String> lineas = new ArrayList<>();
            StringBuilder actual = new StringBuilder();
            for (String palabra : t.split(" ")) {
                String prueba = actual.isEmpty() ? palabra : actual + " " + palabra;
                if (fuente.getStringWidth(prueba) / 1000 * tam > ancho && !actual.isEmpty()) {
                    lineas.add(actual.toString());
                    actual = new StringBuilder(palabra);
                } else {
                    actual = new StringBuilder(prueba);
                }
            }
            if (!actual.isEmpty()) {
                lineas.add(actual.toString());
            }
            return lineas;
        }

        void tabla(String[] cabecera, List<String[]> filas, float[] proporciones) throws IOException {
            float ancho = 484;
            float x = 56;
            for (int i = 0; i < cabecera.length; i++) {
                if (i == 0) {
                    texto(cabecera[i].toUpperCase(), x, y, true, 8, GRIS);
                } else {
                    textoDerecha(cabecera[i].toUpperCase(), x + ancho * proporciones[i], y, true, 8, GRIS);
                }
                x += ancho * proporciones[i];
            }
            y -= 8;
            linea();
            y -= 16;
            for (String[] fila : filas) {
                x = 56;
                for (int i = 0; i < fila.length; i++) {
                    if (i == 0) {
                        texto(fila[i], x, y, false, 10, TEXTO);
                    } else {
                        textoDerecha(fila[i], x + ancho * proporciones[i], y, false, 10, TEXTO);
                    }
                    x += ancho * proporciones[i];
                }
                y -= 8;
                linea();
                y -= 16;
            }
            y -= 6;
        }

        void firmaYQr(String contenidoQr, String leyenda) throws IOException {
            float base = Math.min(y, 300);
            cs.setStrokingColor(TEXTO);
            cs.setLineWidth(0.8f);
            cs.moveTo(90, base);
            cs.lineTo(270, base);
            cs.stroke();
            texto("Firma digital", 145, base - 14, false, 10, TEXTO);
            texto("Director(a) de Servicios Académicos", 100, base - 28, false, 8.5f, GRIS);
            imagen(QrService.imagenAwt(contenidoQr, 240), 440, base - 30, 96, 96);
            texto(leyenda, 430, base - 44, false, 8.5f, GRIS);
            y = base - 60;
        }

        void rectangulo(float x, float yy, float w, float h, Color relleno, Color borde) throws IOException {
            cs.addRect(x, yy, w, h);
            if (relleno != null) {
                cs.setNonStrokingColor(relleno);
                if (borde != null) {
                    cs.setStrokingColor(borde);
                    cs.fillAndStroke();
                } else {
                    cs.fill();
                }
            } else if (borde != null) {
                cs.setStrokingColor(borde);
                cs.stroke();
            }
        }

        void imagen(BufferedImage img, float x, float yy, float w, float h) throws IOException {
            PDImageXObject xo = LosslessFactory.createFromImage(doc, img);
            cs.drawImage(xo, x, yy, w, h);
        }

        void marcaDeAgua() throws IOException {
            PDExtendedGraphicsState gs = new PDExtendedGraphicsState();
            gs.setNonStrokingAlphaConstant(0.09f);
            cs.saveGraphicsState();
            cs.setGraphicsStateParameters(gs);
            cs.beginText();
            cs.setFont(negrita, 64);
            cs.setNonStrokingColor(AZUL);
            cs.setTextMatrix(Matrix.getRotateInstance(Math.toRadians(-18), 150, 420));
            cs.showText("DEMOSTRACIÓN");
            cs.endText();
            cs.restoreGraphicsState();
        }

        private static String limpiar(String t) {
            if (t == null) {
                return "";
            }
            StringBuilder sb = new StringBuilder();
            for (char c : t.toCharArray()) {
                sb.append(c == '\n' || c == '\t' ? ' ' : c == '→' ? '>' : c);
            }
            return sb.toString();
        }
    }
}
