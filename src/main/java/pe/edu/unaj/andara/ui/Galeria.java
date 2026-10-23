package pe.edu.unaj.andara.ui;

import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.layout.Pane;

import java.util.ArrayList;
import java.util.List;

public class Galeria extends Pane {

    private static final double MINIMO = 250;
    private static final double SEPARACION = 22;

    private record Lugar(Node nodo, int fila, int columna, int ancho, int alto) {
    }

    public Galeria() {
        getStyleClass().add("gal");
    }

    public static void destacar(Node n) {
        n.getProperties().put("destacado", Boolean.TRUE);
    }

    private static boolean destacado(Node n) {
        return Boolean.TRUE.equals(n.getProperties().get("destacado"));
    }

    @Override
    public Orientation getContentBias() {
        return Orientation.HORIZONTAL;
    }

    private int columnas(double ancho) {
        return Math.max(1, (int) Math.floor((ancho + SEPARACION) / (MINIMO + SEPARACION)));
    }

    private List<Lugar> ubicar(int cols) {
        List<Lugar> lugares = new ArrayList<>();
        List<boolean[]> ocupado = new ArrayList<>();
        int fila = 0;
        int col = 0;
        for (Node n : getManagedChildren()) {
            int an = destacado(n) && cols >= 2 ? 2 : 1;
            int al = an;
            while (true) {
                while (ocupado.size() < fila + al) {
                    ocupado.add(new boolean[cols]);
                }
                if (col + an <= cols && libre(ocupado, fila, col, an, al)) {
                    break;
                }
                col++;
                if (col >= cols) {
                    col = 0;
                    fila++;
                }
            }
            for (int r = fila; r < fila + al; r++) {
                for (int c = col; c < col + an; c++) {
                    ocupado.get(r)[c] = true;
                }
            }
            lugares.add(new Lugar(n, fila, col, an, al));
            col += an;
            if (col >= cols) {
                col = 0;
                fila++;
            }
        }
        return lugares;
    }

    private static boolean libre(List<boolean[]> ocupado, int fila, int col, int an, int al) {
        for (int r = fila; r < fila + al; r++) {
            for (int c = col; c < col + an; c++) {
                if (ocupado.get(r)[c]) {
                    return false;
                }
            }
        }
        return true;
    }

    private double[] altos(List<Lugar> lugares, double anchoColumna) {
        int filas = lugares.stream().mapToInt(l -> l.fila() + l.alto()).max().orElse(0);
        double[] h = new double[filas];
        for (Lugar l : lugares) {
            if (l.alto() == 1) {
                h[l.fila()] = Math.max(h[l.fila()], l.nodo().prefHeight(anchoColumna));
            }
        }
        for (Lugar l : lugares) {
            if (l.alto() > 1) {
                double w = anchoColumna * l.ancho() + SEPARACION * (l.ancho() - 1);
                double necesario = l.nodo().prefHeight(w);
                double actual = SEPARACION * (l.alto() - 1);
                for (int r = l.fila(); r < l.fila() + l.alto(); r++) {
                    actual += h[r];
                }
                if (necesario > actual) {
                    h[l.fila() + l.alto() - 1] += necesario - actual;
                }
            }
        }
        return h;
    }

    @Override
    protected double computePrefHeight(double ancho) {
        double w = ancho < 0 ? Math.max(getWidth(), MINIMO) : ancho;
        int cols = columnas(w);
        double colW = (w - (cols - 1) * SEPARACION) / cols;
        double[] h = altos(ubicar(cols), colW);
        double total = 0;
        for (double x : h) {
            total += x;
        }
        return total + Math.max(0, h.length - 1) * SEPARACION;
    }

    @Override
    protected double computePrefWidth(double alto) {
        return MINIMO;
    }

    @Override
    protected double computeMinWidth(double alto) {
        return MINIMO;
    }

    @Override
    protected double computeMinHeight(double ancho) {
        return computePrefHeight(ancho);
    }

    @Override
    protected void layoutChildren() {
        double w = getWidth();
        int cols = columnas(w);
        double colW = (w - (cols - 1) * SEPARACION) / cols;
        List<Lugar> lugares = ubicar(cols);
        double[] h = altos(lugares, colW);
        double[] y = new double[h.length + 1];
        for (int i = 0; i < h.length; i++) {
            y[i + 1] = y[i] + h[i] + SEPARACION;
        }
        for (Lugar l : lugares) {
            double x = l.columna() * (colW + SEPARACION);
            double ancho = colW * l.ancho() + SEPARACION * (l.ancho() - 1);
            double alto = y[l.fila() + l.alto()] - y[l.fila()] - SEPARACION;
            l.nodo().resizeRelocate(snapPositionX(x), snapPositionY(y[l.fila()]), snapSizeX(ancho), snapSizeY(alto));
        }
    }
}
