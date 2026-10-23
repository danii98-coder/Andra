package pe.edu.unaj.andara.ui;

import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputControl;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class Campo<C extends Region> {

    private final VBox caja;
    private final C control;
    private final Label error;
    private final List<Function<C, String>> reglas = new ArrayList<>();

    public Campo(String etiqueta, C control) {
        this.control = control;
        this.error = Componentes.parrafo("", "err");
        error.setVisible(false);
        error.setManaged(false);
        caja = new VBox();
        caja.getStyleClass().add("field");
        if (etiqueta != null) {
            Label l = Componentes.etiqueta(etiqueta, "flabel");
            l.setLabelFor(control);
            caja.getChildren().add(l);
            Componentes.accesible(control, etiqueta);
        }
        caja.getChildren().addAll(control, error);
        control.setMaxWidth(Double.MAX_VALUE);
        if (control instanceof TextInputControl t) {
            t.textProperty().addListener((o, a, b) -> limpiar());
        } else if (control instanceof ComboBox<?> c) {
            c.valueProperty().addListener((o, a, b) -> limpiar());
        } else if (control instanceof DatePicker d) {
            d.valueProperty().addListener((o, a, b) -> limpiar());
        }
    }

    public VBox nodo() {
        return caja;
    }

    public C control() {
        return control;
    }

    public Campo<C> regla(Function<C, String> regla) {
        reglas.add(regla);
        return this;
    }

    public Campo<C> requerido(String mensaje) {
        return regla(c -> vacio() ? mensaje : null);
    }

    public Campo<C> minimo(int caracteres) {
        return regla(c -> !vacio() && texto().length() < caracteres ? "Escribe al menos " + caracteres + " caracteres." : null);
    }

    public Campo<C> patron(String regex, String mensaje) {
        return regla(c -> !vacio() && !texto().matches(regex) ? mensaje : null);
    }

    public Campo<C> ayuda(String texto) {
        caja.getChildren().add(Componentes.parrafo(texto, "hint"));
        return this;
    }

    public String texto() {
        if (control instanceof TextInputControl t) {
            return t.getText() == null ? "" : t.getText().trim();
        }
        if (control instanceof ComboBox<?> c && c.getValue() instanceof Opcion o) {
            return o.valor();
        }
        if (control instanceof DatePicker d) {
            return d.getValue() == null ? "" : d.getValue().toString();
        }
        if (control instanceof Formulario.Archivo a) {
            return a.getElegido() == null ? "" : a.getElegido().getName();
        }
        if (control instanceof javafx.scene.control.CheckBox cb) {
            return cb.isSelected() ? "1" : "";
        }
        return "";
    }

    public boolean vacio() {
        return texto().isBlank();
    }

    public String validar() {
        for (Function<C, String> r : reglas) {
            String m = r.apply(control);
            if (m != null) {
                mostrar(m);
                return m;
            }
        }
        limpiar();
        return null;
    }

    public void mostrar(String mensaje) {
        Componentes.fijar(error, mensaje);
        error.setVisible(true);
        error.setManaged(true);
        if (!control.getStyleClass().contains("invalido")) {
            control.getStyleClass().add("invalido");
        }
    }

    public void limpiar() {
        error.setVisible(false);
        error.setManaged(false);
        control.getStyleClass().remove("invalido");
    }
}
