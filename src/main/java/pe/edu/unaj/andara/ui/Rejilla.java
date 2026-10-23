package pe.edu.unaj.andara.ui;

import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.layout.Pane;

import java.util.List;

public class Rejilla extends Pane {

    private int columnas;
    private double anchoMinimo;
    private double hgap = 24;
    private double vgap = 24;

    public Rejilla(int columnas, double anchoMinimo) {
        this.columnas = columnas;
        this.anchoMinimo = anchoMinimo;
        getStyleClass().add("rejilla");
    }

    public Rejilla(int columnas, double anchoMinimo, double separacion) {
        this(columnas, anchoMinimo);
        this.hgap = separacion;
        this.vgap = separacion;
    }

    @Override
    public Orientation getContentBias() {
        return Orientation.HORIZONTAL;
    }

    private int columnasPara(double ancho) {
        int c = columnas;
        while (c > 1 && (ancho - (c - 1) * hgap) / c < anchoMinimo) {
            c--;
        }
        return c;
    }

    @Override
    protected double computePrefWidth(double alto) {
        Insets in = getInsets();
        return in.getLeft() + in.getRight() + anchoMinimo;
    }

    @Override
    protected double computeMinWidth(double alto) {
        return computePrefWidth(alto);
    }

    @Override
    protected double computePrefHeight(double ancho) {
        Insets in = getInsets();
        double util = (ancho < 0 ? Math.max(getWidth(), anchoMinimo) : ancho) - in.getLeft() - in.getRight();
        List<Node> hijos = getManagedChildren();
        if (hijos.isEmpty()) {
            return in.getTop() + in.getBottom();
        }
        int c = columnasPara(util);
        double w = Math.max(0, (util - (c - 1) * hgap) / c);
        double total = 0;
        for (int i = 0; i < hijos.size(); i += c) {
            total += altoFila(hijos, i, c, w);
        }
        total += vgap * (Math.ceil(hijos.size() / (double) c) - 1);
        return in.getTop() + in.getBottom() + total;
    }

    @Override
    protected double computeMinHeight(double ancho) {
        return computePrefHeight(ancho);
    }

    @Override
    protected void layoutChildren() {
        Insets in = getInsets();
        double util = getWidth() - in.getLeft() - in.getRight();
        List<Node> hijos = getManagedChildren();
        int c = columnasPara(util);
        double w = Math.max(0, (util - (c - 1) * hgap) / c);
        double y = in.getTop();
        for (int i = 0; i < hijos.size(); i += c) {
            double h = altoFila(hijos, i, c, w);
            for (int j = i; j < Math.min(i + c, hijos.size()); j++) {
                double x = in.getLeft() + (j - i) * (w + hgap);
                hijos.get(j).resizeRelocate(snapPositionX(x), snapPositionY(y), snapSizeX(w), snapSizeY(h));
            }
            y += h + vgap;
        }
    }

    private double altoFila(List<Node> hijos, int desde, int c, double w) {
        double h = 0;
        for (int j = desde; j < Math.min(desde + c, hijos.size()); j++) {
            h = Math.max(h, hijos.get(j).prefHeight(w));
        }
        return h;
    }
}
