package pe.edu.unaj.andara;

import atlantafx.base.theme.PrimerLight;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.text.Font;
import javafx.stage.Screen;
import javafx.stage.Stage;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import pe.edu.unaj.andara.controller.MainController;
import pe.edu.unaj.andara.model.Ajustes;
import pe.edu.unaj.andara.service.AjustesService;
import pe.edu.unaj.andara.service.ConexionService;
import pe.edu.unaj.andara.ui.Enlaces;

import java.io.InputStream;
import java.util.Locale;
import java.util.Objects;

public class AplicacionFx extends Application {

    private ConfigurableApplicationContext contexto;
    private MainController principal;

    @Override
    public void init() {
        Locale.setDefault(Locale.forLanguageTag("es-PE"));
        contexto = new SpringApplicationBuilder(AndaraApplication.class).web(WebApplicationType.NONE).run();
    }

    @Override
    public void start(Stage ventana) throws Exception {
        boolean mac = System.getProperty("os.name", "").toLowerCase().contains("mac");
        for (String peso : new String[]{"400", "500", "600"}) {
            try (InputStream in = getClass().getResourceAsStream("/fuentes/Inter-" + peso + ".ttf")) {
                if (in != null) {
                    Font.loadFont(in, 14);
                }
            }
        }
        Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());
        contexto.getBean(Enlaces.class).setHostServices(getHostServices());

        FXMLLoader cargador = new FXMLLoader(getClass().getResource("/view/main-view.fxml"));
        cargador.setControllerFactory(contexto::getBean);
        Parent raiz = cargador.load();
        if (!mac) {
            raiz.setStyle("-fx-font-family: 'Inter', 'Segoe UI', sans-serif;");
        }
        Ajustes a = contexto.getBean(AjustesService.class).obtener();
        double ancho = a.getVentanaAncho() != null ? a.getVentanaAncho() : 1280;
        double alto = a.getVentanaAlto() != null ? a.getVentanaAlto() : 800;
        Scene escena = new Scene(raiz, ancho, alto);
        escena.setCamera(new PerspectiveCamera());
        escena.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/css/theme.css")).toExternalForm());
        ventana.getIcons().add(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/img/logo-andara.png"))));
        ventana.setScene(escena);
        ventana.setMinWidth(1024);
        ventana.setMinHeight(680);
        if (a.getVentanaX() != null && a.getVentanaY() != null && dentroDePantalla(a.getVentanaX(), a.getVentanaY())) {
            ventana.setX(a.getVentanaX());
            ventana.setY(a.getVentanaY());
        }
        ventana.setMaximized(a.isMaximizada());
        principal = cargador.getController();
        principal.iniciar(ventana);
        contexto.getBean(ConexionService.class).iniciarMonitoreo();
        ventana.show();
    }

    private static boolean dentroDePantalla(double x, double y) {
        return Screen.getScreens().stream().anyMatch(s -> s.getVisualBounds().contains(x + 40, y + 20));
    }

    @Override
    public void stop() {
        if (principal != null) {
            principal.guardarVentana();
        }
        contexto.close();
        Platform.exit();
    }
}
