package pe.edu.unaj.andara.ui;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.layout.*;

import java.util.List;

public final class Tabla {

    private Tabla() {
    }

    private static GridPane fila(double[] anchos, String clase) {
        GridPane g = new GridPane();
        g.getStyleClass().add(clase);
        for (double a : anchos) {
            ColumnConstraints c = new ColumnConstraints();
            c.setPercentWidth(a);
            c.setFillWidth(true);
            g.getColumnConstraints().add(c);
        }
        return g;
    }

    public static VBox crear(String[] cabeceras, double[] anchos, boolean[] derecha, List<Node[]> filas) {
        VBox t = new VBox();
        t.getStyleClass().add("tbl");
        GridPane cab = fila(anchos, "tbl-h");
        for (int i = 0; i < cabeceras.length; i++) {
            HBox celda = new HBox(Componentes.mayus(cabeceras[i]));
            celda.setAlignment(derecha != null && derecha[i] ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
            cab.add(celda, i, 0);
        }
        t.getChildren().add(cab);
        for (Node[] f : filas) {
            GridPane r = fila(anchos, "tbl-r");
            for (int i = 0; i < f.length; i++) {
                HBox celda = new HBox(f[i]);
                celda.setAlignment(derecha != null && derecha[i] ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
                celda.setMinWidth(0);
                r.add(celda, i, 0);
            }
            t.getChildren().add(r);
        }
        Componentes.marcarUltima(t);
        return t;
    }

    public static VBox celda(String principal, String secundario) {
        VBox v = new VBox(2, Componentes.literalParrafo(principal, "strong", "small"));
        if (secundario != null) {
            v.getChildren().add(Componentes.parrafo(secundario, "hint"));
        }
        return v;
    }
}
