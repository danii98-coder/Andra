package pe.edu.unaj.andara.controller;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import lombok.RequiredArgsConstructor;
import org.kordamp.ikonli.javafx.FontIcon;
import org.springframework.stereotype.Component;
import pe.edu.unaj.andara.model.Persona;
import pe.edu.unaj.andara.model.UsoCarne;
import pe.edu.unaj.andara.service.CarneService;
import pe.edu.unaj.andara.service.QrService;
import pe.edu.unaj.andara.service.SesionService;
import pe.edu.unaj.andara.ui.Componentes;
import pe.edu.unaj.andara.ui.Movimiento;
import pe.edu.unaj.andara.ui.Rejilla;
import pe.edu.unaj.andara.ui.TarjetaCarne;
import pe.edu.unaj.andara.util.Fechas;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class CarneController extends VistaBase {

    private final CarneService carne;
    private final SesionService sesion;

    private Timeline reloj;
    private long franjaDibujada = -1;

    @Override
    public void salir() {
        if (reloj != null) {
            reloj.stop();
        }
    }

    @Override
    protected List<Node> construir() {
        salir();
        Persona yo = sesion.estudiante();
        boolean vigente = carne.vigente();
        List<Node> nodos = new ArrayList<>();
        nodos.add(Componentes.hero("Andara · UNAJ", "Mi carné digital",
                "Tu carné vive en tu celular. Muéstralo en la biblioteca, en el préstamo de equipos o al entrar a un laboratorio.", null));
        Label offline = Componentes.etiqueta("Disponible sin conexión", "off-ok");
        offline.setGraphic(new FontIcon("fth-cloud"));
        HBox insignias = new HBox(8, offline);
        insignias.setAlignment(Pos.CENTER);
        if (!vigente) {
            insignias.getChildren().add(Componentes.botonSm("Confirmar matrícula en Andara Pay", "btn-primary", null, e -> ir("pay")));
        }
        VBox izquierda = new VBox(14, TarjetaCarne.crear(yo, vigente, carne.foto().orElse(null)), insignias);
        ImageView qr = new ImageView();
        qr.setFitWidth(168);
        qr.setFitHeight(168);
        StackPane marco = new StackPane(qr);
        marco.getStyleClass().add("qr-img");
        marco.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        Componentes.accesible(marco, "Código QR dinámico del carné");
        Circle fondo = new Circle(19);
        fondo.getStyleClass().add("qr-ring-bg");
        Arc arco = new Arc(0, 0, 19, 19, 90, 360);
        arco.setType(ArcType.OPEN);
        arco.getStyleClass().add("qr-ring-fg");
        StackPane anillo = new StackPane(fondo, arco);
        anillo.setMaxSize(44, 44);
        StackPane conAnillo = new StackPane(marco, anillo);
        StackPane.setAlignment(anillo, Pos.TOP_RIGHT);
        StackPane.setMargin(anillo, new Insets(-12, -12, 0, 0));
        conAnillo.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        Label restante = Componentes.etiqueta("Se renueva en 15 s", "hint");
        franjaDibujada = -1;
        Runnable actualizar = () -> {
            long ahora = Fechas.milis();
            long f = CarneService.franja(ahora);
            if (f != franjaDibujada) {
                qr.setImage(QrService.imagen(CarneService.token(yo.getCodigo(), f), 336));
                franjaDibujada = f;
            }
            arco.setLength(360 * CarneService.fraccionRestante(ahora));
            Componentes.fijar(restante, "Se renueva en " + CarneService.segundosRestantes(ahora) + " s");
        };
        actualizar.run();
        reloj = new Timeline(new KeyFrame(Duration.millis(500), e -> actualizar.run()));
        reloj.setCycleCount(Animation.INDEFINITE);
        reloj.play();
        HBox botones = new HBox(8, Componentes.botonSm("Ampliar código", "btn-secondary", "fth-maximize-2", e -> ampliar(yo)),
                Componentes.botonSm("Simular lectura en puerta", "btn-primary", "fth-maximize", e -> leer()));
        botones.setAlignment(Pos.CENTER);
        VBox derecha = new VBox(10, Componentes.parrafo("Código de acceso", "h4"),
                Componentes.parrafo("Este código cambia cada 15 segundos. Así nadie puede usar una captura de pantalla de tu carné.", "muted", "small"),
                conAnillo, restante, botones, Componentes.parrafo("Sube el brillo de tu pantalla para que el lector lo capte mejor.", "hint"));
        derecha.getStyleClass().add("qr-panel");
        GridPane dos = new GridPane();
        dos.setHgap(28);
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setPercentWidth(52);
        ColumnConstraints c2 = new ColumnConstraints();
        c2.setPercentWidth(48);
        dos.getColumnConstraints().addAll(c1, c2);
        dos.add(izquierda, 0, 0);
        dos.add(derecha, 1, 0);
        GridPane.setValignment(izquierda, javafx.geometry.VPos.TOP);
        VBox s1 = Componentes.seccion(dos);
        s1.getStyleClass().add("sin-arriba");
        nodos.add(s1);
        Rejilla usos = new Rejilla(4, 200, 16);
        String[][] datos = {{"fth-book", "Biblioteca", "Ingreso y préstamo de libros"}, {"fth-box", "Equipos", "Retiro y devolución de préstamos"},
                {"fth-droplet", "Laboratorios", "Acceso a tus reservas"}, {"fth-home", "Auditorios", "Ingreso a eventos y ceremonias"}};
        for (String[] d : datos) {
            StackPane ic = new StackPane(new FontIcon(d[0]));
            ic.getStyleClass().add("sicon-sm");
            VBox o = new VBox(12, ic, Componentes.parrafo(d[1], "h3"), Componentes.parrafo(d[2], "muted", "small"));
            o.getStyleClass().add("office");
            usos.getChildren().add(o);
        }
        nodos.add(Componentes.seccion(usos));
        List<Node> filas = new ArrayList<>();
        for (UsoCarne u : carne.ultimos()) {
            filas.add(Componentes.fila(Componentes.textosFila(u.getLugar(), Fechas.dmy(u.getFecha().toLocalDate()) + " · " + Fechas.hm(u.getFecha())),
                    Componentes.pill(u.isConcedido() ? "Acceso concedido" : "Rechazado", u.isConcedido() ? "success" : "error")));
        }
        nodos.add(Componentes.seccion(Componentes.cabeceraSeccion("Últimos usos", null),
                filas.isEmpty() ? Componentes.vacio("fth-credit-card", "Sin registros", "Aún no usaste tu carné.") : Componentes.lista(filas)));
        return nodos;
    }

    private void ampliar(Persona yo) {
        ImageView qr = new ImageView(QrService.imagen(carne.tokenActual(), 520));
        qr.setFitWidth(260);
        qr.setFitHeight(260);
        StackPane marco = new StackPane(qr);
        marco.getStyleClass().add("qr-img");
        marco.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        VBox cuerpo = new VBox(12, marco, Componentes.literal(yo.getNombre() + " · " + yo.getCodigo(), "hint"));
        cuerpo.setAlignment(Pos.CENTER);
        notificador.modal("Código de acceso", cuerpo, false, Componentes.boton("Cerrar", "btn-primary", null, e -> notificador.cerrarModal()));
    }

    private void leer() {
        String lugar = CarneService.LUGARES.get(new java.util.Random().nextInt(CarneService.LUGARES.size()));
        Rectangle linea = new Rectangle(160, 3);
        linea.setArcWidth(3);
        linea.setArcHeight(3);
        linea.getStyleClass().add("scan-line");
        StackPane caja = new StackPane(new FontIcon("fth-maximize"), linea);
        caja.getStyleClass().add("scan-box");
        if (!Movimiento.reducido()) {
            TranslateTransition t = new TranslateTransition(Duration.millis(1200), linea);
            t.setFromY(-70);
            t.setToY(70);
            t.setAutoReverse(true);
            t.setCycleCount(Animation.INDEFINITE);
            t.play();
        }
        VBox cuerpo = new VBox(14, caja, Componentes.parrafo("Leyendo tu código en " + lugar + "…", "muted"));
        cuerpo.setAlignment(Pos.CENTER);
        notificador.modal("Lector de puerta", cuerpo, false);
        PauseTransition p = new PauseTransition(Duration.millis(1300));
        p.setOnFinished(e -> {
            if (!notificador.hayModal()) {
                return;
            }
            UsoCarne uso = carne.simularLectura();
            boolean ok = uso.isConcedido();
            StackPane marca = new StackPane(new FontIcon(ok ? "fth-check" : "fth-x"));
            marca.getStyleClass().add("ok-check");
            if (!ok) {
                marca.getStyleClass().add("no");
            }
            VBox r = new VBox(6, marca, Componentes.parrafo(ok ? "Acceso concedido" : "Acceso denegado", "h3"), Componentes.literal(uso.getLugar()),
                    Componentes.parrafo(ok ? "Carné vigente · " + sesion.estudiante().getNombre() : "Tu carné está pendiente: confirma tu matrícula en Andara Pay.", "hint"));
            r.getStyleClass().add("ok-hero");
            notificador.modal("Lector de puerta", r, false, Componentes.boton("Listo", "btn-primary", null, x -> {
                notificador.cerrarModal();
                redibujar();
            }));
        });
        p.play();
    }
}
