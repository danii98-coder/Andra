package pe.edu.unaj.andara.ui;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.function.Consumer;

public class Segmentado extends HBox {

    private final ToggleGroup grupo = new ToggleGroup();
    private Consumer<String> alCambiar = v -> {
    };
    private boolean silencio;

    public Segmentado(boolean grande) {
        getStyleClass().add("seg");
        if (grande) {
            getStyleClass().add("seg-lg");
        }
        grupo.selectedToggleProperty().addListener((o, a, b) -> {
            if (b == null) {
                grupo.selectToggle(a);
                return;
            }
            if (!silencio) {
                alCambiar.accept((String) b.getUserData());
            }
        });
    }

    public Segmentado opcion(String valor, String texto, String icono, Long conteo) {
        ToggleButton b = new ToggleButton();
        Componentes.fijar(b, texto);
        b.setUserData(valor);
        b.setToggleGroup(grupo);
        b.getStyleClass().add("seg-btn");
        b.setMnemonicParsing(false);
        b.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(b, Priority.ALWAYS);
        Node grafico = null;
        if (icono != null) {
            grafico = new FontIcon(icono);
        }
        if (conteo != null) {
            Label c = Componentes.conteo(conteo);
            b.setContentDisplay(javafx.scene.control.ContentDisplay.RIGHT);
            b.setGraphic(c);
        } else if (grafico != null) {
            b.setGraphic(grafico);
        }
        getChildren().add(b);
        return this;
    }

    public Segmentado opcionNodo(String valor, String texto, Node grafico) {
        opcion(valor, texto, null, null);
        ((ToggleButton) getChildren().get(getChildren().size() - 1)).setGraphic(grafico);
        return this;
    }

    public Segmentado alCambiar(Consumer<String> accion) {
        this.alCambiar = accion;
        return this;
    }

    public Segmentado seleccionar(String valor) {
        silencio = true;
        grupo.getToggles().stream().filter(t -> valor.equals(t.getUserData())).findFirst().ifPresent(grupo::selectToggle);
        silencio = false;
        return this;
    }

    public String valor() {
        return grupo.getSelectedToggle() == null ? null : (String) grupo.getSelectedToggle().getUserData();
    }
}
