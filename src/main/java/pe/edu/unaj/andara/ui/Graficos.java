package pe.edu.unaj.andara.ui;

import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.*;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import pe.edu.unaj.andara.service.AdminService;
import pe.edu.unaj.andara.service.AnaliticaService;
import pe.edu.unaj.andara.service.CatalogoService;
import pe.edu.unaj.andara.service.IdiomaService;
import pe.edu.unaj.andara.util.Montos;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public final class Graficos {

    private Graficos() {
    }

    public static Pane barrasDias(List<AdminService.Dia> dias) {
        Pane lienzo = new Pane();
        lienzo.widthProperty().addListener((obs, anterior, nuevo) -> {
            {
                lienzo.getChildren().clear();
                double w = nuevo.doubleValue();
                double alto = 180;
                double max = Math.max(1, dias.stream().mapToDouble(d -> d.monto().doubleValue()).max().orElse(1));
                for (double f : new double[]{0, 0.5, 1}) {
                    double y = alto - alto * f * 0.9;
                    Line l = new Line(0, y, w, y);
                    l.getStyleClass().add("grid-l");
                    lienzo.getChildren().add(l);
                }
                double bw = w / dias.size();
                for (int i = 0; i < dias.size(); i++) {
                    AdminService.Dia d = dias.get(i);
                    double h = Math.max(2, d.monto().doubleValue() / max * alto * 0.9);
                    Rectangle r = new Rectangle(i * bw + bw * 0.22, alto - h, bw * 0.56, h);
                    r.setArcWidth(12);
                    r.setArcHeight(12);
                    r.getStyleClass().add("bar");
                    if (d.hoy()) {
                        r.getStyleClass().add("now");
                    }
                    Tooltip.install(r, new Tooltip(IdiomaService.t(d.etiqueta()) + ": " + Montos.soles(d.monto())));
                    Text et = new Text();
                    et.textProperty().bind(IdiomaService.enlace(d.etiqueta()));
                    et.getStyleClass().add("bl");
                    et.setTextAlignment(TextAlignment.CENTER);
                    et.setWrappingWidth(bw);
                    et.setX(i * bw);
                    et.setY(alto + 18);
                    lienzo.getChildren().addAll(r, et);
                    if (d.monto().signum() > 0) {
                        Text v = new Text(Montos.entero(d.monto()));
                        v.getStyleClass().add("bv");
                        v.setTextAlignment(TextAlignment.CENTER);
                        v.setWrappingWidth(bw);
                        v.setX(i * bw);
                        v.setY(alto - h - 6);
                        lienzo.getChildren().add(v);
                    }
                }
            }
        });
        lienzo.setPrefWidth(400);
        lienzo.setMinWidth(0);
        lienzo.setMinHeight(214);
        lienzo.setPrefHeight(214);
        Componentes.accesible(lienzo, "Recaudación de los últimos 7 días");
        return lienzo;
    }

    public static VBox porMetodo(Map<String, BigDecimal> montos) {
        double total = Math.max(1, montos.values().stream().mapToDouble(BigDecimal::doubleValue).sum());
        HBox barra = new HBox();
        barra.getStyleClass().add("stack");
        Rectangle recorte = new Rectangle();
        recorte.setArcWidth(14);
        recorte.setArcHeight(14);
        recorte.widthProperty().bind(barra.widthProperty());
        recorte.heightProperty().bind(barra.heightProperty());
        barra.setClip(recorte);
        VBox leyenda = new VBox(8);
        montos.forEach((k, v) -> {
            if (v.signum() <= 0) {
                return;
            }
            String clase = "sg-" + k.toLowerCase();
            Region seg = new Region();
            seg.getStyleClass().add(clase);
            seg.prefWidthProperty().bind(barra.widthProperty().multiply(v.doubleValue() / total));
            seg.setMinWidth(0);
            Tooltip.install(seg, new Tooltip(k + ": " + Montos.soles(v)));
            barra.getChildren().add(seg);
            Region punto = new Region();
            punto.getStyleClass().addAll("leg-dot", clase);
            HBox fila = new HBox(8, punto, Componentes.etiqueta(k), Componentes.espacio(), Componentes.literal(Montos.soles(v), "strong"));
            fila.setAlignment(Pos.CENTER_LEFT);
            leyenda.getChildren().add(fila);
        });
        VBox.setMargin(leyenda, new javafx.geometry.Insets(12, 0, 0, 0));
        return new VBox(barra, leyenda);
    }

    public static VBox leyendaSimple(Map<String, BigDecimal> montos) {
        VBox v = new VBox(8);
        montos.forEach((k, m) -> {
            HBox fila = new HBox(8, Componentes.etiqueta(k), Componentes.espacio(), Componentes.literal(Montos.soles(m), "strong"));
            fila.setAlignment(Pos.CENTER_LEFT);
            v.getChildren().add(fila);
        });
        return v;
    }

    public static VBox barrasHorizontales(List<AnaliticaService.Barra> barras, Function<AnaliticaService.Barra, String> valor,
                                          boolean porcentaje) {
        VBox v = new VBox(10);
        double max = Math.max(1, barras.stream().mapToDouble(AnaliticaService.Barra::valor).max().orElse(1));
        for (AnaliticaService.Barra b : barras) {
            Label etiqueta = Componentes.literal(b.etiqueta(), "hb-l");
            etiqueta.setMinWidth(0);
            etiqueta.setTextOverrun(javafx.scene.control.OverrunStyle.ELLIPSIS);
            StackPane pista = new StackPane();
            pista.getStyleClass().add("hb-t");
            Region relleno = new Region();
            relleno.getStyleClass().add("hb-i");
            double fraccion = porcentaje ? b.valor() / 100.0 : b.valor() / max;
            relleno.maxWidthProperty().bind(pista.widthProperty().multiply(Math.max(0, Math.min(1, fraccion))));
            StackPane.setAlignment(relleno, Pos.CENTER_LEFT);
            pista.getChildren().add(relleno);
            Label num = Componentes.literal(valor.apply(b), "strong");
            num.setMinWidth(Region.USE_PREF_SIZE);
            GridPane fila = new GridPane();
            ColumnConstraints c1 = new ColumnConstraints();
            c1.setPercentWidth(52);
            ColumnConstraints c2 = new ColumnConstraints();
            c2.setPercentWidth(36);
            ColumnConstraints c3 = new ColumnConstraints();
            c3.setPercentWidth(12);
            c3.setHalignment(javafx.geometry.HPos.RIGHT);
            fila.getColumnConstraints().addAll(c1, c2, c3);
            fila.setHgap(10);
            fila.add(etiqueta, 0, 0);
            fila.add(pista, 1, 0);
            fila.add(num, 2, 0);
            GridPane.setValignment(pista, VPos.CENTER);
            Tooltip.install(fila, new Tooltip(b.etiqueta() + ": " + valor.apply(b)));
            v.getChildren().add(fila);
        }
        return v;
    }

    public static VBox mapaCalorBnps(AnaliticaService.Analisis a) {
        GridPane g = new GridPane();
        g.setHgap(4);
        g.setVgap(4);
        ColumnConstraints primera = new ColumnConstraints(44);
        g.getColumnConstraints().add(primera);
        for (int j = 0; j < 5; j++) {
            ColumnConstraints c = new ColumnConstraints();
            c.setHgrow(Priority.ALWAYS);
            c.setPercentWidth(-1);
            c.setFillWidth(true);
            g.getColumnConstraints().add(c);
            Label h = Componentes.literal(CatalogoService.FRANJAS.get(j), "hint");
            h.setMaxWidth(Double.MAX_VALUE);
            h.setAlignment(Pos.CENTER);
            g.add(h, j + 1, 0);
        }
        for (int i = 0; i < 5; i++) {
            g.add(Componentes.etiqueta(AnaliticaService.DIAS[i], "hint"), 0, i + 1);
            for (int j = 0; j < 5; j++) {
                int valor = a.calor()[i][j];
                int nivel = AnaliticaService.nivel(valor, a.calorMaximo());
                Label celda = Componentes.literal(String.valueOf(valor), "hc");
                if (nivel >= 3) {
                    celda.getStyleClass().add("dk");
                }
                celda.setStyle("-fx-background-color: " + AnaliticaService.RAMPA[nivel] + ";");
                celda.setMaxWidth(Double.MAX_VALUE);
                Tooltip.install(celda, new Tooltip(IdiomaService.t(AnaliticaService.DIAS[i]) + " " + CatalogoService.FRANJAS.get(j)
                        + ": " + valor + " " + IdiomaService.t("atenciones")));
                g.add(celda, j + 1, i + 1);
            }
        }
        HBox leyenda = new HBox(4, Componentes.etiqueta("Menos", "hint"));
        leyenda.setAlignment(Pos.CENTER_LEFT);
        for (String color : AnaliticaService.RAMPA) {
            Region r = new Region();
            r.getStyleClass().add("heat-leg-i");
            r.setStyle("-fx-background-color: " + color + ";");
            leyenda.getChildren().add(r);
        }
        leyenda.getChildren().add(Componentes.etiqueta("Más", "hint"));
        leyenda.setPadding(new javafx.geometry.Insets(10, 0, 0, 0));
        Componentes.accesible(g, "Mapa de calor de atenciones");
        return new VBox(g, leyenda);
    }
}
