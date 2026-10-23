package pe.edu.unaj.andara.controller;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import lombok.RequiredArgsConstructor;
import org.kordamp.ikonli.javafx.FontIcon;
import org.springframework.stereotype.Component;
import pe.edu.unaj.andara.model.*;
import pe.edu.unaj.andara.service.*;
import pe.edu.unaj.andara.ui.*;
import pe.edu.unaj.andara.util.Fechas;
import pe.edu.unaj.andara.util.Montos;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PagosController extends VistaBase {

    private final PagoService pagos;
    private final AccionesPago accionesPago;

    private final EstadoFiltro filtros = new EstadoFiltro();
    private String pestana = "pend";

    @Override
    protected List<Node> construir() {
        List<Pago> todos = pagos.mios();
        List<Pago> pendientes = todos.stream().filter(Pago::isPendiente).toList();
        List<Pago> pagados = todos.stream().filter(Pago::isPagado).toList();
        List<Node> nodos = new ArrayList<>();
        nodos.add(encabezado(pendientes));
        nodos.add(matricula());
        nodos.add(derechos(todos, pendientes, pagados));
        nodos.add(informacion());
        return nodos;
    }

    private Circle mancha(double radio, Color color) {
        Circle c = new Circle(radio);
        c.setFill(new RadialGradient(0, 0, 0.5, 0.5, 0.5, true, CycleMethod.NO_CYCLE, new Stop(0, color), new Stop(0.65, Color.TRANSPARENT)));
        c.setManaged(false);
        c.setMouseTransparent(true);
        return c;
    }

    private StackPane encabezado(List<Pago> pendientes) {
        Label eyebrow = Componentes.literal("Andara Pay", "pay-eyebrow");
        Label titulo = Componentes.parrafo("Paga tu universidad sin ir al banco.", "pay-titulo");
        titulo.setMaxWidth(560);
        Label sub = Componentes.parrafo("Matrícula, documentos, carné y otros derechos de pago con Yape, Plin o tarjeta. Tu comprobante llega al instante.", "lead");
        sub.setMaxWidth(520);
        FlowPane metodos = new FlowPane(6, 6, Componentes.literal("Yape", "pm-chip", "y"), Componentes.literal("Plin", "pm-chip", "p"),
                Componentes.literal("Visa", "pm-chip"), Componentes.literal("Mastercard", "pm-chip"), Componentes.literal("American Express", "pm-chip"),
                Componentes.literal("Diners Club", "pm-chip"));
        Componentes.accesible(metodos, "Métodos aceptados");
        VBox izquierda = new VBox(12, eyebrow, titulo, sub, metodos);
        VBox.setMargin(metodos, new Insets(6, 0, 0, 0));
        HBox.setHgrow(izquierda, Priority.ALWAYS);
        int n = pendientes.size();
        VBox saldo = new VBox(Componentes.mayus("Por pagar", "label-caps"), Componentes.literal(Montos.soles(PagoService.total(pendientes)), "pay-balance-valor"),
                Componentes.etiqueta(n + " derecho" + (n == 1 ? "" : "s") + " de pago pendiente" + (n == 1 ? "" : "s"), "hint"));
        saldo.getStyleClass().add("pay-balance");
        Optional<Pago> proximo = pagos.masProximo();
        if (proximo.isPresent()) {
            Pago p = proximo.get();
            saldo.getChildren().addAll(Componentes.bloque(Componentes.boton("Pagar lo más próximo · " + Montos.soles(p.getTotal()), "btn-primary", null,
                    e -> accionesPago.pantalla(p.getCodigo(), this::redibujar))), Componentes.parrafo("Vence el " + Fechas.dmy(p.getVence()) + " · " + p.getConcepto(), "hint"));
        } else {
            Label aldia = Componentes.etiqueta("Estás al día", "hint");
            aldia.setGraphic(Componentes.icono("fth-check", "verde"));
            saldo.getChildren().add(aldia);
        }
        HBox pagado = new HBox(Componentes.etiqueta("Pagado en 2026"), Componentes.espacio(), Componentes.literal(Montos.soles(pagos.pagadoEnAnio(2026)), "strong"));
        pagado.getStyleClass().add("pb-row");
        saldo.getChildren().add(pagado);
        saldo.setPrefWidth(400);
        saldo.setMinWidth(320);
        HBox contenido = new HBox(40, izquierda, saldo);
        contenido.getStyleClass().add("pay-hero-in");
        contenido.setAlignment(Pos.CENTER_LEFT);
        Circle azul = mancha(280, Color.rgb(0, 113, 227, 0.14));
        Circle violeta = mancha(210, Color.rgb(116, 34, 132, 0.08));
        Pane manchas = new Pane(azul, violeta);
        manchas.setMouseTransparent(true);
        manchas.widthProperty().addListener((o, a, b) -> {
            azul.setCenterX(b.doubleValue() - 160);
            azul.setCenterY(100);
            violeta.setCenterX(b.doubleValue() - 640);
            violeta.setCenterY(manchas.getHeight() + 50);
        });
        manchas.heightProperty().addListener((o, a, b) -> violeta.setCenterY(b.doubleValue() + 50));
        StackPane centrado = new StackPane(contenido);
        centrado.setAlignment(Pos.CENTER);
        StackPane banda = new StackPane(manchas, centrado);
        banda.getStyleClass().add("pay-hero");
        Rectangle recorte = new Rectangle();
        recorte.widthProperty().bind(banda.widthProperty());
        recorte.heightProperty().bind(banda.heightProperty());
        banda.setClip(recorte);
        banda.setMaxWidth(Double.MAX_VALUE);
        VBox.setMargin(banda, new Insets(0, 0, 16, 0));
        return banda;
    }

    private VBox matricula() {
        Matricula m = pagos.matricula();
        Pago mp = m.getPago();
        boolean ok = m.isConfirmada();
        VBox textos = new VBox(4, Componentes.mayus("Matrícula " + m.getPeriodo(), "label-caps"),
                Componentes.parrafo(ok ? "Tu matrícula está confirmada" : "Completa tu matrícula con el pago", "h2"),
                Componentes.parrafo(ok ? "Pagaste " + Montos.soles(mp.getTotal()) + " con " + mp.getMetodo().getEtiqueta() + " · Comprobante " + mp.getBoleta()
                        : "Ya llenaste tu ficha en el sistema académico de la UNAJ. Tu derecho de pago llegó a Andara Pay: págalo aquí y tu matrícula queda confirmada al instante.", "muted", "small"));
        textos.setMaxWidth(620);
        HBox.setHgrow(textos, Priority.ALWAYS);
        Button accion = ok ? Componentes.boton("Ver comprobante", "btn-secondary", "fth-file", e -> accionesPago.comprobante(mp.getCodigo()))
                : Componentes.boton("Pagar matrícula · " + Montos.soles(mp.getTotal()), "btn-primary", "fth-credit-card", e -> accionesPago.pantalla(mp.getCodigo(), this::redibujar));
        HBox cab = new HBox(20, textos, accion);
        cab.setAlignment(Pos.TOP_LEFT);
        Rejilla pasos = new Rejilla(3, 220, 10);
        pasos.getChildren().addAll(paso("done", null, "Ficha de matrícula", "Completada en el sistema académico UNAJ"),
                paso(ok ? "done" : "cur", "2", "Derecho de pago", ok ? "Pagado en Andara Pay" : "Pendiente · vence el " + Fechas.dmy(mp.getVence())),
                paso(ok ? "done" : "", "3", "Matrícula confirmada", "Constancia disponible en Documentos"));
        VBox tarjeta = new VBox(cab, pasos);
        tarjeta.getStyleClass().add("mat-card");
        if (ok) {
            tarjeta.getStyleClass().add("done");
        }
        return Componentes.seccion(tarjeta);
    }

    private HBox paso(String estado, String numero, String titulo, String detalle) {
        StackPane num = new StackPane("done".equals(estado) ? new FontIcon("fth-check") : Componentes.literal(numero));
        num.getStyleClass().add("mat-num");
        VBox textos = new VBox(2, Componentes.parrafo(titulo, "strong", "small"), Componentes.parrafo(detalle, "hint"));
        HBox h = new HBox(num, textos);
        h.getStyleClass().add("mat-step");
        if (!estado.isEmpty()) {
            h.getStyleClass().add(estado);
        }
        return h;
    }

    private VBox derechos(List<Pago> todos, List<Pago> pendientes, List<Pago> pagados) {
        HBox cab = Componentes.cabeceraSeccion("Derechos de pago", "Todo lo que debes pagar a la universidad, en un solo lugar. No hay pensiones: la UNAJ es pública.",
                Filtros.boton(filtros, PagoService.PRESETS, null, notificador, this::redibujar),
                Componentes.botonSm("Nuevo pago", "btn-primary", "fth-plus", e -> accionesPago.nuevoPago(this::redibujar)));
        Segmentado tabs = new Segmentado(true).opcion("pend", "Por pagar", null, (long) pendientes.size())
                .opcion("paid", "Pagados", null, (long) pagados.size()).opcion("all", "Todos", null, (long) todos.size()).seleccionar(pestana);
        tabs.alCambiar(v -> {
            pestana = v;
            redibujar();
        });
        tabs.setMaxWidth(460);
        Componentes.accesible(tabs, "Estado");
        VBox.setMargin(tabs, new Insets(0, 0, 16, 0));
        List<Pago> lista = "pend".equals(pestana) ? pendientes : "paid".equals(pestana) ? pagados : todos;
        lista = new ArrayList<>(FiltroService.aplicar(lista, filtros, pagos.predicados(), p -> p.getConcepto() + " " + p.getCodigo() + " " + (p.getBoleta() == null ? "" : p.getBoleta())));
        lista.sort(Comparator.comparing((Pago p) -> p.getPagado() != null ? p.getPagado() : p.getCreado()).reversed());
        Node cuerpo;
        if (lista.isEmpty()) {
            cuerpo = Componentes.vacio("fth-credit-card", "pend".equals(pestana) ? "No tienes pagos pendientes" : "No hay pagos con esos filtros",
                    "Cuando solicites un documento, carné o matrícula, su derecho de pago aparecerá aquí.");
        } else {
            VBox filas = new VBox(10);
            lista.forEach(p -> filas.getChildren().add(fila(p)));
            cuerpo = filas;
        }
        return Componentes.seccion(cab, tabs, Filtros.resumen(filtros, PagoService.PRESETS, null, this::redibujar), cuerpo);
    }

    private HBox fila(Pago p) {
        StackPane icono = new StackPane(new FontIcon(p.getTipo().getIcono()));
        icono.getStyleClass().add("pr-ico");
        String detalle = p.getCodigo() + " · " + p.getTipo().getEtiqueta() + (p.isPagado() && p.getPagado() != null
                ? " · Pagado el " + Fechas.dmy(p.getPagado().toLocalDate()) + " con " + p.getMetodo().getEtiqueta()
                : p.getVence() != null ? " · Vence el " + Fechas.dmy(p.getVence()) : "");
        VBox textos = new VBox(2, Componentes.parrafo(p.getConcepto(), "strong"), Componentes.etiqueta(detalle, "hint"));
        HBox.setHgrow(textos, Priority.ALWAYS);
        String estado = PagoService.estadoVisible(p);
        HBox h = new HBox(icono, textos, Componentes.literal(Montos.soles(p.getTotal()), "pr-amt"), Componentes.pill(estado, PagoService.tonoEstado(estado)),
                Componentes.icono("fth-arrow-right", "muted"));
        h.getStyleClass().add("pay-row");
        h.setFocusTraversable(true);
        Componentes.accesible(h, p.getConcepto() + " · " + Montos.soles(p.getTotal()) + " · " + estado);
        Runnable abrir = () -> {
            if (p.isPendiente()) {
                accionesPago.detalle(p.getCodigo(), this::redibujar);
            } else {
                accionesPago.comprobante(p.getCodigo());
            }
        };
        h.setOnMouseClicked(e -> abrir.run());
        h.setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ENTER) {
                abrir.run();
            }
        });
        return h;
    }

    private VBox informacion() {
        Rejilla g = new Rejilla(3, 240);
        String[][] datos = {{"fth-award", "Matrícula", "Llena tu ficha en el sistema académico. El derecho de pago aparece aquí automáticamente."},
                {"fth-file-text", "Documentos", "Constancias, certificados y récord se generan con tus datos y se recogen, por lo general, el mismo día."},
                {"fth-credit-card", "Carné universitario", "Solicítalo con tu foto, págalo y recógelo en la Mesa de Partes de tu sede."}};
        for (String[] d : datos) {
            StackPane ic = new StackPane(new FontIcon(d[0]));
            ic.getStyleClass().add("sicon-sm");
            VBox o = new VBox(12, ic, Componentes.parrafo(d[1], "h3"), Componentes.parrafo(d[2], "muted", "small"));
            o.getStyleClass().add("office");
            g.getChildren().add(o);
        }
        Label nota = Componentes.parrafo("Prototipo: los pagos son simulados y no generan cargos reales.", "hint");
        nota.setMaxWidth(Double.MAX_VALUE);
        nota.setAlignment(Pos.CENTER);
        VBox.setMargin(nota, new Insets(16, 0, 0, 0));
        return Componentes.seccion(g, nota);
    }
}
