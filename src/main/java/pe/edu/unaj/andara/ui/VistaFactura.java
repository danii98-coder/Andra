package pe.edu.unaj.andara.ui;

import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import pe.edu.unaj.andara.model.*;
import pe.edu.unaj.andara.service.CatalogoService;
import pe.edu.unaj.andara.util.Fechas;
import pe.edu.unaj.andara.util.Montos;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class VistaFactura {

    private VistaFactura() {
    }

    public static StackPane crear(String codigo, List<ItemPago> items, DatosSolicitante sol, LocalDate vence, Sede sede,
                                  Pago pagado, CatalogoService catalogo) {
        BigDecimal total = items.stream().map(ItemPago::getImporte).reduce(BigDecimal.ZERO, BigDecimal::add);
        VBox marca = new VBox(Componentes.literal("Andara Pay", "strong", "h3"), Componentes.literal("UNAJ · tus trámites más simples", "hint"));
        HBox marcaFila = new HBox(10, Componentes.logo(36), marca);
        marcaFila.setAlignment(Pos.CENTER_LEFT);
        VBox id = new VBox(2, Componentes.literal(pagado != null ? "COMPROBANTE DE PAGO" : "ORDEN DE PAGO", "label-caps"),
                Componentes.literal(pagado != null ? pagado.getBoleta() : codigo, "h3"));
        id.setAlignment(Pos.TOP_RIGHT);
        if (pagado != null) {
            id.getChildren().add(Componentes.literal("Op. " + pagado.getOperacion(), "hint"));
        }
        HBox cabecera = new HBox(marcaFila, Componentes.espacio(), id);
        cabecera.getStyleClass().add("inv-head");

        String tipo = sol.getTipo() + (catalogo.escuela(sol.getEscuela()).isPresent() ? " · EP " + catalogo.nombreEscuela(sol.getEscuela()) : "");
        VBox izquierda = new VBox(2, Componentes.literal("SOLICITANTE", "label-caps"), Componentes.literal(sol.getNombre(), "strong"),
                Componentes.literal("DNI " + sol.getDni() + (sol.getCodigo() != null && !sol.getCodigo().isBlank() ? " · Código " + sol.getCodigo() : ""), "small"),
                Componentes.literal(tipo, "small"));
        VBox derecha = new VBox(2, Componentes.literal(pagado != null ? "FECHA DE PAGO" : "EMITIDA", "label-caps"),
                Componentes.literal(pagado != null ? Fechas.dmyHm(pagado.getPagado()) : Fechas.dmy(Fechas.hoy()), "strong"),
                Componentes.literal(pagado != null ? "MÉTODO" : "VENCE", "label-caps"),
                Componentes.literal(pagado != null ? pagado.getMetodo().getEtiqueta() : Fechas.dmy(vence), "strong"));
        VBox.setMargin(derecha.getChildren().get(2), new Insets(8, 0, 0, 0));
        GridPane meta = new GridPane();
        meta.getStyleClass().add("inv-meta");
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setPercentWidth(58);
        ColumnConstraints c2 = new ColumnConstraints();
        c2.setPercentWidth(42);
        meta.getColumnConstraints().addAll(c1, c2);
        meta.add(izquierda, 0, 0);
        meta.add(derecha, 1, 0);

        GridPane tabla = new GridPane();
        double[] anchos = {52, 12, 18, 18};
        for (double a : anchos) {
            ColumnConstraints c = new ColumnConstraints();
            c.setPercentWidth(a);
            tabla.getColumnConstraints().add(c);
        }
        tabla.getColumnConstraints().get(1).setHalignment(HPos.RIGHT);
        tabla.getColumnConstraints().get(2).setHalignment(HPos.RIGHT);
        tabla.getColumnConstraints().get(3).setHalignment(HPos.RIGHT);
        String[] cab = {"CONCEPTO", "CANT.", "P. UNIT.", "IMPORTE"};
        for (int i = 0; i < cab.length; i++) {
            Label l = Componentes.literal(cab[i], "inv-th");
            tabla.add(celda(l, i > 0), i, 0);
        }
        int fila = 1;
        for (ItemPago it : items) {
            tabla.add(celda(Componentes.literalParrafo(it.getConcepto()), false), 0, fila);
            tabla.add(celda(Componentes.literal(String.valueOf(it.getCantidad())), true), 1, fila);
            tabla.add(celda(Componentes.literal(Montos.soles(it.getPrecioUnitario())), true), 2, fila);
            tabla.add(celda(Componentes.literal(Montos.soles(it.getImporte())), true), 3, fila);
            fila++;
        }
        HBox totalFila = new HBox(Componentes.literal("Total a pagar", "strong"), Componentes.espacio(),
                Componentes.literal(Montos.soles(total), "strong"));
        totalFila.getStyleClass().add("inv-total");
        totalFila.setPadding(new Insets(12, 6, 0, 6));
        Label nota = Componentes.literalParrafo((pagado != null ? "Pago confirmado. " : "") + "Sede de atención: "
                + sede.getNombreCompleto() + ". Montos referenciales de demostración; no se realizan cargos reales.", "inv-note");
        VBox hoja = new VBox(cabecera, meta, tabla, totalFila, nota);
        StackPane contenedor = new StackPane(hoja);
        contenedor.getStyleClass().add("sheet");
        if (pagado != null) {
            Label marcaAgua = Componentes.literal("DEMOSTRACIÓN", "watermark");
            marcaAgua.setMouseTransparent(true);
            StackPane.setAlignment(marcaAgua, Pos.BOTTOM_RIGHT);
            StackPane.setMargin(marcaAgua, new Insets(0, -10, 30, 0));
            contenedor.getChildren().add(marcaAgua);
        }
        return contenedor;
    }

    private static Region celda(Label l, boolean derecha) {
        HBox c = new HBox(l);
        c.getStyleClass().add("inv-fila");
        c.setAlignment(derecha ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        c.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow(c, Priority.ALWAYS);
        return c;
    }
}
