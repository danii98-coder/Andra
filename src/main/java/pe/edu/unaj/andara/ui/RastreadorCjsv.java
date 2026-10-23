package pe.edu.unaj.andara.ui;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;
import pe.edu.unaj.andara.service.SeguimientoService.Etapa;
import pe.edu.unaj.andara.service.SeguimientoService.SeguimientoCjsv;
import pe.edu.unaj.andara.util.Fechas;

public final class RastreadorCjsv {

    private RastreadorCjsv() {
    }

    public static VBox mini(SeguimientoCjsv s) {
        GridPane pasos = new GridPane();
        pasos.getStyleClass().add("trk");
        int n = s.etapas().size();
        for (int i = 0; i < n; i++) {
            ColumnConstraints c = new ColumnConstraints();
            c.setPercentWidth(100.0 / n);
            c.setHalignment(javafx.geometry.HPos.CENTER);
            pasos.getColumnConstraints().add(c);
        }
        for (int i = 0; i < n; i++) {
            Etapa e = s.etapas().get(i);
            String estado = i < s.actual() ? "done" : i == s.actual() ? "cur" : "";
            StackPane punto = punto(i < s.actual() ? "fth-check" : e.icono(), i == s.actual() && !s.observado());
            Label l = Componentes.parrafo(e.titulo(), "trk-l");
            l.setAlignment(Pos.CENTER);
            l.setMaxWidth(Double.MAX_VALUE);
            VBox paso = new VBox(6, punto, l);
            paso.setAlignment(Pos.TOP_CENTER);
            paso.getStyleClass().add("trk-s");
            if (!estado.isEmpty()) {
                paso.getStyleClass().add(estado);
            }
            StackPane celda = new StackPane();
            if (i > 0) {
                Region linea = new Region();
                linea.getStyleClass().add("trk-linea");
                if (i <= s.actual()) {
                    linea.getStyleClass().add("done");
                }
                linea.setManaged(false);
                celda.getChildren().add(linea);
                celda.widthProperty().addListener((o, a, b) -> {
                    linea.resizeRelocate(-b.doubleValue() / 2 + 16, 14.5, Math.max(0, b.doubleValue() - 32), 3);
                });
            }
            celda.getChildren().add(paso);
            StackPane.setAlignment(paso, Pos.TOP_CENTER);
            pasos.add(celda, i, 0);
        }
        Label eta = Componentes.parrafo(s.estimado());
        HBox fila = new HBox(new FontIcon(s.observado() ? "fth-alert-triangle" : "fth-clock"), eta);
        fila.getStyleClass().add("trk-eta");
        VBox v = new VBox(pasos, fila);
        if (s.observado()) {
            v.getStyleClass().add("trk-obs");
        }
        Componentes.accesible(v, "Seguimiento: " + s.estimado());
        return v;
    }

    private static StackPane punto(String icono, boolean pulso) {
        StackPane p = new StackPane(new FontIcon(icono));
        p.getStyleClass().add("trk-dot");
        if (pulso && !Movimiento.reducido()) {
            Circle halo = new Circle(16);
            halo.getStyleClass().add("trk-halo");
            halo.setManaged(false);
            halo.setMouseTransparent(true);
            halo.setCenterX(16);
            halo.setCenterY(16);
            StackPane cont = new StackPane(p);
            cont.getChildren().add(0, halo);
            ScaleTransition sc = new ScaleTransition(Duration.millis(1800), halo);
            sc.setFromX(1);
            sc.setFromY(1);
            sc.setToX(1.7);
            sc.setToY(1.7);
            FadeTransition f = new FadeTransition(Duration.millis(1800), halo);
            f.setFromValue(0.9);
            f.setToValue(0);
            ParallelTransition pt = new ParallelTransition(sc, f);
            pt.setCycleCount(Animation.INDEFINITE);
            pt.play();
            cont.setMaxSize(32, 32);
            return cont;
        }
        return p;
    }

    public static VBox lineaTiempo(SeguimientoCjsv s) {
        VBox v = new VBox(0);
        for (int i = 0; i < s.etapas().size(); i++) {
            Etapa e = s.etapas().get(i);
            boolean hecho = i < s.actual();
            boolean actual = i == s.actual();
            StackPane punto = new StackPane(new FontIcon(hecho ? "fth-check" : e.icono()));
            punto.getStyleClass().add("tl-dot");
            String detalle = hecho ? Fechas.dmy(s.tiempos().get(i).toLocalDate()) + " · " + Fechas.hm(s.tiempos().get(i))
                    : actual ? (s.observado() ? "Observado" : "En curso") : "Pendiente";
            VBox textos = new VBox(2, Componentes.parrafo(e.titulo(), "strong"), Componentes.etiqueta(detalle, "hint"));
            Region linea = new Region();
            linea.getStyleClass().add("tl-linea");
            linea.setMinHeight(18);
            VBox col = new VBox(punto);
            col.setAlignment(Pos.TOP_CENTER);
            if (i < s.etapas().size() - 1) {
                col.getChildren().add(linea);
                VBox.setVgrow(linea, Priority.ALWAYS);
            }
            HBox item = new HBox(14, col, textos);
            item.getStyleClass().add("tl-item");
            if (hecho) {
                item.getStyleClass().add("done");
            } else if (actual) {
                item.getStyleClass().add("current");
            }
            textos.setPadding(new Insets(2, 0, 14, 0));
            v.getChildren().add(item);
        }
        return v;
    }
}
