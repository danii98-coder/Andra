package pe.edu.unaj.andara.ui;

import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Carrusel extends StackPane {

    public record Datos(String etiqueta, String titulo, String texto, String imagen, String descripcion,
                        double focoX, double focoY) {
    }

    private static final Duration INTERVALO = Duration.seconds(6);
    private static final double MARGEN = 48;

    private int actual;
    private boolean reproduciendo;
    private final List<Diapositiva> diapositivas = new ArrayList<>();
    private final List<StackPane> puntos = new ArrayList<>();
    private final Button pausa = new Button();
    private final Timeline avance = new Timeline(new KeyFrame(INTERVALO, e -> ir(actual + 1)));

    public Carrusel(List<Datos> datos) {
        getStyleClass().add("carousel");
        reproduciendo = !Movimiento.reducido();
        for (int i = 0; i < datos.size(); i++) {
            Diapositiva d = new Diapositiva(datos.get(i), i == 0);
            d.setOpacity(i == 0 ? 1 : 0);
            d.setVisible(i == 0);
            diapositivas.add(d);
            getChildren().add(d);
        }
        Rectangle recorte = new Rectangle();
        recorte.setArcWidth(32);
        recorte.setArcHeight(32);
        recorte.widthProperty().bind(widthProperty());
        recorte.heightProperty().bind(heightProperty());
        setClip(recorte);

        HBox controles = new HBox(4);
        controles.getStyleClass().add("car-ctrl");
        controles.setAlignment(Pos.CENTER_LEFT);
        for (int i = 0; i < datos.size(); i++) {
            final int indice = i;
            Region barra = new Region();
            barra.getStyleClass().add("barra");
            StackPane punto = new StackPane(barra);
            punto.getStyleClass().add("cdot-btn");
            punto.setFocusTraversable(true);
            Componentes.accesible(punto, "Ir a diapositiva " + (i + 1) + ": " + datos.get(i).etiqueta());
            punto.setOnMouseClicked(e -> {
                ir(indice);
                reiniciar();
            });
            puntos.add(punto);
            controles.getChildren().add(punto);
        }
        pausa.getStyleClass().add("pause-btn");
        pausa.setOnAction(e -> alternar());
        controles.getChildren().add(pausa);
        controles.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        StackPane.setAlignment(controles, Pos.BOTTOM_LEFT);
        StackPane.setMargin(controles, new Insets(0, 0, 24, MARGEN - 8));
        getChildren().add(controles);
        avance.setCycleCount(Timeline.INDEFINITE);
        actualizarControles();
        if (reproduciendo) {
            avance.play();
        }
        Movimiento.reducidoProperty().addListener((o, a, b) -> {
            if (b) {
                reproduciendo = false;
                avance.stop();
                actualizarControles();
            }
        });
    }

    public void detener() {
        avance.stop();
    }

    public void reanudar() {
        if (reproduciendo && !Movimiento.reducido()) {
            avance.playFromStart();
        }
    }

    private void alternar() {
        reproduciendo = !reproduciendo;
        if (reproduciendo) {
            avance.playFromStart();
        } else {
            avance.stop();
        }
        actualizarControles();
    }

    private void reiniciar() {
        if (reproduciendo) {
            avance.playFromStart();
        }
    }

    private void ir(int indice) {
        int nuevo = Math.floorMod(indice, diapositivas.size());
        if (nuevo == actual) {
            return;
        }
        Diapositiva saliente = diapositivas.get(actual);
        Diapositiva entrante = diapositivas.get(nuevo);
        entrante.setVisible(true);
        if (Movimiento.reducido()) {
            saliente.setOpacity(0);
            saliente.setVisible(false);
            entrante.setOpacity(1);
        } else {
            FadeTransition fuera = new FadeTransition(Duration.millis(500), saliente);
            fuera.setToValue(0);
            fuera.setOnFinished(e -> saliente.setVisible(false));
            FadeTransition dentro = new FadeTransition(Duration.millis(500), entrante);
            dentro.setToValue(1);
            fuera.play();
            dentro.play();
        }
        actual = nuevo;
        actualizarControles();
    }

    private void actualizarControles() {
        for (int i = 0; i < puntos.size(); i++) {
            puntos.get(i).getStyleClass().remove("on");
            if (i == actual) {
                puntos.get(i).getStyleClass().add("on");
            }
        }
        pausa.setGraphic(new FontIcon(reproduciendo ? "fth-pause" : "fth-play"));
        Componentes.accesible(pausa, (reproduciendo ? "Pausar" : "Reproducir") + " carrusel");
    }

    private static final class Diapositiva extends Pane {

        private final VBox texto;
        private final ImageView foto;
        private final Image imagen;
        private final Rectangle adorno = new Rectangle(88, 88);
        private final Circle punto = new Circle(9);
        private final Datos datos;

        Diapositiva(Datos datos, boolean principal) {
            this.datos = datos;
            imagen = new Image(Objects.requireNonNull(Carrusel.class.getResourceAsStream(datos.imagen())));
            foto = new ImageView(imagen);
            foto.setSmooth(true);
            foto.setPreserveRatio(false);
            Componentes.accesible(foto, datos.descripcion());
            adorno.setArcWidth(48);
            adorno.setArcHeight(48);
            adorno.getStyleClass().add("slide-deco");
            punto.getStyleClass().add("slide-punto");
            Label etiqueta = Componentes.mayus(datos.etiqueta(), "slide-label");
            Label titulo = Componentes.parrafo(datos.titulo(), "slide-titulo");
            if (principal) {
                titulo.getStyleClass().add("principal");
            }
            Label parrafo = Componentes.parrafo(datos.texto(), "slide-texto");
            parrafo.setMaxWidth(520);
            VBox.setMargin(titulo, new Insets(8, 0, 0, 0));
            VBox.setMargin(parrafo, new Insets(10, 0, 0, 0));
            texto = new VBox(etiqueta, titulo, parrafo);
            getChildren().addAll(adorno, foto, punto, texto);
        }

        @Override
        protected void layoutChildren() {
            double w = getWidth();
            double h = getHeight();
            double fw = Math.min(w * 0.46, 500);
            double fx = w - 24 - fw;
            double fy = 24;
            double fh = h - 48;
            foto.setFitWidth(fw);
            foto.setFitHeight(fh);
            foto.setViewport(recorteCover(fw, fh));
            foto.relocate(fx, fy);
            Rectangle esquinas = new Rectangle(fw, fh);
            esquinas.setArcWidth(24);
            esquinas.setArcHeight(24);
            foto.setClip(esquinas);
            adorno.relocate(fx - 16, fy + fh + 12 - 88);
            punto.relocate(fx + fw + 6 - 18, fy - 6);
            double tw = Math.max(200, w * 0.54 - MARGEN);
            double th = texto.prefHeight(tw);
            texto.resizeRelocate(MARGEN, Math.max(MARGEN, (h - th) / 2 - 20), tw, th);
        }

        private Rectangle2D recorteCover(double fw, double fh) {
            double iw = imagen.getWidth();
            double ih = imagen.getHeight();
            double escala = Math.max(fw / iw, fh / ih);
            double vw = fw / escala;
            double vh = fh / escala;
            return new Rectangle2D((iw - vw) * datos.focoX(), (ih - vh) * datos.focoY(), vw, vh);
        }
    }
}
