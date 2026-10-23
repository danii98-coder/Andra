package pe.edu.unaj.andara.util;

import java.text.Normalizer;
import java.util.Locale;

public final class TextoPlano {

    private TextoPlano() {
    }

    public static String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        String sinTildes = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return sinTildes.toLowerCase(Locale.ROOT);
    }

    public static boolean contiene(String texto, String busqueda) {
        return busqueda == null || busqueda.isBlank() || normalizar(texto).contains(normalizar(busqueda.trim()));
    }

    public static String recortar(String texto, int maximo) {
        return texto == null || texto.length() <= maximo ? texto : texto.substring(0, maximo - 2) + "…";
    }
}
