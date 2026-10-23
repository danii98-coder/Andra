package pe.edu.unaj.andara.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.unaj.andara.model.*;
import pe.edu.unaj.andara.repository.ExpedienteRepository;
import pe.edu.unaj.andara.repository.PagoRepository;
import pe.edu.unaj.andara.util.Fechas;
import pe.edu.unaj.andara.util.Hash;
import pe.edu.unaj.andara.util.TextoPlano;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AnaliticaService {

    public static final String[] DIAS = {"Lun", "Mar", "Mié", "Jue", "Vie"};
    public static final String[] DIAS_LARGOS = {"lunes", "martes", "miércoles", "jueves", "viernes"};
    public static final String[] RAMPA = {"#EEF5FD", "#CFE3FA", "#9CC6F4", "#5AA0EA", "#0071E3"};
    private static final double[] PESO_DIA = {1.6, 1.1, 1, 1.2, 0.8};
    private static final double[] PESO_FRANJA = {1.3, 1.7, 0.9, 1.1, 0.7};

    public record Barra(String etiqueta, double valor) {
    }

    public record Hallazgo(String antes, String negrita, String despues) {
    }

    public record Analisis(int tramitesYOrdenes, BigDecimal recaudacion, int usoPromedio, List<Barra> top,
                           List<Barra> porSede, int[][] calor, int calorMaximo, int[] pico, List<Barra> subutilizados,
                           List<Barra> demandados, List<Hallazgo> hallazgos) {
    }

    private final PagoRepository pagos;
    private final ExpedienteRepository expedientes;
    private final CatalogoService catalogo;
    private final ContextoService contexto;

    public static int dias(String periodo) {
        return "sem".equals(periodo) ? 120 : Integer.parseInt(periodo);
    }

    public static int[][] mapaCalor(List<Pago> enPeriodo) {
        int[][] calor = new int[5][5];
        for (int di = 0; di < 5; di++) {
            for (int si = 0; si < 5; si++) {
                int dia = di + 1;
                int desde = 8 + si * 2;
                long real = enPeriodo.stream().filter(p -> p.getCreado().getDayOfWeek().getValue() == dia
                        && p.getCreado().getHour() >= desde && p.getCreado().getHour() < desde + 2).count();
                calor[di][si] = (int) Math.round(PESO_DIA[di] * PESO_FRANJA[si] * 8
                        + Hash.numero(DIAS[di] + CatalogoService.FRANJAS.get(si)) % 5 + real * 3);
            }
        }
        return calor;
    }

    public static int[] pico(int[][] calor) {
        int[] p = {0, 0};
        for (int i = 0; i < calor.length; i++) {
            for (int j = 0; j < calor[i].length; j++) {
                if (calor[i][j] > calor[p[0]][p[1]]) {
                    p = new int[]{i, j};
                }
            }
        }
        return p;
    }

    public static int nivel(int valor, int maximo) {
        return Math.min(4, (int) Math.floor(valor / (double) maximo * 4.999));
    }

    public static int uso(Espacio e) {
        int ocupadas = 0;
        for (int d = 0; d < 5; d++) {
            for (String t : CatalogoService.FRANJAS) {
                if (e.isFueraDeServicio() || !ReservaService.disponibleSimulado(e.getNombre(), d, t)) {
                    ocupadas++;
                }
            }
        }
        return (int) Math.round(ocupadas / 25.0 * 100);
    }

    @Transactional(readOnly = true)
    public Analisis analizar(String periodo) {
        LocalDate desde = Fechas.hoy().minusDays(dias(periodo) - 1L);
        List<Pago> todos = pagos.findAllByOrderByCreadoDesc();
        List<Pago> pg = todos.stream().filter(p -> contexto.incluyeSede(p.getSede()) && !p.getCreado().toLocalDate().isBefore(desde)).toList();
        List<Pago> pagados = pg.stream().filter(Pago::isPagado).toList();
        Map<String, Integer> porConcepto = new LinkedHashMap<>();
        pg.stream().filter(p -> p.getTipo() != TipoPago.MATRICULA).forEach(p -> porConcepto.merge(p.getConcepto(), 1, Integer::sum));
        expedientes.findAllByOrderByIdDesc().stream().filter(e -> contexto.incluyeSede(e.getSede()) && e.getPago() == null)
                .forEach(e -> porConcepto.merge(TextoPlano.recortar(e.getTitulo(), 40), 1, Integer::sum));
        List<Map.Entry<String, Integer>> orden = new ArrayList<>(porConcepto.entrySet());
        orden.sort((a, b) -> b.getValue() - a.getValue());
        List<Barra> top = orden.stream().limit(6).map(e -> new Barra(e.getKey(), e.getValue())).toList();
        int totalConceptos = Math.max(1, porConcepto.values().stream().mapToInt(Integer::intValue).sum());

        int[][] calor = mapaCalor(pg);
        int max = Arrays.stream(calor).flatMapToInt(Arrays::stream).max().orElse(1);
        int[] pico = pico(calor);

        List<Espacio> enContexto = catalogo.espacios().stream().filter(e -> contexto.incluye(e.getSede(), e.getEscuela())).toList();
        List<Barra> usos = new ArrayList<>(enContexto.stream().map(e -> new Barra(e.getNombre(), uso(e))).toList());
        usos.sort(Comparator.comparingDouble(Barra::valor));
        int promedio = (int) Math.round(usos.stream().mapToDouble(Barra::valor).average().orElse(0));
        List<Barra> demandados = new ArrayList<>(usos.subList(Math.max(0, usos.size() - 3), usos.size()));
        Collections.reverse(demandados);

        List<Barra> porSede = catalogo.sedes().stream().map(s -> new Barra(s.getNombre(), PagoService.total(todos.stream()
                .filter(p -> p.isPagado() && p.getPagado() != null && p.getSede().getId().equals(s.getId())
                        && !p.getPagado().toLocalDate().isBefore(desde)).toList()).doubleValue())).toList();

        List<Hallazgo> hallazgos = new ArrayList<>();
        if (!top.isEmpty()) {
            hallazgos.add(new Hallazgo("", top.get(0).etiqueta(), " es el trámite más pedido ("
                    + Math.round(top.get(0).valor() / totalConceptos * 100) + " % del periodo). Conviene emitirlo de forma automática."));
        }
        String franja = CatalogoService.FRANJAS.get(pico[1]);
        hallazgos.add(new Hallazgo("Las ventanillas se saturan los ", DIAS_LARGOS[pico[0]] + " de " + franja + " a "
                + String.format("%02d:00", Integer.parseInt(franja.substring(0, 2)) + 2),
                ". Sugerimos abrir una ventanilla adicional en esa franja."));
        if (!usos.isEmpty()) {
            hallazgos.add(new Hallazgo("", usos.get(0).etiqueta(), " se usa solo el " + (int) usos.get(0).valor()
                    + " % del tiempo. Podría abrirse a otras escuelas o a talleres."));
        }
        hallazgos.add(new Hallazgo(Math.round(pagados.size() / (double) Math.max(1, pg.size()) * 100)
                + " % de las órdenes del periodo ya están pagadas con Andara Pay, sin colas en el banco.", "", ""));
        return new Analisis(pg.size(), PagoService.total(pagados), promedio, top, porSede, calor, max, pico,
                usos.subList(0, Math.min(4, usos.size())), demandados, hallazgos);
    }
}
