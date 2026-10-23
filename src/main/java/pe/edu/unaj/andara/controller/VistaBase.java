package pe.edu.unaj.andara.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import org.springframework.beans.factory.annotation.Autowired;
import pe.edu.unaj.andara.exception.NegocioException;
import pe.edu.unaj.andara.ui.Notificador;
import pe.edu.unaj.andara.ui.PieDePagina;
import pe.edu.unaj.andara.ui.Vista;

import java.util.List;

public abstract class VistaBase implements Vista {

    @FXML
    protected ScrollPane desplazable;
    @FXML
    protected VBox pagina;

    @Autowired
    protected PieDePagina pie;
    @Autowired
    protected Notificador notificador;

    protected abstract List<Node> construir();

    protected boolean conPie() {
        return true;
    }

    @Override
    public void refrescar() {
        pagina.getChildren().setAll(construir());
        if (conPie()) {
            pagina.getChildren().add(pie.crear());
        }
    }

    public void redibujar() {
        double v = desplazable.getVvalue();
        refrescar();
        Platform.runLater(() -> {
            desplazable.layout();
            desplazable.setVvalue(v);
        });
    }

    protected boolean intentar(Runnable accion) {
        try {
            accion.run();
            return true;
        } catch (NegocioException e) {
            notificador.error(e.getMessage());
            return false;
        }
    }

    protected void ir(String ruta) {
        pe.edu.unaj.andara.ui.Navegacion.ir(pagina, ruta);
    }
}
