package pe.edu.unaj.andara.ui;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.WeakChangeListener;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.util.StringConverter;
import org.kordamp.ikonli.javafx.FontIcon;
import pe.edu.unaj.andara.service.IdiomaService;
import pe.edu.unaj.andara.util.Fechas;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class Formulario {

    private static final DateTimeFormatter DMY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final List<Campo<?>> campos = new ArrayList<>();

    public <C extends javafx.scene.layout.Region> Campo<C> agregar(Campo<C> c) {
        campos.add(c);
        return c;
    }

    public boolean validar() {
        Campo<?> primero = null;
        for (Campo<?> c : campos) {
            if (c.nodo().getScene() != null && c.validar() != null && primero == null) {
                primero = c;
            }
        }
        if (primero != null) {
            primero.control().requestFocus();
            return false;
        }
        return true;
    }

    public static Campo<TextField> texto(String etiqueta, String placeholder, String valor) {
        TextField t = new TextField(valor == null ? "" : valor);
        if (placeholder != null) {
            Componentes.prompt(t, placeholder);
        }
        return new Campo<>(etiqueta, t);
    }

    public static Campo<TextField> numero(String etiqueta, String placeholder, String valor, int maximoDigitos) {
        Campo<TextField> c = texto(etiqueta, placeholder, valor);
        soloDigitos(c.control(), maximoDigitos);
        return c;
    }

    public static Campo<PasswordField> clave(String etiqueta) {
        return new Campo<>(etiqueta, new PasswordField());
    }

    public static Campo<TextArea> area(String etiqueta, String placeholder) {
        TextArea a = new TextArea();
        a.setWrapText(true);
        a.setPrefRowCount(3);
        if (placeholder != null) {
            Componentes.prompt(a, placeholder);
        }
        return new Campo<>(etiqueta, a);
    }

    public static Campo<DatePicker> fecha(String etiqueta, LocalDate valor, LocalDate minimo, LocalDate maximo) {
        DatePicker d = new DatePicker(valor);
        configurarFecha(d, minimo, maximo);
        return new Campo<>(etiqueta, d);
    }

    public static void configurarFecha(DatePicker d, LocalDate minimo, LocalDate maximo) {
        d.setEditable(false);
        d.setShowWeekNumbers(false);
        d.setConverter(new StringConverter<>() {
            @Override
            public String toString(LocalDate f) {
                return f == null ? "" : f.format(DMY);
            }

            @Override
            public LocalDate fromString(String s) {
                return s == null || s.isBlank() ? null : LocalDate.parse(s, DMY);
            }
        });
        d.setDayCellFactory(p -> new DateCell() {
            @Override
            public void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                if (item != null && ((minimo != null && item.isBefore(minimo)) || (maximo != null && item.isAfter(maximo)))) {
                    setDisable(true);
                }
            }
        });
    }

    public static Campo<ComboBox<Opcion>> selector(String etiqueta, List<Opcion> opciones, String placeholder, String valor) {
        ComboBox<Opcion> c = new ComboBox<>();
        configurarSelector(c, opciones, placeholder, valor);
        return new Campo<>(etiqueta, c);
    }

    public static void configurarSelector(ComboBox<Opcion> c, List<Opcion> opciones, String placeholder, String valor) {
        List<Opcion> items = new ArrayList<>();
        if (placeholder != null) {
            items.add(Opcion.de("", placeholder));
        }
        items.addAll(opciones);
        c.setItems(FXCollections.observableArrayList(items));
        c.setMaxWidth(Double.MAX_VALUE);
        c.setVisibleRowCount(12);
        c.setConverter(new StringConverter<>() {
            @Override
            public String toString(Opcion o) {
                return o == null ? "" : IdiomaService.t(o.etiqueta());
            }

            @Override
            public Opcion fromString(String s) {
                return null;
            }
        });
        c.setCellFactory(l -> new ListCell<>() {
            @Override
            protected void updateItem(Opcion o, boolean vacio) {
                super.updateItem(o, vacio);
                getStyleClass().remove("grupo");
                setDisable(false);
                if (vacio || o == null) {
                    setText(null);
                    return;
                }
                setText(IdiomaService.t(o.etiqueta()));
                if (o.grupo()) {
                    getStyleClass().add("grupo");
                    setDisable(true);
                }
            }
        });
        c.valueProperty().addListener((o, a, b) -> {
            if (b != null && b.grupo()) {
                c.setValue(a);
            }
        });
        seleccionar(c, valor == null ? "" : valor);
        if (IdiomaService.actual() != null) {
            ChangeListener<String> refresco = (o, a, b) -> {
                Opcion actual = c.getValue();
                List<Opcion> copia = new ArrayList<>(c.getItems());
                c.getItems().setAll(copia);
                c.setValue(actual);
            };
            c.getProperties().put("refrescoIdioma", refresco);
            IdiomaService.actual().idiomaProperty().addListener(new WeakChangeListener<>(refresco));
        }
    }

    public static void seleccionar(ComboBox<Opcion> c, String valor) {
        c.getItems().stream().filter(o -> !o.grupo() && o.valor().equals(valor)).findFirst()
                .ifPresentOrElse(c::setValue, () -> {
                    if (!c.getItems().isEmpty() && !c.getItems().get(0).grupo()) {
                        c.setValue(c.getItems().get(0));
                    }
                });
    }

    public static String valor(ComboBox<Opcion> c) {
        return c.getValue() == null ? "" : c.getValue().valor();
    }

    public static Campo<CheckBox> casilla(String texto) {
        CheckBox cb = new CheckBox();
        Componentes.fijar(cb, texto);
        cb.setWrapText(true);
        cb.getStyleClass().add("casilla");
        Campo<CheckBox> c = new Campo<>(null, cb);
        cb.selectedProperty().addListener((o, a, b) -> c.limpiar());
        return c;
    }

    public static class Archivo extends HBox {
        private final Label nombre = Componentes.etiqueta("Ningún archivo seleccionado", "archivo-nombre");
        private File elegido;
        private Consumer<File> alElegir;

        public Archivo(String extensiones, Notificador notificador, Consumer<File> alElegirInicial) {
            this.alElegir = alElegirInicial;
            Button elegir = Componentes.botonSm("Elegir archivo", "btn-secondary", "fth-upload", null);
            elegir.setOnAction(e -> {
                FileChooser fc = new FileChooser();
                fc.getExtensionFilters().add(new FileChooser.ExtensionFilter(extensiones, extensiones.split(" ")));
                File f = fc.showOpenDialog(notificador.ventana());
                if (f != null) {
                    if (f.length() > 5L * 1024 * 1024) {
                        notificador.error("El archivo supera los 5 MB.");
                        return;
                    }
                    elegido = f;
                    nombre.textProperty().unbind();
                    nombre.setText(f.getName());
                    if (alElegir != null) {
                        alElegir.accept(f);
                    }
                }
            });
            getChildren().addAll(elegir, nombre);
            getStyleClass().add("archivo");
            setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(nombre, Priority.ALWAYS);
        }

        public File getElegido() {
            return elegido;
        }
    }

    public static Campo<Archivo> archivo(String etiqueta, String extensiones, String ayuda, Notificador n, Consumer<File> alElegir) {
        Archivo a = new Archivo(extensiones, n, null);
        Campo<Archivo> c = new Campo<>(etiqueta, a);
        a.alElegir = f -> {
            c.limpiar();
            if (alElegir != null) {
                alElegir.accept(f);
            }
        };
        c.ayuda(ayuda == null ? "PDF, JPG o PNG. Máximo 5 MB." : ayuda);
        return c;
    }

    public static GridPane rejilla(int columnas, javafx.scene.Node... campos) {
        GridPane g = new GridPane();
        g.setHgap(24);
        g.setVgap(16);
        for (int i = 0; i < columnas; i++) {
            ColumnConstraints col = new ColumnConstraints();
            col.setPercentWidth(100.0 / columnas);
            col.setHgrow(Priority.ALWAYS);
            g.getColumnConstraints().add(col);
        }
        for (int i = 0; i < campos.length; i++) {
            g.add(campos[i], i % columnas, i / columnas);
        }
        return g;
    }

    public static void soloDigitos(TextField t, int maximo) {
        t.setTextFormatter(new TextFormatter<String>(cambio -> {
            String nuevo = cambio.getControlNewText();
            return nuevo.matches("\\d{0," + maximo + "}") ? cambio : null;
        }));
    }

    public static String textoFecha(LocalDate f) {
        return f == null ? "" : Fechas.dmy(f);
    }

    public static VBox columna(javafx.scene.Node... nodos) {
        VBox v = new VBox(14, nodos);
        return v;
    }

    public static FontIcon iconoCampo(String literal) {
        return new FontIcon(literal);
    }
}
