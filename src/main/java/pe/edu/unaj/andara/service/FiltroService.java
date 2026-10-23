package pe.edu.unaj.andara.service;

import pe.edu.unaj.andara.util.Fechas;
import pe.edu.unaj.andara.util.TextoPlano;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

public final class FiltroService {

    public record Accesores<T>(Function<T, LocalDate> fecha, Function<T, String> codigo, Map<String, Function<T, String>> campos) {
    }

    private FiltroService() {
    }

    public static <T> List<T> aplicar(List<T> lista, EstadoFiltro estado, Map<String, Predicate<T>> predicados,
                                      Function<T, String> textoDe) {
        List<T> l = lista;
        for (String clave : estado.getActivos()) {
            Predicate<T> p = predicados.get(clave);
            if (p != null) {
                l = l.stream().filter(p).toList();
            }
        }
        if (!estado.getTexto().isBlank()) {
            l = l.stream().filter(x -> TextoPlano.contiene(textoDe.apply(x), estado.getTexto())).toList();
        }
        return l;
    }

    public static <T> List<T> aplicarAvanzado(List<T> lista, EstadoFiltro estado, Accesores<T> acc) {
        List<T> l = lista;
        String desde = estado.avanzado("from");
        String hasta = estado.avanzado("to");
        if (desde != null) {
            LocalDate d = LocalDate.parse(desde);
            l = l.stream().filter(x -> {
                LocalDate f = acc.fecha().apply(x);
                return f != null && !f.isBefore(d);
            }).toList();
        }
        if (hasta != null) {
            LocalDate h = LocalDate.parse(hasta);
            l = l.stream().filter(x -> {
                LocalDate f = acc.fecha().apply(x);
                return f != null && !f.isAfter(h);
            }).toList();
        }
        String codigo = estado.avanzado("code");
        if (codigo != null) {
            l = l.stream().filter(x -> TextoPlano.contiene(acc.codigo().apply(x), codigo)).toList();
        }
        for (Map.Entry<String, Function<T, String>> campo : acc.campos().entrySet()) {
            String valor = estado.avanzado(campo.getKey());
            if (valor != null) {
                l = l.stream().filter(x -> valor.equals(campo.getValue().apply(x))).toList();
            }
        }
        return l;
    }

    public static <T> Map<String, Predicate<T>> predicadosFecha(Function<T, LocalDate> fecha) {
        LocalDate hoy = Fechas.hoy();
        Map<String, Predicate<T>> m = new HashMap<>();
        m.put("today", x -> hoy.equals(fecha.apply(x)));
        m.put("d7", x -> {
            LocalDate f = fecha.apply(x);
            return f != null && !f.isBefore(hoy.minusDays(6));
        });
        m.put("month", x -> {
            LocalDate f = fecha.apply(x);
            return f != null && f.getYear() == hoy.getYear() && f.getMonth() == hoy.getMonth();
        });
        return m;
    }

    public static String validarRango(String desde, String hasta) {
        if (desde != null && !desde.isBlank() && hasta != null && !hasta.isBlank()
                && LocalDate.parse(desde).isAfter(LocalDate.parse(hasta))) {
            return "La fecha “Hasta” debe ser posterior a “Desde”.";
        }
        return null;
    }
}
