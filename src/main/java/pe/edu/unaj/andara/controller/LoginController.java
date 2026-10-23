package pe.edu.unaj.andara.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pe.edu.unaj.andara.exception.NegocioException;
import pe.edu.unaj.andara.model.Administrador;
import pe.edu.unaj.andara.model.Persona;
import pe.edu.unaj.andara.model.Rol;
import pe.edu.unaj.andara.service.IdiomaService;
import pe.edu.unaj.andara.service.SesionService;
import pe.edu.unaj.andara.ui.*;

import java.util.Objects;

@Component
@RequiredArgsConstructor
public class LoginController implements Vista {

    private final SesionService sesion;
    private final IdiomaService idioma;
    private final Notificador notificador;
    private final MainController principal;

    @FXML
    private HBox raiz;

    private Rol rol = Rol.ESTUDIANTE;
    private String error = "";

    @Override
    public void refrescar() {
        raiz.getChildren().setAll(arte(), formulario());
    }

    private StackPane arte() {
        Image foto = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/img/carrusel/01-bienvenida-campus.jpg")));
        ImageView vista = new ImageView(foto);
        vista.setPreserveRatio(false);
        StackPane marco = new StackPane(vista);
        marco.getStyleClass().add("login-art");
        marco.setMinWidth(0);
        HBox.setHgrow(marco, Priority.ALWAYS);
        HBox.setMargin(marco, new Insets(16));
        marco.prefWidthProperty().bind(raiz.widthProperty().multiply(0.52));
        marco.layoutBoundsProperty().addListener((o, a, b) -> {
            double w = b.getWidth();
            double h = b.getHeight();
            if (w <= 0 || h <= 0) {
                return;
            }
            vista.setFitWidth(w);
            vista.setFitHeight(h);
            double escala = Math.max(w / foto.getWidth(), h / foto.getHeight());
            double vw = w / escala;
            double vh = h / escala;
            vista.setViewport(new Rectangle2D((foto.getWidth() - vw) * 0.5, (foto.getHeight() - vh) * 0.45, vw, vh));
            Rectangle recorte = new Rectangle(w, h);
            recorte.setArcWidth(48);
            recorte.setArcHeight(48);
            marco.setClip(recorte);
        });
        VBox tarjeta = new VBox(Componentes.logo(48), Componentes.literal("Andara", "h2"),
                Componentes.etiqueta("UNAJ · tus trámites más simples", "muted"));
        tarjeta.getStyleClass().add("login-art-txt");
        tarjeta.setMaxHeight(Region.USE_PREF_SIZE);
        tarjeta.setMaxWidth(Double.MAX_VALUE);
        StackPane.setAlignment(tarjeta, Pos.BOTTOM_LEFT);
        StackPane.setMargin(tarjeta, new Insets(28));
        marco.getChildren().add(tarjeta);
        return marco;
    }

