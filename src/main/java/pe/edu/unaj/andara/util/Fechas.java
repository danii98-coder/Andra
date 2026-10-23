package pe.edu.unaj.andara.util;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class Fechas {

    public static final String[] MESES = {"enero", "febrero", "marzo", "abril", "mayo", "junio", "julio",
            "agosto", "septiembre", "octubre", "noviembre", "diciembre"};
    public static final String[] MESES_CORTOS = {"ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep",
            "oct", "nov", "dic"};
    public static final String[] DIAS = {"lunes", "martes", "miércoles", "jueves", "viernes", "sábado", "domingo"};

    private static final DateTimeFormatter DMY = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter HM = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter ISO_MIN = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm", Locale.ROOT);

    private static Clock reloj = Clock.systemDefaultZone();

    private Fechas() {
    }

    public static void fijarReloj(Clock nuevo) {
        reloj = nuevo == null ? Clock.systemDefaultZone() : nuevo;
    }

    public static Clock reloj() {
        return reloj;
    }

    public static LocalDate hoy() {
        return LocalDate.now(reloj);
    }

    public static LocalDateTime ahora() {
        return LocalDateTime.now(reloj).withSecond(0).withNano(0);
    }

    public static long milis() {
        return reloj.millis();
    }

    public static String dmy(LocalDate fecha) {
        return fecha == null ? "" : fecha.format(DMY);
    }

    public static String hm(LocalDateTime momento) {
        return momento == null ? "" : momento.format(HM);
    }

    public static String dmyHm(LocalDateTime momento) {
        return momento == null ? "" : dmy(momento.toLocalDate()) + " " + hm(momento);
    }

    public static String capitalizar(String texto) {
        return texto == null || texto.isEmpty() ? texto : Character.toUpperCase(texto.charAt(0)) + texto.substring(1);
    }

    public static String dia(LocalDate fecha) {
        return DIAS[fecha.getDayOfWeek().getValue() - 1];
    }

    public static String largo(LocalDate fecha) {
        return capitalizar(dia(fecha)) + ", " + String.format("%02d", fecha.getDayOfMonth()) + " de "
                + MESES[fecha.getMonthValue() - 1] + ", " + fecha.getYear();
    }

    public static String largoSinDia(LocalDate fecha) {
        return String.format("%02d", fecha.getDayOfMonth()) + " de " + MESES[fecha.getMonthValue() - 1] + ", "
                + fecha.getYear();
    }

    public static String corto(LocalDate fecha) {
        return capitalizar(dia(fecha).substring(0, 3)) + " " + fecha.getDayOfMonth() + " "
                + MESES_CORTOS[fecha.getMonthValue() - 1];
    }

    public static String mesAnio(LocalDate fecha) {
        return capitalizar(MESES[fecha.getMonthValue() - 1]) + " " + fecha.getYear();
    }

    public static String hora12(String hhmm) {
        LocalTime t = LocalTime.parse(hhmm);
        int h = (t.getHour() + 11) % 12 + 1;
        return h + ":" + String.format("%02d", t.getMinute()) + (t.getHour() >= 12 ? " p. m." : " a. m.");
    }

    public static String iso(LocalDateTime momento) {
        return momento.format(ISO_MIN);
    }

    public static LocalDateTime leerMomento(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        return texto.length() <= 10 ? LocalDate.parse(texto).atTime(9, 0) : LocalDateTime.parse(texto);
    }

    public static LocalDate leerFecha(String texto) {
        return texto == null || texto.isBlank() ? null : LocalDate.parse(texto.substring(0, 10));
    }

    public static LocalDate desdeTexto(String texto) {
        if (texto == null) {
            return null;
        }
        String t = TextoPlano.normalizar(texto);
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d{1,2}) de (\\w+),? (\\d{4})").matcher(t);
        if (m.find()) {
            String mes = m.group(2).equals("setiembre") ? "septiembre" : m.group(2);
            for (int i = 0; i < MESES.length; i++) {
                if (TextoPlano.normalizar(MESES[i]).equals(mes)) {
                    return LocalDate.of(Integer.parseInt(m.group(3)), i + 1, Integer.parseInt(m.group(1)));
                }
            }
        }
        java.util.regex.Matcher d = java.util.regex.Pattern.compile("(\\d{2})/(\\d{2})/(\\d{4})").matcher(t);
        if (d.find()) {
            return LocalDate.of(Integer.parseInt(d.group(3)), Integer.parseInt(d.group(2)), Integer.parseInt(d.group(1)));
        }
        return null;
    }
}
