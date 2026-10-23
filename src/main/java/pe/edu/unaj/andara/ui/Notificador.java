package pe.edu.unaj.andara.ui;

import javafx.animation.PauseTransition;
import javafx.beans.binding.Bindings;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.stage.Window;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class Notificador {

    public static final double ANCHO_MODAL = 560;
    public static final double ANCHO_MODAL_AMPLIO = 720;
    private static final Duration DURACION_TOAST = Duration.millis(3800);

    private StackPane raiz;
    private Node fondo;
    private Pane capaFlotante;
    private StackPane capaModal;
    private VBox zonaToasts;
    private Node focoPrevio;
    private VBox modalActual;
    private Runnable alCerrarModal;
    private Region popoverActual;
    private Region atrapaClics;

    public void registrar(StackPane raiz, Node fondo, Pane capaFlotante, StackPane capaModal, VBox zonaToasts) {
        this.raiz = raiz;
        this.fondo = fondo;
        this.capaFlotante = capaFlotante;
        this.capaModal = capaModal;
        this.zonaToasts = zonaToasts;
        capaModal.setVisible(false);
        capaModal.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.TAB && modalActual != null) {
                ciclarFoco(e.isShiftDown());
                e.consume();
            }
        });
    }

    public Window ventana() {
        return raiz == null || raiz.getScene() == null ? null : raiz.getScene().getWindow();
    }

    public void exito(String mensaje) {
        toast(mensaje, "fth-check", "success");
    }

    public void error(String mensaje) {
        toast(mensaje, "fth-alert-triangle", "error");
    }

    public void info(String mensaje) {
        toast(mensaje, "fth-info", "info");
    }

    private void toast(String mensaje, String icono, String tono) {
        if (zonaToasts == null) {
            return;
        }
        Label texto = Componentes.parrafo(mensaje);
        HBox.setHgrow(texto, Priority.ALWAYS);
        HBox toast = new HBox(new FontIcon(icono), texto);
        toast.getStyleClass().addAll("toast", tono);
        toast.setMaxHeight(Region.USE_PREF_SIZE);
        zonaToasts.getChildren().add(toast);
        Movimiento.subir(toast);
        PauseTransition espera = new PauseTransition(DURACION_TOAST);
        espera.setOnFinished(e -> Movimiento.desvanecer(toast, () -> zonaToasts.getChildren().remove(toast)));
        espera.play();
    }

    public VBox modal(String titulo, Node cuerpo, boolean amplio, Node... pie) {
        return modal(titulo, cuerpo, amplio, null, pie);
    }

    public VBox modal(String titulo, Node cuerpo, boolean amplio, Runnable alCerrar, Node... pie) {
        cerrarPopover();
        if (modalActual == null) {
            focoPrevio = raiz.getScene() == null ? null : raiz.getScene().getFocusOwner();
        } else if (this.alCerrarModal != null) {
            Runnable previo = this.alCerrarModal;
            this.alCerrarModal = null;
            previo.run();
        }
        this.alCerrarModal = alCerrar;
        Label lblTitulo = Componentes.parrafo(titulo, "modal-titulo");
        HBox.setHgrow(lblTitulo, Priority.ALWAYS);
        lblTitulo.setMaxWidth(Double.MAX_VALUE);
        Button cerrar = Componentes.iconoRedondo("fth-x", "Cerrar", e -> cerrarModal());
        HBox cabecera = new HBox(lblTitulo, cerrar);
        cabecera.getStyleClass().add("modal-h");

        VBox cuerpoCaja = new VBox(cuerpo);
        cuerpoCaja.getStyleClass().add("modal-b");
        ScrollPane desplazable = new ScrollPane(cuerpoCaja);
        desplazable.setFitToWidth(true);
        desplazable.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        desplazable.getStyleClass().add("modal-scroll");
        VBox.setVgrow(desplazable, Priority.ALWAYS);

        VBox modal = new VBox(cabecera, desplazable);
        if (pie.length > 0) {
            HBox piePagina = new HBox(pie);
            piePagina.getStyleClass().add("modal-f");
            modal.getChildren().add(piePagina);
        }
        modal.getStyleClass().add("modal");
        double ancho = amplio ? ANCHO_MODAL_AMPLIO : ANCHO_MODAL;
        modal.setPrefWidth(ancho);
        modal.setMaxWidth(ancho);
        modal.maxWidthProperty().bind(Bindings.min(ancho, capaModal.widthProperty().subtract(32)));
        modal.setMaxHeight(Region.USE_PREF_SIZE);
        desplazable.prefViewportHeightProperty().bind(Bindings.min(cuerpoCaja.heightProperty(),
                capaModal.heightProperty().subtract(190)));

        Region velo = new Region();
        velo.getStyleClass().add("velo");
        velo.setOnMouseClicked(e -> cerrarModal());
        capaModal.getChildren().setAll(velo, modal);
        StackPane.setAlignment(modal, Pos.CENTER);
        capaModal.setVisible(true);
        fondo.setEffect(new GaussianBlur(8));
        fondo.setMouseTransparent(true);
        modalActual = modal;
        Movimiento.subir(modal);
        javafx.application.Platform.runLater(() -> {
            Node primero = enfocables(modal).stream().filter(n -> n != cerrar).findFirst().orElse(cerrar);
            primero.requestFocus();
        });
        return modal;
    }

    public boolean hayModal() {
        return modalActual != null;
    }

    public void cerrarModal() {
        if (modalActual == null) {
            return;
        }
        modalActual = null;
        capaModal.getChildren().clear();
        capaModal.setVisible(false);
        fondo.setEffect(null);
        fondo.setMouseTransparent(false);
        Runnable r = alCerrarModal;
        alCerrarModal = null;
        if (r != null) {
            r.run();
        }
        if (focoPrevio != null && focoPrevio.getScene() != null) {
            focoPrevio.requestFocus();
        }
    }

    public void confirmar(String titulo, String texto, String aceptar, boolean peligro, Runnable alAceptar) {
        Label mensaje = Componentes.parrafo(texto, "muted");
        Button volver = Componentes.boton("Volver", "btn-secondary", null, e -> cerrarModal());
        Button ok = Componentes.boton(aceptar, "btn-primary", null, null);
        ok.setOnAction(e -> {
            cerrarModal();
            alAceptar.run();
        });
        if (peligro) {
            ok.getStyleClass().add("peligro");
        }
        modal(titulo, mensaje, false, volver, ok);
        javafx.application.Platform.runLater(ok::requestFocus);
    }

    public void aviso(String titulo, Node cuerpo, String textoBoton) {
        modal(titulo, cuerpo, false, Componentes.boton(textoBoton, "btn-primary", null, e -> cerrarModal()));
    }

    public void popover(Region contenido, Node ancla) {
        cerrarPopover();
        if (ancla == null || ancla.getScene() == null) {
            return;
        }
        Bounds b = capaFlotante.sceneToLocal(ancla.localToScene(ancla.getBoundsInLocal()));
        atrapaClics = new Region();
        atrapaClics.setPrefSize(capaFlotante.getWidth(), capaFlotante.getHeight());
        atrapaClics.resize(capaFlotante.getWidth(), capaFlotante.getHeight());
        atrapaClics.setOnMousePressed(e -> {
            cerrarPopover();
            e.consume();
        });
        ScrollPane envoltura = new ScrollPane(contenido);
        envoltura.setFitToWidth(true);
        envoltura.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        envoltura.getStyleClass().add("pop-scroll");
        VidrioDasm vidrio = new VidrioDasm("pop-vidrio", false);
        vidrio.setRadio(16);
        vidrio.setFuente(() -> fondo);
        StackPane marco = new StackPane(vidrio, envoltura);
        marco.getStyleClass().add("pop-marco");
        marco.setPickOnBounds(true);
        marco.translateYProperty().addListener((o, antes, ahora) -> vidrio.marcar());
        // El contenido debe estar en la escena antes de medirlo: sin CSS aplicado
        // (anchos, fuentes, paddings) el tamaño preferido sale menor y la ventana se recorta.
        capaFlotante.getChildren().setAll(atrapaClics, marco);
        marco.applyCss();
        double alto = capaFlotante.getHeight();
        double maxAlto = Math.max(160, Math.min(680, alto - b.getMaxY() - 24));
        double ancho = Math.min(contenido.prefWidth(-1), capaFlotante.getWidth() - 32);
        double altoContenido = contenido.prefHeight(ancho);
        envoltura.setPrefViewportWidth(ancho);
        boolean cabe = altoContenido <= maxAlto;
        envoltura.setVbarPolicy(cabe ? ScrollPane.ScrollBarPolicy.NEVER : ScrollPane.ScrollBarPolicy.AS_NEEDED);
        envoltura.setFitToHeight(cabe);
        envoltura.setPrefViewportHeight(Math.ceil(Math.min(maxAlto, altoContenido)));
        double x = Math.max(16, Math.min(b.getMaxX() - ancho, capaFlotante.getWidth() - ancho - 16));
        double y = b.getMaxY() + 8;
        marco.autosize();
        marco.relocate(x, y);
        popoverActual = marco;
        Movimiento.subir(marco);
        javafx.application.Platform.runLater(() -> enfocables(contenido).stream().findFirst().ifPresent(Node::requestFocus));
    }

    public boolean hayPopover() {
        return popoverActual != null;
    }

    public void cerrarPopover() {
        if (capaFlotante != null) {
            capaFlotante.getChildren().clear();
        }
        popoverActual = null;
        atrapaClics = null;
    }

    public boolean cerrarArriba() {
        if (hayPopover()) {
            cerrarPopover();
            return true;
        }
        if (hayModal()) {
            cerrarModal();
            return true;
        }
        return false;
    }

    private void ciclarFoco(boolean atras) {
        List<Node> nodos = enfocables(modalActual);
        if (nodos.isEmpty()) {
            return;
        }
        Node actual = raiz.getScene().getFocusOwner();
        int i = nodos.indexOf(actual);
        int siguiente = i < 0 ? 0 : Math.floorMod(i + (atras ? -1 : 1), nodos.size());
        nodos.get(siguiente).requestFocus();
    }

    private static List<Node> enfocables(Node raizBusqueda) {
        List<Node> l = new ArrayList<>();
        recolectar(raizBusqueda, l);
        return l;
    }

    private static void recolectar(Node n, List<Node> l) {
        if (n == null || !n.isVisible() || n.isDisabled()) {
            return;
        }
        if (n instanceof Control && n.isFocusTraversable() && !(n instanceof ScrollPane)) {
            l.add(n);
        }
        if (n instanceof Parent p) {
            for (Node h : p.getChildrenUnmodifiable()) {
                recolectar(h, l);
            }
        }
    }
}