    private VBox formulario() {
        FlowPane idiomas = new FlowPane(4, 4);
        idiomas.setAlignment(Pos.CENTER_RIGHT);
        ToggleGroup grupoIdioma = new ToggleGroup();
        for (String[] l : IdiomaService.IDIOMAS) {
            ToggleButton b = new ToggleButton(l[1]);
            b.getStyleClass().addAll("chip", "lang");
            b.setToggleGroup(grupoIdioma);
            b.setSelected(l[0].equals(idioma.getIdioma()));
            b.setOnAction(e -> {
                principal.cambiarIdioma(l[0]);
                b.setSelected(true);
            });
            idiomas.getChildren().add(b);
        }
        VBox.setMargin(idiomas, new Insets(0, 0, 18, 0));

        HBox marca = new HBox(10, Componentes.logo(40), new VBox(Componentes.literal("Andara", "h4"), Componentes.literal("UNAJ", "muted", "small")));
        marca.setAlignment(Pos.CENTER_LEFT);
        VBox.setMargin(marca, new Insets(0, 0, 28, 0));

        Label titulo = Componentes.parrafo("Ingresa a tu cuenta", "login-titulo");
        Label sub = Componentes.parrafo("Elige cómo quieres ingresar.", "muted");

        Segmentado tipo = new Segmentado(true).opcion("ESTUDIANTE", "Estudiante", "fth-user", null)
                .opcion("ADMINISTRADOR", "Administrador", "fth-shield", null).seleccionar(rol.name());
        tipo.alCambiar(v -> {
            rol = Rol.valueOf(v);
            error = "";
            refrescar();
        });
        VBox.setMargin(tipo, new Insets(18, 0, 20, 0));

        boolean estudiante = rol == Rol.ESTUDIANTE;
        Formulario f = new Formulario();
        Campo<TextField> usuario = f.agregar(Formulario.texto(estudiante ? "Código universitario o DNI" : "Correo institucional", null, ""))
                .requerido(estudiante ? "Escribe tu código o DNI." : "Escribe tu correo institucional.");
        Campo<PasswordField> clave = f.agregar(Formulario.clave("Contraseña")).requerido("Escribe tu contraseña.");
        VBox.setMargin(clave.nodo(), new Insets(14, 0, 0, 0));
        Label lblError = Componentes.parrafo(error, "err");
        lblError.setVisible(!error.isBlank());
        lblError.setManaged(!error.isBlank());
        VBox.setMargin(lblError, new Insets(10, 0, 0, 0));
        Button ingresar = Componentes.bloque(Componentes.boton(estudiante ? "Ingresar como estudiante" : "Ingresar como administrador",
                "btn-primary", null, null));
        ingresar.setDefaultButton(true);
        VBox.setMargin(ingresar, new Insets(14, 0, 0, 0));
        ingresar.setOnAction(e -> {
            if (!f.validar()) {
                return;
            }
            try {
                sesion.ingresar(rol, usuario.control().getText(), clave.control().getText());
                error = "";
                Navegacion.ir(raiz, "sesion:entrar");
            } catch (NegocioException ex) {
                error = ex.getMessage();
                Componentes.fijar(lblError, error);
                lblError.setVisible(true);
                lblError.setManaged(true);
                clave.control().clear();
                clave.control().requestFocus();
            }
        });
        Button olvido = Componentes.enlaceFlecha("¿Olvidaste tu contraseña?", e -> notificador.info(
                "En el sistema real recibirías un enlace en tu correo institucional. En esta demostración usa la cuenta de ejemplo."));
        olvido.setGraphic(null);
        VBox.setMargin(olvido, new Insets(8, 0, 0, 0));

        VBox demo = new VBox();
        demo.getStyleClass().add("demo-box");
        demo.getChildren().add(Componentes.etiqueta("Cuenta de demostración", "strong"));
        if (estudiante) {
            Persona p = sesion.estudiante();
            demo.getChildren().add(Componentes.parrafo("Código " + p.getCodigo() + " o DNI " + p.getDni() + " · contraseña andara2026"));
        } else {
            Administrador a = sesion.administrador();
            demo.getChildren().add(Componentes.parrafo("Correo " + a.getCorreo() + " · contraseña admin2026"));
        }
        demo.getChildren().add(Componentes.botonSm("Usar cuenta de demostración", "btn-secondary", null, e -> {
            usuario.control().setText(estudiante ? sesion.estudiante().getCodigo() : sesion.administrador().getCorreo());
            clave.control().setText(estudiante ? "andara2026" : "admin2026");
            ingresar.requestFocus();
        }));
        VBox.setMargin(demo, new Insets(22, 0, 0, 0));
        Label nota = Componentes.parrafo("Prototipo: solo funcionan las cuentas de demostración y no se envía información a ningún servidor.", "hint");
        VBox.setMargin(nota, new Insets(14, 0, 0, 0));

        VBox caja = new VBox(idiomas, marca, titulo, sub, tipo, usuario.nodo(), clave.nodo(), lblError, ingresar, olvido, demo, nota);
        caja.getStyleClass().add("login-box");
        caja.setPrefWidth(440);
        caja.setMaxWidth(440);
        VBox columna = new VBox(caja);
        columna.setAlignment(Pos.CENTER);
        HBox.setHgrow(columna, Priority.ALWAYS);
        ScrollPane sp = new ScrollPane(columna);
        sp.setFitToWidth(true);
        sp.setFitToHeight(true);
        sp.getStyleClass().add("pagina-scroll");
        VBox envoltura = new VBox(sp);
        VBox.setVgrow(sp, Priority.ALWAYS);
        envoltura.prefWidthProperty().bind(raiz.widthProperty().multiply(0.48));
        envoltura.setMinWidth(460);
        Platform.runLater(() -> usuario.control().requestFocus());
        return envoltura;
    }
}
