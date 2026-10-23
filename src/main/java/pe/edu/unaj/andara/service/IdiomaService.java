package pe.edu.unaj.andara.service;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.annotation.PostConstruct;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pe.edu.unaj.andara.config.JsonRecursos;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class IdiomaService {

    public static final List<String[]> IDIOMAS = List.of(
            new String[]{"es", "Español", "Castellano"},
            new String[]{"en", "English", "Inglés"},
            new String[]{"qu", "Runasimi", "Quechua"},
            new String[]{"ay", "Aymar aru", "Aimara"});

    private static final Pattern TOK = Pattern.compile(
            "S/\\s?\\d[\\d.,]*|\\b[A-Z][A-Z0-9]*(?:-[A-Z0-9]+)+\\b|\\d[\\d.,:/]*(?:\\s?%)?|“[^”]*”");
    private static final Pattern DOS_LETRAS = Pattern.compile("[A-Za-zÁÉÍÓÚáéíóúñÑ]{2}");
    private static final Pattern UNA_LETRA = Pattern.compile("[A-Za-zÁÉÍÓÚáéíóúñÑ]");
    private static final Pattern MARCAS = Pattern.compile("\\{(\\d+)}");
    private static final String MESES_RX = "(enero|febrero|marzo|abril|mayo|junio|julio|agosto|septiembre|octubre|noviembre|diciembre)";
    private static final Pattern FECHA_CON_DIA = Pattern.compile(
            "\\b(lunes|martes|miércoles|jueves|viernes|sábado|domingo),?\\s+(\\d{1,2}) de " + MESES_RX
                    + "(?:,? (?:de )?(\\d{4}))?", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern FECHA_SIN_DIA = Pattern.compile(
            "\\b(\\d{1,2}) de " + MESES_RX + "(?:,? (?:de )?(\\d{4}))?", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern DIA_CORTO = Pattern.compile("\\b(Lun|Mar|Mié|Jue|Vie|Sáb|Dom)\\b(\\.?)");
    private static final Pattern MES_CORTO = Pattern.compile("\\b(\\d{1,2}) (ene|feb|mar|abr|may|jun|jul|ago|sep|oct|nov|dic)\\b\\.?");
    private static final Pattern DIA_CAPITAL = Pattern.compile(
            "(^|[^A-Za-zÁ-ú])(Lunes|Martes|Miércoles|Jueves|Viernes|Sábado|Domingo)(?![A-Za-zá-ú])");
    private static final String[] SEPARADORES = {" · ", " • ", " — ", " – ", ". ", ": ", " | ", ", ", " ("};
    private static final List<String> DIAS_ES = List.of("lunes", "martes", "miércoles", "jueves", "viernes", "sábado", "domingo");
    private static final List<String> MESES_ES = List.of("enero", "febrero", "marzo", "abril", "mayo", "junio", "julio",
            "agosto", "septiembre", "octubre", "noviembre", "diciembre");
    private static final List<String> DIAS_ES_CORTOS = List.of("lun", "mar", "mié", "jue", "vie", "sáb", "dom");
    private static final List<String> MESES_ES_CORTOS = List.of("ene", "feb", "mar", "abr", "may", "jun", "jul", "ago",
            "sep", "oct", "nov", "dic");
    private static final String[] MESES_EN = {"January", "February", "March", "April", "May", "June", "July", "August",
            "September", "October", "November", "December"};

    private record Prefijo(Pattern patron, Map<String, String> plantillas) {
    }

    private static final List<Prefijo> PREFIJOS = List.of(
            new Prefijo(Pattern.compile("^Ver detalles de (.+)$"), Map.of("en", "View details of $", "qu", "$ sut'ichaykunata qhaway", "ay", "$ qhanancht'awinak uñjaña")),
            new Prefijo(Pattern.compile("^Reservar (.+)$"), Map.of("en", "Book $", "qu", "$ waqaychay", "ay", "$ katuyasiña")),
            new Prefijo(Pattern.compile("^Cancelar reserva (.+)$"), Map.of("en", "Cancel booking $", "qu", "$ waqaychayta saqiy", "ay", "$ katuyasiñ apanukuña")),
            new Prefijo(Pattern.compile("^Ir a diapositiva (.+)$"), Map.of("en", "Go to slide $", "qu", "$ diapositivaman riy", "ay", "$ diapositivar saraña")),
            new Prefijo(Pattern.compile("^Hola, ([^\\s.,]+)([.,!]?)$"), Map.of("en", "Hi, $", "qu", "Napaykullayki, $", "ay", "Kamisaki, $")),
            new Prefijo(Pattern.compile("^Seguimiento de (.+)$"), Map.of("en", "Tracking for $", "qu", "$ qatipay", "ay", "$ arktawi")),
            new Prefijo(Pattern.compile("^Solicitar (.+)$"), Map.of("en", "Request $", "qu", "$ mañakuy", "ay", "$ mayiña")));

    private static IdiomaService instancia;

    private final JsonRecursos recursos;
    private final ObjectProperty<String> idioma = new SimpleObjectProperty<>("es");
    private final Map<String, Map<String, String>> diccionario = new HashMap<>();
    private final Map<String, List<String>> dias = new HashMap<>();
    private final Map<String, Map<String, Optional<String>>> cache = new ConcurrentHashMap<>();
    private final Set<String> faltantes = ConcurrentHashMap.newKeySet();

    public IdiomaService(JsonRecursos recursos) {
        this.recursos = recursos;
    }

    @PostConstruct
    public void cargar() {
        JsonNode raiz = recursos.traducciones();
        for (String l : List.of("en", "qu", "ay")) {
            diccionario.put(l, new HashMap<>());
            cache.put(l, new ConcurrentHashMap<>());
            List<String> d = new ArrayList<>();
            raiz.path("dias").path(l).forEach(x -> d.add(x.asText()));
            dias.put(l, d);
        }
        Iterator<Map.Entry<String, JsonNode>> it = raiz.path("textos").fields();
        while (it.hasNext()) {
            Map.Entry<String, JsonNode> e = it.next();
            for (String l : List.of("en", "qu", "ay")) {
                JsonNode t = e.getValue().get(l);
                if (t != null && !t.asText().isEmpty()) {
                    diccionario.get(l).put(e.getKey(), t.asText());
                }
            }
        }
        agregar("Sede Capilla", "Capilla Campus", "Capilla sede", "Capilla sede");
        agregar("Sede Ayabacas", "Ayabacas Campus", "Ayabacas sede", "Ayabacas sede");
        agregar("Sede Salida Cuzco", "Salida Cuzco Campus", "Salida Cuzco sede", "Salida Cuzco sede");
        agregar("Pago", "Payment", "Pagay", "Payllawi");
        agregar("Espacio", "Space", "Kiti", "Chiqa");
        agregar("Dirección de Servicios Académicos", "Academic Services Office", "Yachay Yanapakuy Kamachiy", "Yatiqañ Yanapt'añ Irpawi");
        agregar("Dependencia / Escuela profesional", "Office / School", "Kamachiy / Yachay programa", "Irpawi / Yatiqañ programa");
        instancia = this;
    }

    private void agregar(String es, String en, String qu, String ay) {
        diccionario.get("en").put(es, en);
        diccionario.get("qu").put(es, qu);
        diccionario.get("ay").put(es, ay);
    }

    public static IdiomaService actual() {
        return instancia;
    }

    public ObjectProperty<String> idiomaProperty() {
        return idioma;
    }

    public String getIdioma() {
        return idioma.get();
    }

    public void setIdioma(String codigo) {
        idioma.set(codigo);
    }

    public int totalTextos() {
        return diccionario.get("en").size();
    }

    public Set<String> claves() {
        return Collections.unmodifiableSet(diccionario.get("en").keySet());
    }

    public String directa(String clave, String lengua) {
        Map<String, String> d = diccionario.get(lengua);
        return d == null ? null : d.get(clave);
    }

    public Set<String> faltantes() {
        return Collections.unmodifiableSet(faltantes);
    }

    public static String t(String texto) {
        return instancia == null ? texto : instancia.traducir(texto);
    }

    public static String t(String plantilla, Object... valores) {
        return t(formatear(plantilla, valores));
    }

    public static String formatear(String plantilla, Object... valores) {
        Matcher m = MARCAS.matcher(plantilla);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            int i = Integer.parseInt(m.group(1));
            m.appendReplacement(sb, Matcher.quoteReplacement(i < valores.length ? String.valueOf(valores[i]) : ""));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    public static StringBinding enlace(String texto) {
        if (instancia == null) {
            return Bindings.createStringBinding(() -> texto);
        }
        return Bindings.createStringBinding(() -> instancia.traducir(texto), instancia.idioma);
    }

    public static StringBinding enlaceMayus(String texto) {
        if (instancia == null) {
            return Bindings.createStringBinding(() -> texto.toUpperCase(Locale.ROOT));
        }
        return Bindings.createStringBinding(() -> instancia.traducir(texto).toUpperCase(Locale.ROOT), instancia.idioma);
    }

    public static StringBinding enlace(java.util.function.Supplier<String> texto) {
        if (instancia == null) {
            return Bindings.createStringBinding(texto::get);
        }
        return Bindings.createStringBinding(() -> instancia.traducir(texto.get()), instancia.idioma);
    }

    public String traducir(String s) {
        return traducir(s, idioma.get());
    }

    public String traducir(String s, String lengua) {
        if (s == null || "es".equals(lengua) || s.isEmpty()) {
            return s;
        }
        String k = s.trim();
        if (k.isEmpty() || !UNA_LETRA.matcher(k).find()) {
            return s;
        }
        Map<String, Optional<String>> c = cache.get(lengua);
        if (c == null) {
            return s;
        }
        Optional<String> guardado = c.get(k);
        String t;
        if (guardado != null) {
            t = guardado.orElse(null);
        } else {
            t = nucleo(k, lengua, 0);
            if (c.size() < 20000) {
                c.put(k, Optional.ofNullable(t));
            }
            if (t == null && faltantes.add(k)) {
                log.info("Sin traducción ({}): {}", lengua, k);
            }
        }
        if (t != null && !"en".equals(lengua)) {
            List<String> d = dias.get(lengua);
            Matcher m = DIA_CAPITAL.matcher(t);
            StringBuilder sb = new StringBuilder();
            while (m.find()) {
                String w = m.group(2).toLowerCase();
                m.appendReplacement(sb, Matcher.quoteReplacement(m.group(1) + d.get(DIAS_ES.indexOf(w))));
            }
            m.appendTail(sb);
            t = sb.toString();
        }
        return t == null ? s : s.replace(k, t);
    }

    private String buscar(String k, String lengua) {
        Map<String, String> d = diccionario.get(lengua);
        String t = d.get(k);
        if (t != null) {
            return t;
        }
        List<String> valores = new ArrayList<>();
        Matcher m = TOK.matcher(k);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            valores.add(m.group());
            m.appendReplacement(sb, Matcher.quoteReplacement("{" + (valores.size() - 1) + "}"));
        }
        m.appendTail(sb);
        if (!valores.isEmpty()) {
            String plantilla = d.get(sb.toString());
            if (plantilla != null) {
                Matcher r = MARCAS.matcher(plantilla);
                StringBuilder out = new StringBuilder();
                while (r.find()) {
                    int i = Integer.parseInt(r.group(1));
                    r.appendReplacement(out, Matcher.quoteReplacement(i < valores.size() ? valores.get(i) : ""));
                }
                r.appendTail(out);
                return out.toString();
            }
        }
        return null;
    }

    private String nucleo(String k, String lengua, int profundidad) {
        if (!DOS_LETRAS.matcher(k).find()) {
            return null;
        }
        String t = buscar(k, lengua);
        if (t != null) {
            return t;
        }
        if (profundidad > 4) {
            return null;
        }
        String fecha = fechas(k, lengua);
        if (fecha != null) {
            String r = nucleo(fecha, lengua, profundidad + 1);
            return r != null ? r : fecha;
        }
        for (String sep : SEPARADORES) {
            int i = k.indexOf(sep);
            if (i > 0) {
                String a = k.substring(0, i);
                String b = k.substring(i + sep.length());
                String ta = nucleo(a, lengua, profundidad + 1);
                String tb = " (".equals(sep) ? nucleo(b.replaceAll("\\)$", ""), lengua, profundidad + 1)
                        : nucleo(b, lengua, profundidad + 1);
                if (ta != null || tb != null) {
                    String izquierda = ta != null ? ta : a;
                    String derecha = " (".equals(sep) && tb != null ? tb + ")" : (tb != null ? tb : b);
                    return izquierda + sep + derecha;
                }
            }
        }
        for (Prefijo p : PREFIJOS) {
            Matcher x = p.patron().matcher(k);
            if (x.matches()) {
                String cola = nucleo(x.group(1), lengua, profundidad + 1);
                String fin = x.groupCount() >= 2 && x.group(2) != null ? x.group(2) : "";
                return p.plantillas().get(lengua).replace("$", cola != null ? cola : x.group(1)) + fin;
            }
        }
        return null;
    }

    private String fechas(String k, String lengua) {
        List<String> d = dias.get(lengua);
        boolean cambio = false;
        Matcher m = FECHA_CON_DIA.matcher(k);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            cambio = true;
            int wi = DIAS_ES.indexOf(m.group(1).toLowerCase());
            int mi = MESES_ES.indexOf(m.group(3).toLowerCase());
            String anio = m.group(4) != null ? ", " + m.group(4) : "";
            String r = "en".equals(lengua) ? d.get(wi) + ", " + MESES_EN[mi] + " " + m.group(2) + anio
                    : d.get(wi) + ", " + m.group(2) + " " + MESES_ES.get(mi) + ("qu".equals(lengua) ? " killa" : " phaxsi") + anio;
            m.appendReplacement(sb, Matcher.quoteReplacement(r));
        }
        m.appendTail(sb);
        String r1 = sb.toString();
        Matcher n = FECHA_SIN_DIA.matcher(r1);
        StringBuilder sb2 = new StringBuilder();
        while (n.find()) {
            cambio = true;
            int mi = MESES_ES.indexOf(n.group(2).toLowerCase());
            String anio = n.group(3) != null ? ", " + n.group(3) : "";
            String r = "en".equals(lengua) ? MESES_EN[mi] + " " + n.group(1) + anio
                    : n.group(1) + " " + MESES_ES.get(mi) + ("qu".equals(lengua) ? " killa" : " phaxsi") + anio;
            n.appendReplacement(sb2, Matcher.quoteReplacement(r));
        }
        n.appendTail(sb2);
        String r2 = sb2.toString();
        if ("en".equals(lengua)) {
            Matcher c = DIA_CORTO.matcher(r2);
            StringBuilder sb3 = new StringBuilder();
            while (c.find()) {
                cambio = true;
                c.appendReplacement(sb3, Matcher.quoteReplacement(d.get(DIAS_ES_CORTOS.indexOf(c.group(1).toLowerCase())).substring(0, 3)));
            }
            c.appendTail(sb3);
            Matcher mc = MES_CORTO.matcher(sb3.toString());
            StringBuilder sb4 = new StringBuilder();
            while (mc.find()) {
                cambio = true;
                mc.appendReplacement(sb4, Matcher.quoteReplacement(MESES_EN[MESES_ES_CORTOS.indexOf(mc.group(2))].substring(0, 3) + " " + mc.group(1)));
            }
            mc.appendTail(sb4);
            r2 = sb4.toString();
        }
        return cambio ? r2 : null;
    }
}
