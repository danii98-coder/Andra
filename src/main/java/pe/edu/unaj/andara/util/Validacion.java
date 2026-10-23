package pe.edu.unaj.andara.util;

public final class Validacion {

    private Validacion() {
    }

    public static boolean dni(String valor) {
        return valor != null && valor.trim().matches("\\d{8}");
    }

    public static boolean celular(String valor) {
        return valor != null && valor.replaceAll("\\s", "").matches("9\\d{8}");
    }

    public static boolean correo(String valor) {
        return valor != null && valor.trim().matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+");
    }

    public static boolean codigoAprobacion(String valor) {
        return valor != null && valor.trim().matches("\\d{6}");
    }

    public static boolean cvv(String valor) {
        return valor != null && valor.trim().matches("\\d{3,4}");
    }

    public static String soloDigitos(String valor) {
        return valor == null ? "" : valor.replaceAll("\\D", "");
    }
}
