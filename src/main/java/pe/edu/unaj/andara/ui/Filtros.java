package pe.edu.unaj.andara.ui;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.kordamp.ikonli.javafx.FontIcon;
import pe.edu.unaj.andara.service.EstadoFiltro;
import pe.edu.unaj.andara.service.FiltroService;
import pe.edu.unaj.andara.util.Fechas;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public final class Filtros {

    public record Avanzado(String id, String tipo, String etiqueta, String placeholder, List<Opcion> opciones, boolean ancho) {
        public static Avanzado fecha(String id, String etiqueta) {
            return new Avanzado(id, "date", etiqueta, null, List.of(), false);
        }

        public static Avanzado texto(String id, String etiqueta, String placeholder) {
            return new Avanzado(id, "text", etiqueta, placeholder, List.of(), true);
        }

        public static Avanzado selector(String id, String etiqueta, String placeholder, List<Opcion> opciones) {
            return new Avanzado(id, "select", etiqueta, placeholder, opciones, false);
        }
    }

    private static final Map<String, String> NOMBRES = Map.of("from", "Desde", "to", "Hasta", "code", "Código",
            "fac", "Escuela", "tipo", "Tipo", "kind", "Concepto");

    private Filtros() {
    }

    public static Button boton(EstadoFiltro estado, List<String[]> presets, List<Avanzado> avanzados, Notificador n,
                               Runnable alCambiar) {
        Button b = Componentes.botonSm("Filtros", "btn-secondary", "fth-filter", null);
        int cantidad = estado.cantidad();
        if (cantidad > 0) {
            b.getStyleClass().add("is-on");
            Label c = Componentes.literal(String.valueOf(cantidad), "flt-count");
            HBox conTexto = new HBox(6, new FontIcon("fth-filter"), Componentes.etiqueta("Filtros"), c);
            conTexto.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            b.textProperty().unbind();
            b.setText(null);
            b.setGraphic(conTexto);
        }
        b.setOnAction(e -> n.popover(panel(estado, presets, avanzados, n, alCambiar), b));
        return b;
    }

    private static VBox panel(EstadoFiltro estado, List<String[]> presets, List<Avanzado> avanzados, Notificador n,
                              Runnable alCambiar) {
        VBox caja = new VBox(4);
        caja.getStyleClass().add("pop");
        if (avanzados != null && !avanzados.isEmpty()) {
            caja.getStyleClass().add("ancho");
        }
        caja.setPadding(new Insets(14));
        caja.getChildren().add(Componentes.etiqueta("Filtros rápidos", "pop-group"));
        FlowPane chips = Componentes.chips();
        for (String[] p : presets) {
            ToggleButton chip = new ToggleButton();
            Componentes.fijar(chip, p[1]);
            chip.getStyleClass().add("chip");
            chip.setSelected(estado.getActivos().contains(p[0]));
            chip.setOnAction(e -> {
                estado.alternar(p[0]);
                alCambiar.run();
            });
            chips.getChildren().add(chip);
        }
        caja.getChildren().add(chips);
        Map<String, Campo<?>> campos = new LinkedHashMap<>();
        Label errorRango = Componentes.parrafo("", "err");
        errorRango.setVisible(false);
        errorRango.setManaged(false);
        if (avanzados != null && !avanzados.isEmpty()) {
            caja.getChildren().add(Componentes.etiqueta("Búsqueda avanzada", "pop-group"));
            GridPane g = new GridPane();
            g.getStyleClass().add("flt-adv");
            g.setHgap(12);
            g.setVgap(10);
            ColumnConstraints c1 = new ColumnConstraints();
            c1.setPercentWidth(50);
            ColumnConstraints c2 = new ColumnConstraints();
            c2.setPercentWidth(50);
            g.getColumnConstraints().addAll(c1, c2);
            int fila = 0;
            int col = 0;
            for (Avanzado a : avanzados) {
                Campo<?> campo = switch (a.tipo()) {
                    case "date" -> {
                        String v = estado.avanzado(a.id());
                        yield Formulario.fecha(a.etiqueta(), v == null ? null : LocalDate.parse(v), null, null);
                    }
                    case "select" -> Formulario.selector(a.etiqueta(), a.opciones(), a.placeholder() == null ? "Todos" : a.placeholder(),
                            estado.avanzado(a.id()));
                    default -> Formulario.texto(a.etiqueta(), a.placeholder(), estado.avanzado(a.id()));
                };
                campos.put(a.id(), campo);
                if (a.ancho()) {
                    if (col != 0) {
                        fila++;
                        col = 0;
                    }
                    g.add(campo.nodo(), 0, fila, 2, 1);
                    fila++;
                } else {
                    g.add(campo.nodo(), col, fila);
                    col++;
                    if (col > 1) {
                        col = 0;
                        fila++;
                    }
                }
            }
            caja.getChildren().addAll(g, errorRango);
        }
        caja.getChildren().add(Componentes.etiqueta("Tu propio filtro", "pop-group"));
        TextField propio = new TextField(estado.getTexto());
        Componentes.prompt(propio, "Escribe una palabra o código…");
        Componentes.accesible(propio, "Escribe un filtro");
        HBox.setHgrow(propio, Priority.ALWAYS);
        Button aplicar = Componentes.botonSm("Aplicar", "btn-primary", null, null);
        aplicar.setDefaultButton(true);
        HBox fila = new HBox(propio, aplicar);
        fila.getStyleClass().add("copy-row");
        caja.getChildren().add(fila);
        int cantidad = estado.cantidad();
        Label resumen = Componentes.etiqueta(cantidad == 0 ? "Sin filtros activos"
                : cantidad + " filtro" + (cantidad > 1 ? "s" : "") + " activo" + (cantidad > 1 ? "s" : ""), "hint");
        Button limpiar = Componentes.botonSm("Limpiar", "btn-tertiary", null, e -> {
            estado.limpiar();
            n.cerrarPopover();
            alCambiar.run();
            n.info("Filtros quitados.");
        });
        limpiar.setDisable(cantidad == 0);
        HBox pie = new HBox(resumen, Componentes.espacio(), limpiar);
        pie.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        pie.setPadding(new Insets(10, 0, 0, 0));
        caja.getChildren().add(pie);
        aplicar.setOnAction(e -> {
            Map<String, String> valores = new LinkedHashMap<>();
            campos.forEach((k, c) -> valores.put(k, c.texto()));
            String err = FiltroService.validarRango(valores.get("from"), valores.get("to"));
            if (err != null) {
                Componentes.fijar(errorRango, err);
                errorRango.setVisible(true);
                errorRango.setManaged(true);
                campos.get("to").control().requestFocus();
                return;
            }
            estado.getAvanzados().clear();
            valores.forEach((k, v) -> {
                if (v != null && !v.isBlank()) {
                    estado.getAvanzados().put(k, v);
                }
            });
            estado.setTexto(propio.getText());
            n.cerrarPopover();
            alCambiar.run();
            n.info(estado.cantidad() > 0 ? "Filtros aplicados." : "Sin filtros activos.");
        });
        return caja;
    }

    public static Node resumen(EstadoFiltro estado, List<String[]> presets, Function<String, String> nombreEscuela,
                               Runnable alCambiar) {
        List<Node> etiquetas = new ArrayList<>();
        for (String id : estado.getActivos()) {
            String nombre = presets.stream().filter(p -> p[0].equals(id)).map(p -> p[1]).findFirst().orElse(id);
            etiquetas.add(Componentes.tag(nombre));
        }
        if (!estado.getTexto().isBlank()) {
            etiquetas.add(Componentes.literal("“" + estado.getTexto() + "”", "tag"));
        }
        estado.getAvanzados().forEach((k, v) -> {
            if (v == null || v.isBlank()) {
                return;
            }
            String valor = "fac".equals(k) && nombreEscuela != null ? nombreEscuela.apply(v)
                    : v.matches("\\d{4}-\\d{2}-\\d{2}") ? Fechas.dmy(LocalDate.parse(v)) : v;
            etiquetas.add(Componentes.tag(NOMBRES.getOrDefault(k, k) + ": " + valor));
        });
        if (etiquetas.isEmpty()) {
            Region vacio = new Region();
            vacio.setManaged(false);
            return vacio;
        }
        FlowPane f = new FlowPane();
        f.getStyleClass().add("flt-active");
        f.getChildren().addAll(etiquetas);
        Button quitar = Componentes.enlaceFlecha("Quitar filtros", e -> {
            estado.limpiar();
            alCambiar.run();
        });
        quitar.setGraphic(null);
        f.getChildren().add(quitar);
        return f;
    }
}
