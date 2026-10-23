package pe.edu.unaj.andara.util;

import pe.edu.unaj.andara.model.MetodoPago;

import java.time.YearMonth;

public final class TarjetaJemm {

    private TarjetaJemm() {
    }

    public static MetodoPago marca(String numero) {
        String d = Validacion.soloDigitos(numero);
        if (d.matches("^4.*")) {
            return MetodoPago.VISA;
        }
        if (d.matches("^(5[1-5]|2[2-7]).*")) {
            return MetodoPago.MASTERCARD;
        }
        if (d.matches("^3[47].*")) {
            return MetodoPago.AMEX;
        }
        if (d.matches("^3(0[0-5]|[68]).*")) {
            return MetodoPago.DINERS;
        }
        return null;
    }

    public static boolean luhn(String numero) {
        String d = Validacion.soloDigitos(numero);
        if (d.length() < 13) {
            return false;
        }
        int suma = 0;
        for (int i = 0; i < d.length(); i++) {
            int n = d.charAt(d.length() - 1 - i) - '0';
            if (i % 2 == 1) {
                n *= 2;
                if (n > 9) {
                    n -= 9;
                }
            }
            suma += n;
        }
        return suma % 10 == 0;
    }

    public static String formatear(String numero) {
        String d = Validacion.soloDigitos(numero);
        if (d.length() > 19) {
            d = d.substring(0, 19);
        }
        return d.replaceAll("(.{4})", "$1 ").trim();
    }

    public static String formatearVencimiento(String texto) {
        String d = Validacion.soloDigitos(texto);
        if (d.length() > 4) {
            d = d.substring(0, 4);
        }
        return d.length() > 2 ? d.substring(0, 2) + "/" + d.substring(2) : d;
    }

    public static boolean vencimientoValido(String mmaa, YearMonth actual) {
        if (mmaa == null || !mmaa.matches("\\d{2}/\\d{2}")) {
            return false;
        }
        int mes = Integer.parseInt(mmaa.substring(0, 2));
        int anio = 2000 + Integer.parseInt(mmaa.substring(3));
        if (mes < 1 || mes > 12) {
            return false;
        }
        return !YearMonth.of(anio, mes).isBefore(actual);
    }
}
