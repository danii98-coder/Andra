package pe.edu.unaj.andara.ui;

import javafx.animation.AnimationTimer;
import javafx.geometry.Bounds;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.util.function.Supplier;

public class VidrioDasm extends StackPane {

    private static final double MARGEN = 30;
    private static final long INTERVALO_NS = 33_000_000L;
    private static final double LIMITE_MS = 24;

    private final ImageView capa = new ImageView();
    private final Region tinte = new Region();
    private Supplier<Node> fuente = () -> null;
    private boolean sucio = true;
    private boolean desenfoque = true;
    private long ultimo;
    private double costoPromedio;
    private WritableImage lienzo;
    private final Rectangle recorte = new Rectangle();

    public VidrioDasm(String claseTinte) {
        this(claseTinte, true);
    }

    /**
     * @param saturar realza los colores del fondo. Desactivarlo en capas flotantes: sobre ellas
     *                Prism dibuja el ColorAdjust con los canales alterados (tonos rojos/azules).
     */
    public VidrioDasm(String claseTinte, boolean saturar) {
        capa.setManaged(false);
        capa.setMouseTransparent(true);
        capa.setSmooth(true);
        ColorAdjust saturacion = new ColorAdjust();
        saturacion.setSaturation(0.8);
        GaussianBlur desenfoqueGauss = new GaussianBlur(28);
        if (saturar) {
            desenfoqueGauss.setInput(saturacion);
        }
        capa.setEffect(desenfoqueGauss);
        tinte.getStyleClass().add(claseTinte);
        tinte.setMouseTransparent(true);
        getChildren().addAll(capa, tinte);
        recorte.widthProperty().bind(widthProperty());
        recorte.heightProperty().bind(heightProperty());
        setClip(recorte);
        widthProperty().addListener((o, a, b) -> marcar());
        heightProperty().addListener((o, a, b) -> marcar());
        AnimationTimer reloj = new AnimationTimer() {
            @Override
            public void handle(long ahora) {
                if (sucio && ahora - ultimo > INTERVALO_NS) {
                    ultimo = ahora;
                    sucio = false;
                    refrescar();
                }
            }
        };
        sceneProperty().addListener((o, a, escena) -> {
            if (escena == null) {
                reloj.stop();
            } else {
                marcar();
                reloj.start();
            }
        });
        reloj.start();
    }

    public void setRadio(double radio) {
        recorte.setArcWidth(radio * 2);
        recorte.setArcHeight(radio * 2);
    }

    public void setFuente(Supplier<Node> fuente) {
        this.fuente = fuente;
        marcar();
    }

    public void marcar() {
        sucio = true;
    }

    public boolean isDesenfoqueActivo() {
        return desenfoque;
    }

    private void refrescar() {
        Node f = fuente.get();
        if (!desenfoque || f == null || f.getScene() == null || getScene() == null || getWidth() <= 0 || getHeight() <= 0
                || f.getParent() == null) {
            capa.setImage(null);
            return;
        }
        long inicio = System.nanoTime();
        Bounds propio = localToScene(getLayoutBounds());
        Bounds enPadre = f.getParent().sceneToLocal(propio);
        if (enPadre == null) {
            return;
        }
        SnapshotParameters sp = new SnapshotParameters();
        sp.setFill(Color.web("#F5F5F7"));
        sp.setViewport(new Rectangle2D(enPadre.getMinX() - MARGEN, enPadre.getMinY() - MARGEN,
                enPadre.getWidth() + MARGEN * 2, enPadre.getHeight() + MARGEN * 2));
        int w = (int) Math.ceil(enPadre.getWidth() + MARGEN * 2);
        int h = (int) Math.ceil(enPadre.getHeight() + MARGEN * 2);
        if (lienzo == null || lienzo.getWidth() != w || lienzo.getHeight() != h) {
            lienzo = new WritableImage(w, h);
        }
        WritableImage img = f.snapshot(sp, lienzo);
        capa.setImage(img);
        capa.setFitWidth(getWidth() + MARGEN * 2);
        capa.setFitHeight(getHeight() + MARGEN * 2);
        capa.relocate(-MARGEN, -MARGEN);
        double ms = (System.nanoTime() - inicio) / 1_000_000.0;
        costoPromedio = costoPromedio == 0 ? ms : costoPromedio * 0.8 + ms * 0.2;
        if (costoPromedio > LIMITE_MS) {
            desenfoque = false;
            capa.setImage(null);
            tinte.getStyleClass().add("sin-desenfoque");
        }
    }
}
