package pe.edu.unaj.andara.ui;

import java.util.ArrayList;
import java.util.List;

public record Opcion(String valor, String etiqueta, boolean grupo) {

    public static Opcion de(String valor, String etiqueta) {
        return new Opcion(valor, etiqueta, false);
    }

    public static Opcion grupo(String etiqueta) {
        return new Opcion("", etiqueta, true);
    }

    public static List<Opcion> lista(List<String> valores) {
        List<Opcion> l = new ArrayList<>();
        valores.forEach(v -> l.add(de(v, v)));
        return l;
    }

    public static List<Opcion> pares(List<String[]> pares) {
        List<Opcion> l = new ArrayList<>();
        pares.forEach(p -> l.add(de(p[0], p[1])));
        return l;
    }
}
