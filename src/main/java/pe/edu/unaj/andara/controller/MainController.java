package pe.edu.unaj.andara.controller;

import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.util.Duration;
import lombok.RequiredArgsConstructor;
import org.kordamp.ikonli.javafx.FontIcon;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import pe.edu.unaj.andara.model.*;
import pe.edu.unaj.andara.service.*;
import pe.edu.unaj.andara.ui.*;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class MainController {

    private final ApplicationContext contexto;
    private final Notificador notificador;
    private final ContextoService contextoService;
    private final CatalogoService catalogo;
    private final NotificacionService notificaciones;
    private final DemoService demo;
    private final SesionService sesion;
    private final IdiomaService idioma;
    private final AjustesService ajustes;
    private final ConexionService conexion;

    @FXML private StackPane raiz;
    @FXML private StackPane aplicacion;
    @FXML private StackPane contenido;
    @FXML private VBox capaCabecera;
    @FXML private Pane capaFlotante;
    @FXML private StackPane capaModal;
    @FXML private VBox zonaToasts;

    private Stage ventana;
    private final ToggleGroup grupoNav = new ToggleGroup();
    private final Map<String, Vista> controladores = new HashMap<>();
    private final Map<String, Parent> vistas = new HashMap<>();
    private String rutaActual;
    private VidrioDasm vidrio;
    private HBox nav;
    private Label insignia;
    private Circle puntoEscuela;
    private Button btnEscuela;
    private Button btnSede;
    private Button btnCampana;
    private Button btnUsuario;
    private Button btnIdioma;
    private Label lema;
    private HBox barraSinConexion;
    private boolean silencioNav;

    @FXML
    public void initialize() {
        notificador.registrar(raiz, aplicacion, capaFlotante, capaModal, zonaToasts);
        raiz.addEventHandler(Navegacion.IR, e -> navegar(e.getRuta()));
        Ajustes a = ajustes.obtener();
        idioma.setIdioma(a.getIdioma());
        Movimiento.reducidoProperty().set(a.isReducirMovimiento());
        construirCabecera();
        conexion.enLineaProperty().addListener((o, antes, ahora) -> alCambiarConexion(antes, ahora));
        conexion.pendientesProperty().addListener((o, x, y) -> actualizarBarraSinConexion());
        Timeline refresco = new Timeline(new KeyFrame(Duration.millis(500), e -> vidrio.marcar()));
        refresco.setCycleCount(Timeline.INDEFINITE);
        refresco.play();
        grupoNav.selectedToggleProperty().addListener((obs, anterior, actual) -> {
            if (actual == null) {
                if (anterior != null && !silencioNav) {
                    grupoNav.selectToggle(anterior);
                }
                return;
            }
            if (!silencioNav) {
                mostrar((String) actual.getUserData());
            }
        });
    }

    public void iniciar(Stage ventana) {
        this.ventana = ventana;
        ventana.getScene().addEventFilter(KeyEvent.KEY_PRESSED, this::atajos);
        raiz.widthProperty().addListener((o, a, b) -> lema.setVisible(b.doubleValue() > 1120));
        arrancarSesion();
    }

    public void guardarVentana() {
        if (ventana == null) {
            return;
        }
        ajustes.cambiar(a -> {
            a.setMaximizada(ventana.isMaximized());
            if (!ventana.isMaximized()) {
                a.setVentanaX(ventana.getX());
                a.setVentanaY(ventana.getY());
                a.setVentanaAncho(ventana.getWidth());
                a.setVentanaAlto(ventana.getScene().getHeight());
            }
        });
    }

    private void atajos(KeyEvent e) {
        if (e.getCode() == KeyCode.ESCAPE) {
            if (notificador.cerrarArriba()) {
                e.consume();
            }
            return;
        }
        boolean mod = e.isShortcutDown();
        if (!mod || !sesion.conectado()) {
            return;
        }
        if (e.getCode() == KeyCode.F) {
            Vista v = controladores.get(rutaActual);
            if (v != null) {
                v.enfocarBusqueda();
                e.consume();
            }
            return;
        }
        if (e.getCode().isDigitKey()) {
            int n = e.getCode().getCode() - KeyCode.DIGIT1.getCode();
            List<SesionService.Ruta> rutas = SesionService.rutas(sesion.rol());
            if (n >= 0 && n < rutas.size() && !notificador.hayModal()) {
                irA(rutas.get(n).clave());
                e.consume();
            }
        }
    }

    private void arrancarSesion() {
        controladores.values().forEach(Vista::salir);
        controladores.clear();
        vistas.clear();
        notificador.cerrarArriba();
        construirNavegacion();
        boolean conectado = sesion.conectado();
        capaCabecera.setVisible(conectado);
        capaCabecera.setManaged(conectado);
        if (!conectado) {
            mostrar("login");
            return;
        }
        actualizarCabecera();
        irA(SesionService.rutaInicial(sesion.rol()));
    }

    public void alIngresar() {
        arrancarSesion();
        if (sesion.esAdministrador()) {
            notificador.exito("Hola, " + sesion.administrador().getPrimerNombre() + ". Bienvenida al panel.");
        } else {
            notificador.exito("Hola, " + sesion.estudiante().getPrimerNombre() + ".");
        }
    }

    private void construirCabecera() {
        vidrio = new VidrioDasm("header");
        vidrio.setFuente(() -> contenido.getChildren().isEmpty() ? null : contenido.getChildren().get(0));

        StackPane logo = new StackPane(Componentes.logo(34));
        VBox textos = new VBox(Componentes.literal("Andara", "brand-name"), Componentes.literal("UNAJ", "brand-sub"));
        textos.setAlignment(Pos.CENTER_LEFT);
        lema = Componentes.literal("tus trámites más simples", "brand-tag");
        lema.managedProperty().bind(lema.visibleProperty());
        HBox marca = new HBox(logo, textos, lema);
        marca.getStyleClass().add("brand");
        Componentes.accesible(marca, "Andara UNAJ, tus trámites más simples. Ir al inicio");
        marca.setOnMouseClicked(e -> irA(SesionService.rutaInicial(sesion.rol())));

        btnIdioma = Componentes.iconoRedondo("fth-globe", "Idioma", e -> popoverIdioma());
        btnCampana = Componentes.iconoRedondo("fth-bell", "Notificaciones", e -> popoverNotificaciones());
        insignia = Componentes.literal("", "dot-badge");
        insignia.setMouseTransparent(true);
        StackPane campana = new StackPane(btnCampana, insignia);
        StackPane.setAlignment(insignia, Pos.TOP_RIGHT);
        StackPane.setMargin(insignia, new Insets(4, 4, 0, 0));
        btnEscuela = Componentes.iconoRedondo("fth-home", "Escuela profesional", e -> popoverEscuela());
        puntoEscuela = new Circle(4);
        puntoEscuela.getStyleClass().add("fac-dot");
        puntoEscuela.setMouseTransparent(true);
        StackPane escuela = new StackPane(btnEscuela, puntoEscuela);
        StackPane.setAlignment(puntoEscuela, Pos.TOP_RIGHT);
        StackPane.setMargin(puntoEscuela, new Insets(8, 8, 0, 0));
        btnSede = new Button();
        btnSede.getStyleClass().add("ctx-btn");
        btnSede.setGraphic(new FontIcon("fth-map-pin"));
        btnSede.setOnAction(e -> popoverSede());
        HBox.setMargin(btnSede, new Insets(0, 4, 0, 4));
        btnUsuario = Componentes.iconoRedondo("fth-user", "Mi cuenta", e -> popoverUsuario());
        HBox acciones = new HBox(btnIdioma, campana, escuela, btnSede, btnUsuario);
        acciones.getStyleClass().add("header-actions");

        HBox fila = new HBox(marca, Componentes.espacio(), acciones);
        fila.getStyleClass().add("header-in");
        fila.setMaxWidth(1168);
        HBox centrada = new HBox(fila);
        centrada.setAlignment(Pos.CENTER);
        HBox.setHgrow(fila, Priority.ALWAYS);

        nav = new HBox();
        nav.getStyleClass().add("nav");
        ScrollPane navScroll = new ScrollPane(nav);
        navScroll.getStyleClass().add("nav-scroll");
        navScroll.setFitToHeight(true);
        navScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        navScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        nav.minWidthProperty().bind(navScroll.widthProperty());
        HBox subnav = new HBox(navScroll);
        HBox.setHgrow(navScroll, Priority.ALWAYS);
        subnav.getStyleClass().add("subnav");

        VBox cabecera = new VBox(centrada, subnav);
        vidrio.getChildren().add(cabecera);

        barraSinConexion = new HBox();
        barraSinConexion.getStyleClass().add("off-bar");
        barraSinConexion.setVisible(false);
        barraSinConexion.setManaged(false);
        capaCabecera.getChildren().setAll(vidrio, barraSinConexion);
    }

    private void construirNavegacion() {
        silencioNav = true;
        grupoNav.getToggles().clear();
        nav.getChildren().clear();
        silencioNav = false;
        Rol rol = sesion.rol();
        if (rol == null) {
            return;
        }
        for (SesionService.Ruta r : SesionService.rutas(rol)) {
            ToggleButton b = new ToggleButton();
            Componentes.fijar(b, r.etiqueta());
            b.setUserData(r.clave());
            b.setToggleGroup(grupoNav);
            b.getStyleClass().add("nav-link");
            b.setMnemonicParsing(false);
            nav.getChildren().add(b);
        }
    }

    public void irA(String ruta) {
        notificador.cerrarPopover();
        Rol rol = sesion.rol();
        if (!SesionService.puedeAcceder(rol, ruta)) {
            ruta = SesionService.rutaInicial(rol);
        }
        String destino = ruta;
        Toggle t = grupoNav.getToggles().stream().filter(x -> destino.equals(x.getUserData())).findFirst().orElse(null);
        if (t == null) {
            silencioNav = true;
            grupoNav.selectToggle(null);
            silencioNav = false;
            mostrar(destino);
        } else if (grupoNav.getSelectedToggle() == t) {
            mostrar(destino);
        } else {
            grupoNav.selectToggle(t);
        }
    }

    private void navegar(String ruta) {
        switch (ruta) {
            case "ctx:sede" -> popoverSede();
            case "ctx:escuela" -> popoverEscuela();
            case "contacto" -> {
                irA("ayuda");
                if (controladores.get("ayuda") instanceof AyudaController a) {
                    Platform.runLater(a::irACanales);
                }
            }
            case "mapa" -> {
                irA("reservas");
                if (controladores.get("reservas") instanceof ReservasController r) {
                    r.mostrarMapa();
                }
            }
            case "sesion:entrar" -> alIngresar();
            case "telefono" -> contexto.getBean(PieDePagina.class).modalTelefono();
            default -> irA(ruta);
        }
    }

    private void mostrar(String ruta) {
        if (rutaActual != null && controladores.get(rutaActual) != null) {
            controladores.get(rutaActual).salir();
        }
        rutaActual = ruta;
        Parent vista = vistas.computeIfAbsent(ruta, this::cargar);
        contenido.getChildren().setAll(vista);
        Vista v = controladores.get(ruta);
        if (v != null) {
            v.refrescar();
        }
        if (vista instanceof ScrollPane sp) {
            sp.setVvalue(0);
            sp.vvalueProperty().addListener((o, a, b) -> vidrio.marcar());
            if (sp.getContent() != null) {
                sp.getContent().layoutBoundsProperty().addListener((o, a, b) -> vidrio.marcar());
            }
        }
        actualizarBarraSinConexion();
        Movimiento.aparecer(vista, Duration.millis(220), Duration.ZERO);
        actualizarTitulo();
        vidrio.marcar();
        Platform.runLater(contenido::requestFocus);
    }

    private Parent cargar(String ruta) {
        FXMLLoader cargador = new FXMLLoader(getClass().getResource("/view/" + ruta + "-view.fxml"));
        cargador.setControllerFactory(contexto::getBean);
        cargador.setClassLoader(getClass().getClassLoader());
        try {
            Parent nodo = cargador.load();
            if (cargador.getController() instanceof Vista v) {
                controladores.put(ruta, v);
            }
            return nodo;
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo cargar la vista " + ruta, e);
        }
    }

    private void actualizarTitulo() {
        if (ventana == null) {
            return;
        }
        String titulo;
        if ("login".equals(rutaActual)) {
            titulo = "Andara · Ingresar";
        } else if ("inicio".equals(rutaActual)) {
            titulo = "Andara";
        } else if ("admin".equals(rutaActual)) {
            titulo = "Andara · Panel";
        } else {
            titulo = "Andara · " + SesionService.etiqueta(rutaActual);
        }
        ventana.titleProperty().unbind();
        ventana.titleProperty().bind(IdiomaService.enlace(titulo));
    }

    private void refrescarActual() {
        Vista v = controladores.get(rutaActual);
        if (v != null) {
            v.refrescar();
        }
    }

    private void actualizarCabecera() {
        if (!sesion.conectado()) {
            return;
        }
        Componentes.fijar(btnSede, contextoService.nombreSede());
        Componentes.accesible(btnSede, "Sede: " + contextoService.nombreSede() + ". Cambiar sede");
        EscuelaProfesional e = contextoService.escuela();
        puntoEscuela.setVisible(e != null);
        Componentes.ayuda(btnEscuela, e == null ? "Todas las escuelas" : e.getNombre());
        Componentes.accesible(btnEscuela, "Escuela profesional: " + (e == null ? "todas" : e.getNombre()) + ". Cambiar escuela");
        actualizarCampana();
    }

    private void actualizarCampana() {
        long n = notificaciones.noLeidas(sesion.rol());
        insignia.setText(String.valueOf(n));
        insignia.setVisible(n > 0);
        Componentes.accesible(btnCampana, "Notificaciones" + (n > 0 ? ", " + n + " sin leer" : ""));
    }

    @EventListener
    public void alCambiarDatos(CambioDatos evento) {
        if (insignia == null) {
            return;
        }
        if (Platform.isFxApplicationThread()) {
            actualizarCampana();
        } else {
            try {
                Platform.runLater(this::actualizarCampana);
            } catch (IllegalStateException ignorado) {
                return;
            }
        }
    }

    private void alCambiarConexion(boolean antes, boolean ahora) {
        actualizarBarraSinConexion();
        if (!sesion.conectado()) {
            return;
        }
        if (antes && !ahora && !conexion.isSimulado()) {
            notificador.info("Te quedaste sin conexión. Andara sigue funcionando con tus datos guardados.");
        }
        if (!antes && ahora) {
            sincronizar();
        }
    }

    private void sincronizar() {
        int n = conexion.contarPendientes();
        actualizarBarraSinConexion();
        if (n == 0) {
            return;
        }
        String s = n > 1 ? "s" : "";
        notificador.info("Conexión recuperada. Sincronizando " + n + " cambio" + s + "…");
        PauseTransition p = new PauseTransition(Duration.millis(1200));
        p.setOnFinished(e -> {
            conexion.sincronizar();
            actualizarBarraSinConexion();
            notificador.exito("Listo: " + n + " cambio" + s + " sincronizado" + s + ".");
        });
        p.play();
    }

    private void actualizarBarraSinConexion() {
        boolean visible = conexion.sinConexion() && sesion.conectado();
        barraSinConexion.setVisible(visible);
        barraSinConexion.setManaged(visible);
        if (visible) {
            int n = conexion.contarPendientes();
            Label texto = Componentes.parrafo("Puedes ver tus documentos, tu carné y tus reservas. Lo que hagas se enviará al volver la conexión.");
            Label negrita = Componentes.etiqueta("Sin conexión.", "off-negrita");
            HBox mensaje = new HBox(6, negrita, texto);
            mensaje.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(mensaje, Priority.ALWAYS);
            barraSinConexion.getChildren().setAll(new FontIcon("fth-wifi-off"), mensaje);
            if (n > 0) {
                barraSinConexion.getChildren().add(Componentes.pillPlano(n + " cambio" + (n > 1 ? "s" : "") + " por sincronizar", "warning"));
            }
            if (conexion.isSimulado()) {
                barraSinConexion.getChildren().add(Componentes.botonSm("Volver a conectarse", "btn-secondary", null,
                        e -> conexion.simularSinConexion(false)));
            }
        }
        if (!contenido.getChildren().isEmpty() && contenido.getChildren().get(0) instanceof ScrollPane sp
                && sp.getContent() != null) {
            sp.getContent().getStyleClass().remove("con-barra");
            if (visible) {
                sp.getContent().getStyleClass().add("con-barra");
            }
        }
    }

    private VBox caja() {
        VBox caja = new VBox();
        caja.getStyleClass().add("pop");
        return caja;
    }

    private HBox cabeceraPopover(String titulo, Node accion) {
        HBox c = new HBox(Componentes.etiqueta(titulo, "pop-titulo"), Componentes.espacio());
        if (accion != null) {
            c.getChildren().add(accion);
        }
        c.getStyleClass().add("pop-h");
        return c;
    }

    private HBox opcion(String titulo, String subtitulo, boolean elegida, boolean literal, Runnable accion) {
        FontIcon check = Componentes.icono("fth-check", "ck");
        check.setVisible(elegida);
        VBox textos = new VBox(1, literal ? Componentes.literalParrafo(titulo, "opt-titulo") : Componentes.parrafo(titulo, "opt-titulo"));
        if (subtitulo != null) {
            textos.getChildren().add(Componentes.parrafo(subtitulo, "opt-sub"));
        }
        HBox.setHgrow(textos, Priority.ALWAYS);
        HBox fila = new HBox(check, textos);
        HBox.setMargin(check, new Insets(2, 0, 0, 0));
        fila.getStyleClass().add("opt");
        if (elegida) {
            fila.getStyleClass().add("on");
        }
        fila.setFocusTraversable(true);
        fila.setOnMouseClicked(e -> accion.run());
        fila.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER || e.getCode() == KeyCode.SPACE) {
                accion.run();
            }
        });
        return fila;
    }

    private HBox opcionMenu(String texto, boolean peligro, Runnable accion) {
        Label l = Componentes.etiqueta(texto, "notif-texto");
        if (peligro) {
            l.getStyleClass().add("peligro");
        }
        HBox fila = new HBox(l);
        fila.getStyleClass().addAll("notif", "read");
        fila.setAlignment(Pos.CENTER_LEFT);
        fila.setFocusTraversable(true);
        Runnable ejecutar = () -> {
            notificador.cerrarPopover();
            accion.run();
        };
        fila.setOnMouseClicked(e -> ejecutar.run());
        fila.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER || e.getCode() == KeyCode.SPACE) {
                ejecutar.run();
            }
        });
        return fila;
    }

    private Region separador() {
        Region s = new Region();
        s.getStyleClass().add("pop-sep");
        VBox.setMargin(s, new Insets(6, 0, 6, 0));
        return s;
    }

    private void popoverIdioma() {
        VBox c = caja();
        c.getChildren().add(cabeceraPopover("Idioma", null));
        for (String[] l : IdiomaService.IDIOMAS) {
            c.getChildren().add(opcion(l[1], l[2], l[0].equals(idioma.getIdioma()), true, () -> cambiarIdioma(l[0])));
        }
        Label nota = Componentes.parrafo("Traducción de demostración: el quechua y el aimara deben ser revisados por hablantes nativos.", "hint");
        nota.setPadding(new Insets(8, 12, 4, 12));
        c.getChildren().add(nota);
        notificador.popover(c, btnIdioma);
    }

    public void cambiarIdioma(String codigo) {
        notificador.cerrarPopover();
        idioma.setIdioma(codigo);
        ajustes.cambiar(a -> a.setIdioma(codigo));
        String aviso = switch (codigo) {
            case "en" -> "Language: English";
            case "qu" -> "Simi: Runasimi";
            case "ay" -> "Aru: Aymar aru";
            default -> "Idioma: Español";
        };
        notificador.info(aviso);
    }

    private void popoverNotificaciones() {
        VBox c = caja();
        Rol rol = sesion.rol();
        Button marcar = Componentes.botonSm("Marcar todo como leído", "btn-tertiary", null, e -> {
            notificaciones.marcarTodas(rol);
            popoverNotificaciones();
        });
        c.getChildren().add(cabeceraPopover("Notificaciones", marcar));
        List<Notificacion> lista = notificaciones.listar(rol);
        if (lista.isEmpty()) {
            Label vacio = Componentes.etiqueta("No tienes notificaciones.", "muted", "small");
            vacio.setPadding(new Insets(12));
            c.getChildren().add(vacio);
        }
        for (Notificacion n : lista) {
            Circle punto = new Circle(4);
            punto.getStyleClass().add("bullet");
            Label texto = Componentes.parrafo(n.getTexto(), "notif-texto");
            HBox.setHgrow(texto, Priority.ALWAYS);
            HBox fila = new HBox(punto, texto);
            HBox.setMargin(punto, new Insets(7, 0, 0, 0));
            fila.getStyleClass().add("notif");
            fila.setFocusTraversable(true);
            if (n.isLeida()) {
                fila.getStyleClass().add("read");
            }
            Runnable abrir = () -> {
                String destino = notificaciones.abrir(n.getId());
                notificador.cerrarPopover();
                irA(destino);
            };
            fila.setOnMouseClicked(e -> abrir.run());
            fila.setOnKeyPressed(e -> {
                if (e.getCode() == KeyCode.ENTER) {
                    abrir.run();
                }
            });
            c.getChildren().add(fila);
        }
        notificador.popover(c, btnCampana);
    }

    private void popoverSede() {
        VBox c = caja();
        c.getChildren().add(cabeceraPopover("Sede", null));
        String actual = contextoService.codigoSede();
        if (sesion.esAdministrador()) {
            c.getChildren().add(opcion("Todas las sedes", "Vista consolidada", Ajustes.TODAS.equals(actual), false,
                    () -> cambiarContexto(contextoService.cambiarSede(Ajustes.TODAS))));
        }
        for (Sede s : catalogo.sedes()) {
            c.getChildren().add(opcion(s.getNombreCompleto(), s.getDescripcion(), s.getCodigo().equals(actual), false,
                    () -> cambiarContexto(contextoService.cambiarSede(s.getCodigo()))));
        }
        notificador.popover(c, btnSede);
    }

    private void popoverEscuela() {
        EscuelaProfesional actual = contextoService.escuela();
        VBox opciones = new VBox();
        opciones.getChildren().add(opcion("Todas las escuelas", "Ver todo lo de la sede " + contextoService.nombreSede(),
                actual == null, false, () -> cambiarContexto(contextoService.cambiarEscuela(null))));
        for (Sede s : catalogo.sedes()) {
            List<EscuelaProfesional> escuelas = catalogo.escuelasDe(s);
            if (escuelas.isEmpty()) {
                continue;
            }
            opciones.getChildren().add(Componentes.mayus("Sede " + s.getNombre(), "pop-group"));
            for (EscuelaProfesional e : escuelas) {
                opciones.getChildren().add(opcion(e.getNombre(), null, actual != null && actual.getId().equals(e.getId()), false,
                        () -> cambiarContexto(contextoService.cambiarEscuela(e.getCodigo()))));
            }
        }
        VBox c = caja();
        c.getChildren().addAll(cabeceraPopover("Escuela profesional", null), opciones);
        notificador.popover(c, btnEscuela);
    }

    private void cambiarContexto(String mensaje) {
        notificador.cerrarPopover();
        actualizarCabecera();
        refrescarActual();
        notificador.exito(mensaje);
    }

    private void popoverUsuario() {
        VBox c = caja();
        VBox datos = new VBox(2);
        datos.setPadding(new Insets(12, 12, 8, 12));
        boolean admin = sesion.esAdministrador();
        if (admin) {
            Administrador a = sesion.administrador();
            datos.getChildren().addAll(Componentes.literal(a.getNombre(), "strong", "texto"),
                    Componentes.parrafo(a.getCargo(), "muted", "small"), Componentes.literal(a.getCorreo(), "muted", "small"));
        } else {
            Persona p = sesion.estudiante();
            datos.getChildren().addAll(Componentes.literal(p.getNombre(), "strong", "texto"),
                    Componentes.etiqueta("Código " + p.getCodigo() + " · DNI " + p.getDni(), "muted", "small"),
                    Componentes.parrafo("EP " + p.getEscuela().getNombre() + " · Sede " + p.getSede().getNombre(), "muted", "small"));
        }
        Label rol = Componentes.etiqueta(admin ? "Administrador" : "Estudiante", "user-role");
        VBox.setMargin(rol, new Insets(6, 0, 0, 0));
        datos.getChildren().add(rol);
        c.getChildren().addAll(datos, separador());
        if (admin) {
            c.getChildren().addAll(opcionMenu("Panel", false, () -> irA("admin")),
                    opcionMenu("Transacciones", false, () -> irA("a-pagos")),
                    opcionMenu("Análisis", false, () -> irA("a-datos")));
        } else {
            c.getChildren().addAll(opcionMenu("Mis solicitudes", false, () -> irA("solicitudes")),
                    opcionMenu("Mi carné digital", false, () -> irA("carne")),
                    opcionMenu("Andara Pay", false, () -> irA("pay")),
                    opcionMenu("Mis favoritos", false, () -> irA("favoritos")));
        }
        boolean simulado = conexion.isSimulado();
        c.getChildren().add(opcionMenu(simulado ? "Volver a conectarse" : "Probar modo sin conexión", false, () -> {
            conexion.simularSinConexion(!simulado);
            if (!simulado) {
                notificador.info("Modo sin conexión activado para la prueba.");
            }
        }));
        c.getChildren().add(opcionMenu(Movimiento.reducido() ? "Reducir movimiento: activado" : "Reducir movimiento: desactivado", false, () -> {
            boolean nuevo = !Movimiento.reducido();
            Movimiento.reducidoProperty().set(nuevo);
            ajustes.cambiar(a -> a.setReducirMovimiento(nuevo));
            notificador.info(nuevo ? "Reducir movimiento activado." : "Reducir movimiento desactivado.");
        }));
        c.getChildren().add(opcionMenu("Restablecer datos de demostración", false, this::restablecerDemo));
        c.getChildren().add(opcionMenu("Cerrar sesión", true, this::cerrarSesion));
        notificador.popover(c, btnUsuario);
    }

    private void restablecerDemo() {
        notificador.confirmar("¿Restablecer datos de demostración?",
                "Se borrarán las reservas, solicitudes y cambios que hiciste en esta aplicación.", "Restablecer", true, () -> {
                    demo.restablecer();
                    idioma.setIdioma(ajustes.obtener().getIdioma());
                    actualizarCabecera();
                    refrescarActual();
                    notificador.exito("Datos restablecidos.");
                });
    }

    private void cerrarSesion() {
        notificador.confirmar("¿Cerrar sesión?", "Volverás a la pantalla de ingreso. Tus datos de demostración se mantienen.",
                "Cerrar sesión", false, () -> {
                    sesion.cerrar();
                    arrancarSesion();
                });
    }
}
