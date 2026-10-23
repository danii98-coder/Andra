package pe.edu.unaj.andara.util;

import java.time.DayOfWeek;
import java.time.LocalDate;

public final class DiasHabiles {

    private DiasHabiles() {
    }

    public static boolean esHabil(LocalDate fecha) {
        DayOfWeek d = fecha.getDayOfWeek();
        return d != DayOfWeek.SATURDAY && d != DayOfWeek.SUNDAY;
    }

    public static LocalDate sumar(LocalDate desde, int dias) {
        LocalDate d = desde;
        int restantes = dias;
        while (restantes > 0) {
            d = d.plusDays(1);
            if (esHabil(d)) {
                restantes--;
            }
        }
        return d;
    }
}
