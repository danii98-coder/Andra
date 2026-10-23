package pe.edu.unaj.andara.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.unaj.andara.exception.NegocioException;
import pe.edu.unaj.andara.model.Documento;
import pe.edu.unaj.andara.model.DocumentoPersonal;
import pe.edu.unaj.andara.model.EscuelaProfesional;
import pe.edu.unaj.andara.model.TipoFavorito;
import pe.edu.unaj.andara.repository.DocumentoPersonalRepository;
import pe.edu.unaj.andara.util.Fechas;
import pe.edu.unaj.andara.util.TextoPlano;

import java.text.Collator;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

@Service
@RequiredArgsConstructor
public class DocumentoService {

    public record Criterio(String texto, String tipo, String dependencia, String publico, String rango, LocalDate desde,
                           LocalDate hasta) {
        public static Criterio vacio() {
            return new Criterio("", "", "", "", "any", null, null);
        }

        public Criterio conTipo(String nuevoTipo) {
            return new Criterio(texto, nuevoTipo, dependencia, publico, rango, desde, hasta);
        }
    }

    public record Rango(LocalDate desde, LocalDate hasta) {
    }

    public static final List<String[]> RANGOS = List.of(new String[]{"any", "Cualquier fecha"},
            new String[]{"30d", "Últimos 30 días"}, new String[]{"6m", "Últimos 6 meses"},
            new String[]{"year", "Este año (2026)"}, new String[]{"2025", "Año 2025"}, new String[]{"2024", "Año 2024"},
            new String[]{"custom", "Rango personalizado…"});
    public static final List<String[]> ORDENES = List.of(new String[]{"rel", "Relevancia"},
            new String[]{"az", "Nombre (A–Z)"}, new String[]{"new", "Más recientes"}, new String[]{"old", "Más antiguos"},
            new String[]{"fav", "Favoritos primero"});
    public static final List<String[]> PRESETS = List.of(new String[]{"fav", "Mis favoritos"},
            new String[]{"norm", "Normativa"}, new String[]{"form", "Formatos y solicitudes"},
            new String[]{"y26", "Publicados en 2026"});
    public static final List<String> PUBLICOS = List.of("Estudiantes", "Docentes", "Administrativos", "Público en General");
    public static final List<String> TIPOS_RAPIDOS = List.of("Malla curricular", "Sílabos", "Certificados", "Constancias",
            "Récord académico", "Reglamentos");

    private final CatalogoService catalogo;
    private final FavoritoService favoritos;
    private final DocumentoPersonalRepository personales;

    public static Rango limites(Criterio c, LocalDate hoy) {
        String anio = String.valueOf(hoy.getYear());
        return switch (c.rango() == null ? "any" : c.rango()) {
            case "30d" -> new Rango(hoy.minusDays(30), hoy);
            case "6m" -> new Rango(hoy.minusDays(182), hoy);
            case "year" -> new Rango(LocalDate.parse(anio + "-01-01"), LocalDate.parse(anio + "-12-31"));
            case "2025" -> new Rango(LocalDate.parse("2025-01-01"), LocalDate.parse("2025-12-31"));
            case "2024" -> new Rango(LocalDate.parse("2024-01-01"), LocalDate.parse("2024-12-31"));
            case "custom" -> new Rango(c.desde() != null ? c.desde() : LocalDate.MIN, c.hasta() != null ? c.hasta() : LocalDate.MAX);
            default -> null;
        };
    }

    public static String validarRangoPersonalizado(Criterio c) {
        if ("custom".equals(c.rango()) && c.desde() != null && c.hasta() != null && c.desde().isAfter(c.hasta())) {
            return "La fecha final debe ser posterior a la inicial.";
        }
        return null;
    }

    public static List<Documento> filtrar(List<Documento> todos, Criterio c, EscuelaProfesional escuela, LocalDate hoy) {
        Rango r = limites(c, hoy);
        return todos.stream().filter(d -> TextoPlano.contiene(d.getNombre() + " " + d.getDescripcion() + " " + d.getTipo()
                        + " " + d.getDependencia(), c.texto()))
                .filter(d -> c.tipo() == null || c.tipo().isBlank() || d.getTipo().equals(c.tipo()))
                .filter(d -> c.dependencia() != null && !c.dependencia().isBlank() ? d.getDependencia().equals(c.dependencia())
                        : escuela == null || d.getEscuela() == null || d.getEscuela().getCodigo().equals(escuela.getCodigo()))
                .filter(d -> c.publico() == null || c.publico().isBlank() || d.getPublico().equals(c.publico()))
                .filter(d -> r == null || (!d.getFecha().isBefore(r.desde()) && !d.getFecha().isAfter(r.hasta())))
                .toList();
    }

    public Map<String, Predicate<Documento>> predicados() {
        Set<String> fav = favoritos.codigos(TipoFavorito.DOCUMENTO);
        return Map.of("fav", d -> fav.contains(d.getCodigo()),
                "norm", d -> List.of("Reglamentos", "Directivas", "Resoluciones").contains(d.getTipo()),
                "form", d -> List.of("Formatos y plantillas", "Solicitudes y formularios").contains(d.getTipo()),
                "y26", d -> d.getFecha().getYear() == 2026);
    }

    public static List<Documento> ordenar(List<Documento> lista, String orden, boolean conRango, Set<String> fav) {
        List<Documento> l = new ArrayList<>(lista);
        Collator es = Collator.getInstance(Locale.forLanguageTag("es"));
        switch (orden) {
            case "az" -> l.sort(Comparator.comparing(Documento::getNombre, es));
            case "new" -> l.sort(Comparator.comparing(Documento::getFecha).reversed());
            case "old" -> l.sort(Comparator.comparing(Documento::getFecha));
            case "fav" -> l.sort(Comparator.comparing((Documento d) -> fav.contains(d.getCodigo()) ? 0 : 1));
            default -> {
                if (conRango) {
                    l.sort(Comparator.comparing(Documento::getFecha).reversed());
                }
            }
        }
        return l;
    }

    @Transactional(readOnly = true)
    public List<Documento> buscar(Criterio c, EscuelaProfesional escuela, EstadoFiltro filtros, String orden) {
        List<Documento> l = filtrar(catalogo.documentos(), c, escuela, Fechas.hoy());
        l = FiltroService.aplicar(l, filtros, predicados(),
                d -> d.getNombre() + " " + d.getDescripcion() + " " + d.getTipo() + " " + d.getDependencia());
        return ordenar(l, orden, limites(c, Fechas.hoy()) != null, favoritos.codigos(TipoFavorito.DOCUMENTO));
    }

    @Transactional(readOnly = true)
    public List<DocumentoPersonal> misDocumentos() {
        return personales.findAllByOrderByIdAsc();
    }

    @Transactional(readOnly = true)
    public DocumentoPersonal miDocumento(String codigo) {
        return personales.findByCodigo(codigo).orElseThrow(() -> new NegocioException("El documento no existe."));
    }

    public static String etiquetaRango(String clave) {
        return RANGOS.stream().filter(r -> r[0].equals(clave)).map(r -> r[1]).findFirst().orElse("Cualquier fecha");
    }

    public static String etiquetaOrden(String clave) {
        return ORDENES.stream().filter(r -> r[0].equals(clave)).map(r -> r[1]).findFirst().orElse("Relevancia");
    }
}
