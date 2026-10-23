package pe.edu.unaj.andara.ui;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;
import pe.edu.unaj.andara.service.IdiomaService;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class Componentes {

    private static final Map<String, String> TONOS = Map.ofEntries(
            Map.entry("Aprobado", "success"), Map.entry("Disponible", "success"), Map.entry("Pagado", "success"),
            Map.entry("Listo", "success"), Map.entry("Confirmado", "success"), Map.entry("Acceso concedido", "success"),
            Map.entry("Devuelto", "neutral"), Map.entry("Anulado", "neutral"), Map.entry("Reembolsado", "neutral"),
            Map.entry("Entregado", "neutral"), Map.entry("Venció", "neutral"), Map.entry("Cancelado", "neutral"),
            Map.entry("Pendiente", "warning"), Map.entry("Por pagar", "warning"), Map.entry("En revisión", "warning"),
            Map.entry("Rechazado", "error"), Map.entry("Observado", "error"), Map.entry("Ocupado", "error"),
            Map.entry("Vencido", "error"), Map.entry("En uso", "info"), Map.entry("Tu sede", "info"));

    private static Image logoImagen;

    private Componentes() {
    }

    public static void fijar(Labeled l, String texto) {
        l.textProperty().unbind();
        l.textProperty().bind(IdiomaService.enlace(texto == null ? "" : texto));
    }

    public static void prompt(TextInputControl c, String texto) {
        c.promptTextProperty().unbind();
        c.promptTextProperty().bind(IdiomaService.enlace(texto));
    }

    public static void accesible(Node n, String texto) {
        n.accessibleTextProperty().unbind();
        n.accessibleTextProperty().bind(IdiomaService.enlace(texto));
    }

    public static void ayuda(Control c, String texto) {
        Tooltip t = new Tooltip();
        t.textProperty().bind(IdiomaService.enlace(texto));
        c.setTooltip(t);
    }

    public static Label etiqueta(String texto, String... clases) {
        Label l = new Label();
        fijar(l, texto);
        l.getStyleClass().addAll(clases);
        l.setMnemonicParsing(false);
        return l;
    }

    public static Label mayus(String texto, String... clases) {
        Label l = new Label();
        l.textProperty().bind(IdiomaService.enlaceMayus(texto));
        l.getStyleClass().addAll(clases);
        l.setMnemonicParsing(false);
        return l;
    }

    public static Label parrafo(String texto, String... clases) {
        Label l = etiqueta(texto, clases);
        l.setWrapText(true);
        l.setMinHeight(Region.USE_PREF_SIZE);
        return l;
    }

    public static Label literal(String texto, String... clases) {
        Label l = new Label(texto);
        l.getStyleClass().addAll(clases);
        l.setMnemonicParsing(false);
        return l;
    }

    public static Label literalParrafo(String texto, String... clases) {
        Label l = literal(texto, clases);
        l.setWrapText(true);
        l.setMinHeight(Region.USE_PREF_SIZE);
        return l;
    }

    public static Text texto(String contenido, String... clases) {
        Text t = new Text();
        t.textProperty().bind(IdiomaService.enlace(contenido));
        t.getStyleClass().addAll(clases);
        return t;
    }

    public static TextFlow flujo(Node... partes) {
        TextFlow f = new TextFlow(partes);
        f.setMinHeight(Region.USE_PREF_SIZE);
        return f;
    }

    public static Button boton(String texto, String estilo, String icono, EventHandler<ActionEvent> accion) {
        Button b = new Button();
        if (texto != null) {
            fijar(b, texto);
        }
        if (icono != null) {
            b.setGraphic(new FontIcon(icono));
        }
        b.getStyleClass().addAll("btn", estilo);
        b.setMnemonicParsing(false);
        b.setMinWidth(Region.USE_PREF_SIZE);
        if (accion != null) {
            b.setOnAction(accion);
        }
        return b;
    }

    public static Button botonSm(String texto, String estilo, String icono, EventHandler<ActionEvent> accion) {
        Button b = boton(texto, estilo, icono, accion);
        b.getStyleClass().add("btn-sm");
        return b;
    }

    public static Button bloque(Button b) {
        b.setMaxWidth(Double.MAX_VALUE);
        b.setMinWidth(0);
        return b;
    }

    public static Button botonIcono(String icono, String descripcion, EventHandler<ActionEvent> accion) {
        Button b = boton(null, "btn-secondary", icono, accion);
        b.getStyleClass().addAll("btn-sm", "btn-icon");
        accesible(b, descripcion);
        ayuda(b, descripcion);
        return b;
    }

    public static Button iconoRedondo(String icono, String descripcion, EventHandler<ActionEvent> accion) {
        Button b = new Button(null, new FontIcon(icono));
        b.getStyleClass().add("icon-btn");
        accesible(b, descripcion);
        if (accion != null) {
            b.setOnAction(accion);
        }
        return b;
    }

    public static Button enlaceFlecha(String texto, EventHandler<ActionEvent> accion) {
        Button b = new Button(null, new FontIcon("fth-arrow-right"));
        fijar(b, texto);
        b.getStyleClass().add("link-arrow");
        b.setMnemonicParsing(false);
        b.setOnAction(accion);
        return b;
    }

    public static Button estrella(boolean marcada, String nombre, Runnable accion) {
        Button b = new Button(null, new FontIcon("fth-star"));
        b.getStyleClass().add("star-btn");
        if (marcada) {
            b.getStyleClass().add("on");
        }
        accesible(b, (marcada ? "Quitar de" : "Agregar a") + " favoritos: " + nombre);
        b.setOnAction(e -> {
            e.consume();
            accion.run();
        });
        return b;
    }

    public static FontIcon icono(String literal, String... clases) {
        FontIcon i = new FontIcon(literal);
        i.getStyleClass().addAll(clases);
        return i;
    }

    public static Label pill(String texto, String tono) {
        Circle punto = new Circle(3);
        punto.getStyleClass().add("punto");
        Label l = etiqueta(texto, "pill", tono == null ? "neutral" : tono);
        l.setGraphic(punto);
        l.setMinWidth(Region.USE_PREF_SIZE);
        return l;
    }

    public static Label pill(String texto) {
        return pill(texto, tono(texto));
    }

    public static String tono(String estado) {
        return TONOS.getOrDefault(estado, "neutral");
    }

    public static Label pillPlano(String texto, String tono) {
        Label l = etiqueta(texto, "pill", tono);
        l.setMinWidth(Region.USE_PREF_SIZE);
        return l;
    }

    public static Label tag(String texto) {
        Label l = etiqueta(texto, "tag");
        l.setMinWidth(Region.USE_PREF_SIZE);
        return l;
    }

    public static Label conteo(long n) {
        Label l = literal(String.valueOf(n), "count");
        l.setMinWidth(Region.USE_PREF_SIZE);
        return l;
    }

    public static VBox vacio(String icono, String titulo, String sub, Node... extra) {
        VBox caja = new VBox(icono(icono), etiqueta(titulo, "empty-titulo"), parrafo(sub, "empty-sub"));
        if (extra.length > 0) {
            HBox acciones = new HBox(8, extra);
            acciones.setAlignment(Pos.CENTER);
            acciones.setPadding(new Insets(16, 0, 0, 0));
            caja.getChildren().add(acciones);
        }
        caja.getStyleClass().add("empty");
        caja.setMaxWidth(Double.MAX_VALUE);
        return caja;
    }

    public static HBox metaLinea(String icono, String texto) {
        Label l = parrafo(texto);
        HBox fila = new HBox(icono(icono), l);
        fila.getStyleClass().add("meta-line");
        return fila;
    }

    public static VBox hero(String antetitulo, String titulo, String subtitulo, Node lineaContexto) {
        VBox hero = new VBox();
        hero.getStyleClass().add("page-hero");
        if (antetitulo != null) {
            Label eyebrow = parrafo(antetitulo, "eyebrow");
            eyebrow.setAlignment(Pos.CENTER);
            hero.getChildren().add(eyebrow);
        }
        Label h1 = parrafo(titulo, "hero-titulo");
        h1.setAlignment(Pos.CENTER);
        hero.getChildren().add(h1);
        if (subtitulo != null) {
            Label p = parrafo(subtitulo, "hero-sub");
            p.setAlignment(Pos.CENTER);
            hero.getChildren().add(p);
        }
        if (lineaContexto != null) {
            hero.getChildren().add(lineaContexto);
        }
        return hero;
    }

    public static VBox heroAdmin(String titulo, String subtitulo, Node lineaContexto) {
        VBox h = hero(null, titulo, subtitulo, lineaContexto);
        Label eyebrow = etiqueta("Panel administrativo", "eyebrow");
        eyebrow.setGraphic(icono("fth-shield"));
        h.getChildren().add(0, eyebrow);
        return h;
    }

    public static HBox lineaContexto(String sede, String escuela, String rutaCambiar) {
        HBox linea = new HBox();
        linea.getStyleClass().add("ctx-line");
        if (sede != null) {
            Label s = etiqueta(sede);
            s.setGraphic(new FontIcon("fth-map-pin"));
            linea.getChildren().add(s);
        }
        Label e = etiqueta(escuela);
        e.setGraphic(new FontIcon("fth-book-open"));
        linea.getChildren().add(e);
        Button cambiar = botonSm("Cambiar", "btn-tertiary", null, null);
        cambiar.setOnAction(ev -> Navegacion.ir(cambiar, rutaCambiar));
        linea.getChildren().add(cambiar);
        return linea;
    }

    public static HBox cabeceraSeccion(String titulo, String subtitulo, Node... acciones) {
        VBox textos = new VBox(parrafo(titulo, "h2"));
        if (subtitulo != null) {
            textos.getChildren().add(parrafo(subtitulo, "sub"));
        }
        HBox.setHgrow(textos, Priority.ALWAYS);
        textos.setMaxWidth(Double.MAX_VALUE);
        HBox cabecera = new HBox(textos);
        if (acciones.length > 0) {
            HBox acc = new HBox(8, acciones);
            acc.setAlignment(Pos.BOTTOM_RIGHT);
            acc.setMinWidth(Region.USE_PREF_SIZE);
            cabecera.getChildren().add(acc);
        }
        cabecera.getStyleClass().add("section-h");
        return cabecera;
    }

    public static VBox seccion(Node... hijos) {
        VBox s = new VBox(hijos);
        s.getStyleClass().add("section");
        s.setMaxWidth(1168);
        return s;
    }

    public static VBox lista(List<? extends Node> filas) {
        VBox lista = new VBox();
        lista.getStyleClass().add("list");
        lista.getChildren().setAll(filas);
        marcarUltima(lista);
        return lista;
    }

    public static void marcarUltima(Pane contenedor) {
        contenedor.getChildren().forEach(n -> n.getStyleClass().remove("ultima"));
        if (!contenedor.getChildren().isEmpty()) {
            contenedor.getChildren().get(contenedor.getChildren().size() - 1).getStyleClass().add("ultima");
        }
    }

    public static HBox fila(Node principal, Node... acciones) {
        HBox.setHgrow(principal, Priority.ALWAYS);
        if (principal instanceof Region r) {
            r.setMaxWidth(Double.MAX_VALUE);
        }
        HBox acc = new HBox(acciones);
        acc.getStyleClass().add("row-actions");
        acc.setMinWidth(Region.USE_PREF_SIZE);
        HBox f = new HBox(principal, acc);
        f.getStyleClass().add("row");
        return f;
    }

    public static VBox textosFila(String titulo, String sub) {
        VBox v = new VBox(2, parrafo(titulo, "row-title"));
        if (sub != null) {
            v.getChildren().add(parrafo(sub, "row-sub"));
        }
        return v;
    }

    public static GridPane kv(Object... pares) {
        GridPane g = new GridPane();
        g.getStyleClass().add("kv");
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setPercentWidth(38);
        ColumnConstraints c2 = new ColumnConstraints();
        c2.setPercentWidth(62);
        c2.setHalignment(HPos.LEFT);
        g.getColumnConstraints().addAll(c1, c2);
        int fila = 0;
        for (int i = 0; i + 1 < pares.length; i += 2) {
            if (pares[i] == null) {
                continue;
            }
            Label dt = parrafo(String.valueOf(pares[i]), "dt");
            Node dd = pares[i + 1] instanceof Node n ? n : parrafo(String.valueOf(pares[i + 1]), "dd");
            g.addRow(fila++, dt, dd);
        }
        return g;
    }

    public static Image imagenLogo() {
        if (logoImagen == null) {
            logoImagen = new Image(Objects.requireNonNull(Componentes.class.getResourceAsStream("/img/logo-andara.png")));
        }
        return logoImagen;
    }

    public static ImageView logo(double tamanio) {
        ImageView vista = new ImageView(imagenLogo());
        vista.setFitWidth(tamanio);
        vista.setFitHeight(tamanio);
        vista.setSmooth(true);
        Rectangle clip = new Rectangle(tamanio, tamanio);
        clip.setArcWidth(tamanio * 0.5);
        clip.setArcHeight(tamanio * 0.5);
        vista.setClip(clip);
        return vista;
    }

    public static void desplazarHasta(ScrollPane pagina, Node destino) {
        if (pagina == null || destino == null || destino.getScene() == null) {
            return;
        }
        Node contenido = pagina.getContent();
        double y = destino.localToScene(0, 0).getY() - contenido.localToScene(0, 0).getY();
        double maximo = contenido.getBoundsInLocal().getHeight() - pagina.getViewportBounds().getHeight();
        if (maximo <= 0) {
            return;
        }
        double objetivo = Math.max(0, Math.min(1, (y - 120) / maximo));
        if (Movimiento.reducido()) {
            pagina.setVvalue(objetivo);
            return;
        }
        new Timeline(new KeyFrame(Duration.millis(320), new KeyValue(pagina.vvalueProperty(), objetivo,
                Interpolator.SPLINE(0.25, 0.1, 0.25, 1)))).play();
    }

    public static Region espacio() {
        Region r = new Region();
        HBox.setHgrow(r, Priority.ALWAYS);
        return r;
    }

    public static Region espacioV() {
        Region r = new Region();
        VBox.setVgrow(r, Priority.ALWAYS);
        return r;
    }

    public static HBox vineta(String texto, String... clases) {
        Label punto = literal("•", clases);
        punto.setMinWidth(Region.USE_PREF_SIZE);
        Label l = parrafo(texto, clases);
        HBox.setHgrow(l, Priority.ALWAYS);
        HBox h = new HBox(8, punto, l);
        h.setAlignment(Pos.TOP_LEFT);
        return h;
    }

    public static FlowPane chips(Node... hijos) {
        FlowPane f = new FlowPane(hijos);
        f.getStyleClass().add("chips");
        return f;
    }

    public static VBox kpi(String etiqueta, String valor, String tono, String ayuda) {
        Label v = literal(valor, "kpi-valor");
        if (tono != null && !tono.isBlank()) {
            v.getStyleClass().add(tono);
        }
        VBox k = new VBox(mayus(etiqueta, "label-caps"), v, parrafo(ayuda, "hint"));
        k.getStyleClass().add("kpi");
        return k;
    }
}
