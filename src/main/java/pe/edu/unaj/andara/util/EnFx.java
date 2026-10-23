package pe.edu.unaj.andara.util;

import javafx.application.Platform;

public final class EnFx {

    private EnFx() {
    }

    public static void ejecutar(Runnable tarea) {
        try {
            if (Platform.isFxApplicationThread()) {
                tarea.run();
            } else {
                Platform.runLater(tarea);
            }
        } catch (IllegalStateException sinToolkit) {
            tarea.run();
        }
    }
}
