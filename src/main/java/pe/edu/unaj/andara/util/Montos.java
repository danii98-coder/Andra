package pe.edu.unaj.andara.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public final class Montos {

    private static final DecimalFormatSymbols SIMBOLOS = DecimalFormatSymbols.getInstance(Locale.US);

    private Montos() {
    }

    public static String soles(BigDecimal monto) {
        DecimalFormat f = new DecimalFormat("#,##0.00", SIMBOLOS);
        return "S/ " + f.format(monto == null ? BigDecimal.ZERO : monto.setScale(2, RoundingMode.HALF_UP));
    }

    public static String soles(double monto) {
        return soles(BigDecimal.valueOf(monto));
    }

    public static String miles(long valor) {
        return new DecimalFormat("#,##0", SIMBOLOS).format(valor);
    }

    public static String entero(BigDecimal monto) {
        return String.valueOf(monto.setScale(0, RoundingMode.HALF_UP).intValue());
    }
}
