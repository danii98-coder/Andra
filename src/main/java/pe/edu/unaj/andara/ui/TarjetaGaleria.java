package pe.edu.unaj.andara.ui;

import javafx.animation.Animation;
import javafx.animation.RotateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Rotate;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;

public class TarjetaGaleria extends VBox {

    private final Label cta;
    private final Node objetivoGiro;
    private RotateTransition giro;

    private static final class Arte extends StackPane {
        private final boolean destacado;

        Arte(boolean destacado, Node... hijos) {
            super(hijos);
            this.destacado = destacado;
        }

        @Override
        public Orientation getContentBias() {
            return Orientation.HORIZONTAL;
        }

        @Override
        protected double computePrefHeight(double ancho) {
            double w = ancho < 0 ? 250 : ancho;
            return destacado ? Math.max(260, w * 0.5) : w * 0.625;
        }

        @Override
        protected double computeMinHeight(double ancho) {
            return destacado ? 260 : computePrefHeight(ancho);
        }
    }

    public TarjetaGaleria(Node arte, Node objetivoGiro, String tipo, Label estado, String titulo, Node meta, Node pieIzquierdo,
                          boolean destacado, String bandera, Button estrella, String accesible, Runnable alAbrir, int indice) {
        this.objetivoGiro = objetivoGiro;
        getStyleClass().add("tile");
        if (destacado) {
            getStyleClass().add("featured");
            Galeria.destacar(this);
        }
        Arte marco = new Arte(destacado, arte);
        marco.getStyleClass().add("tile-art");
        if (bandera != null) {
            Label flag = Componentes.etiqueta(bandera, "tile-flag");
            StackPane.setAlignment(flag, Pos.TOP_LEFT);
            StackPane.setMargin(flag, new Insets(12));
            marco.getChildren().add(flag);
        }
        if (estrella != null) {
            StackPane.setAlignment(estrella, Pos.TOP_RIGHT);
            StackPane.setMargin(estrella, new Insets(12));
            marco.getChildren().add(estrella);
        }
        Rectangle recorte = new Rectangle();
        recorte.setArcWidth(36);
        recorte.setArcHeight(36);
        recorte.widthProperty().bind(marco.widthProperty());
        recorte.heightProperty().bind(marco.heightProperty().add(40));
        marco.setClip(recorte);
        if (destacado) {
            VBox.setVgrow(marco, Priority.ALWAYS);
        }

        HBox arriba = new HBox(8, Componentes.mayus(tipo, "label-caps"), Componentes.espacio(), estado);
        arriba.setAlignment(Pos.CENTER_LEFT);
        Label lblTitulo = Componentes.literalParrafo(titulo, "tile-titulo");
        cta = Componentes.etiqueta("Ver detalles", "tile-cta");
        cta.setGraphic(new FontIcon("fth-arrow-right"));
        cta.setOpacity(0);
        HBox pie = new HBox(8, pieIzquierdo, Componentes.espacio(), cta);
        pie.setAlignment(Pos.CENTER_LEFT);
        pie.setPadding(new Insets(8, 0, 0, 0));
        VBox cuerpo = new VBox(arriba, lblTitulo, meta, pie);
        cuerpo.getStyleClass().add("tile-body");
        getChildren().addAll(marco, cuerpo);
        setFocusTraversable(true);
        Componentes.accesible(this, accesible);
        setOnMouseClicked(e -> alAbrir.run());
        setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER || e.getCode() == KeyCode.SPACE) {
                alAbrir.run();
                e.consume();
            }
        });
        setOnMouseEntered(e -> activar());
        setOnMouseExited(e -> desactivar());
        focusedProperty().addListener((o, a, b) -> {
            if (b) {
                getStyleClass().add("foco");
                activar();
            } else {
                getStyleClass().remove("foco");
                desactivar();
            }
        });
        setCursor(javafx.scene.Cursor.HAND);
        Movimiento.aparecer(this, Duration.millis(450), Duration.millis(40L * indice));
    }

    private void activar() {
        Movimiento.opacidad(cta, 1);
        if (Movimiento.reducido() || objetivoGiro == null) {
            return;
        }
        if (giro == null) {
            giro = new RotateTransition(Duration.millis(1150), objetivoGiro);
            giro.setAxis(Rotate.Y_AXIS);
            giro.setFromAngle(0);
            giro.setToAngle(360);
            giro.setInterpolator(Movimiento.GIRO);
            giro.setOnFinished(e -> objetivoGiro.setRotate(0));
        }
        if (giro.getStatus() != Animation.Status.RUNNING) {
            giro.playFromStart();
        }
    }

    private void desactivar() {
        if (!isFocused() && !isHover()) {
            Movimiento.opacidad(cta, 0);
        }
    }
}
