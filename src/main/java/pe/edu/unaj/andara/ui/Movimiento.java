package pe.edu.unaj.andara.ui;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Node;
import javafx.util.Duration;

public final class Movimiento {

    public static final Interpolator SUAVE = Interpolator.SPLINE(0.25, 0.1, 0.25, 1);
    public static final Interpolator GIRO = Interpolator.SPLINE(0.45, 0.05, 0.25, 1);
    public static final Duration RAPIDO = Duration.millis(180);

    private static final BooleanProperty REDUCIDO = new SimpleBooleanProperty(false);

    private Movimiento() {
    }

    public static BooleanProperty reducidoProperty() {
        return REDUCIDO;
    }

    public static boolean reducido() {
        return REDUCIDO.get();
    }

    public static void aparecer(Node n, Duration duracion, Duration retraso) {
        if (reducido()) {
            n.setOpacity(1);
            return;
        }
        n.setOpacity(0);
        FadeTransition f = new FadeTransition(duracion, n);
        f.setFromValue(0);
        f.setToValue(1);
        f.setDelay(retraso);
        f.setInterpolator(SUAVE);
        f.play();
    }

    public static void subir(Node n) {
        if (reducido()) {
            n.setOpacity(1);
            return;
        }
        n.setOpacity(0);
        n.setTranslateY(12);
        FadeTransition f = new FadeTransition(Duration.millis(220), n);
        f.setToValue(1);
        TranslateTransition t = new TranslateTransition(Duration.millis(220), n);
        t.setToY(0);
        ParallelTransition p = new ParallelTransition(f, t);
        p.setInterpolator(SUAVE);
        p.play();
    }

    public static void desvanecer(Node n, Runnable alTerminar) {
        if (reducido()) {
            alTerminar.run();
            return;
        }
        FadeTransition f = new FadeTransition(Duration.millis(200), n);
        f.setToValue(0);
        f.setInterpolator(SUAVE);
        f.setOnFinished(e -> alTerminar.run());
        f.play();
    }

    public static void opacidad(Node n, double valor) {
        if (reducido()) {
            n.setOpacity(valor);
            return;
        }
        FadeTransition f = new FadeTransition(RAPIDO, n);
        f.setToValue(valor);
        f.setInterpolator(SUAVE);
        f.play();
    }
}
