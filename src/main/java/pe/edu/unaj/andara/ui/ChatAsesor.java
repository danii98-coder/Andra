package pe.edu.unaj.andara.ui;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pe.edu.unaj.andara.exception.NegocioException;
import pe.edu.unaj.andara.model.*;
import pe.edu.unaj.andara.service.ConsultaAsesorService;
import pe.edu.unaj.andara.util.Fechas;

@Component
@RequiredArgsConstructor
public class ChatAsesor {

    private final Notificador notificador;
    private final ConsultaAsesorService consultas;

    public void abrir(Expediente s) {
        ConsultaAsesor inicial;
        try {
            inicial = consultas.abrir(s.getCodigo());
        } catch (NegocioException ex) {
            notificador.error(ex.getMessage());
            return;
        }
        VBox log = new VBox(8);
        log.getStyleClass().add("chat-log");
        ScrollPane sp = new ScrollPane(log);
        sp.setFitToWidth(true);
        sp.setPrefViewportHeight(280);
        sp.getStyleClass().add("chat-scroll");
        Label cupo = Componentes.etiqueta("", "pill");
        cupo.setMinWidth(Region.USE_PREF_SIZE);
        TextField entrada = new TextField();
        Componentes.prompt(entrada, "Escribe tu consulta sobre este trámite…");
        Componentes.accesible(entrada, "Consulta al asesor");
        HBox.setHgrow(entrada, Priority.ALWAYS);
        Button enviar = Componentes.boton("Enviar", "btn-primary", null, null);
        Button ampliar = Componentes.bloque(Componentes.boton("Solicitar ampliar consulta", "btn-secondary", "fth-plus-circle", null));
        Label ayudaAmpliar = Componentes.parrafo("", "hint");

        Runnable[] pintar = new Runnable[1];
        ConsultaAsesor[] actual = {inicial};
        pintar[0] = () -> {
            ConsultaAsesor c = actual[0];
            log.getChildren().clear();
            for (int i = 0; i < c.getMensajes().size(); i++) {
                MensajeAsesor m = c.getMensajes().get(i);
                log.getChildren().add(i == 0 ? contexto(m) : burbuja(m));
            }
            int quedan = c.restantes();
            Componentes.fijar(cupo, quedan + " de " + c.getCupo() + " consultas disponibles");
            cupo.getStyleClass().removeAll("success", "warning", "error");
            cupo.getStyleClass().add(quedan == 0 ? "error" : quedan == 1 ? "warning" : "success");
            boolean pendiente = c.getAmpliacion() == EstadoAmpliacion.PENDIENTE;
            entrada.setDisable(quedan == 0);
            enviar.setDisable(quedan == 0);
            Componentes.prompt(entrada, quedan == 0 ? (pendiente ? "Esperando la evaluación del asesor…" : "Agotaste tus consultas")
                    : "Escribe tu consulta sobre este trámite…");
            ampliar.setDisable(quedan > 0 || c.getAmpliacion() != EstadoAmpliacion.NINGUNA);
            Componentes.fijar(ayudaAmpliar, switch (c.getAmpliacion()) {
                case PENDIENTE -> "Solicitud enviada. El asesor está evaluando si es necesario ampliar tu consulta.";
                case APROBADA -> "El asesor aprobó la ampliación: se sumaron 3 consultas adicionales.";
                case RECHAZADA -> "El asesor no consideró necesaria la ampliación.";
                default -> quedan > 0 ? "Disponible al agotar tus 3 consultas. El asesor decide si otorga 3 oportunidades más."
                        : "Pide 3 oportunidades más; el asesor evaluará si es necesario.";
            });
            Platform.runLater(() -> sp.setVvalue(1));
        };

        Runnable mandar = () -> {
            String t = entrada.getText() == null ? "" : entrada.getText().trim();
            if (t.isEmpty()) {
                return;
            }
            try {
                actual[0] = consultas.preguntar(s.getCodigo(), t);
                entrada.clear();
                pintar[0].run();
                if (!entrada.isDisabled()) {
                    Platform.runLater(entrada::requestFocus);
                }
            } catch (NegocioException ex) {
                notificador.error(ex.getMessage());
            }
        };
        enviar.setOnAction(e -> mandar.run());
        entrada.setOnAction(e -> mandar.run());
        ampliar.setOnAction(e -> {
            try {
                actual[0] = consultas.solicitarAmpliacion(s.getCodigo());
                pintar[0].run();
                PauseTransition p = new PauseTransition(Duration.millis(1500));
                p.setOnFinished(x -> {
                    actual[0] = consultas.evaluarAmpliacion(s.getCodigo());
                    pintar[0].run();
                    if (actual[0].getAmpliacion() == EstadoAmpliacion.APROBADA) {
                        notificador.exito("El asesor amplió tu consulta: 3 oportunidades más.");
                    }
                });
                p.play();
            } catch (NegocioException ex) {
                notificador.error(ex.getMessage());
            }
        });

        pintar[0].run();
        HBox cabecera = new HBox(8, Componentes.pill("Asesor en línea", "success"), Componentes.espacio(), cupo);
        cabecera.setAlignment(Pos.CENTER_LEFT);
        HBox fila = new HBox(entrada, enviar);
        fila.getStyleClass().add("copy-row");
        VBox cuerpo = new VBox(10, cabecera, sp, fila, new Separator(), ampliar, ayudaAmpliar,
                Componentes.parrafo("Simulación: las respuestas del asesor son automáticas de demostración.", "hint"));
        notificador.modal("Consulta en línea · " + s.getCodigo(), cuerpo, false);
        if (!entrada.isDisabled()) {
            Platform.runLater(entrada::requestFocus);
        }
    }

    private VBox contexto(MensajeAsesor m) {
        VBox v = new VBox(4, Componentes.mayus("Contexto enviado automáticamente", "label-caps"),
                Componentes.literalParrafo(m.getTexto(), "small"));
        v.getStyleClass().add("wa-msg");
        v.getStyleClass().add("mail");
        return v;
    }

    private HBox burbuja(MensajeAsesor m) {
        Label l = Componentes.literalParrafo(m.getTexto(), "msg", m.isDeAsesor() ? "op" : "me");
        l.setMaxWidth(380);
        VBox v = new VBox(2, l, Componentes.etiqueta((m.isDeAsesor() ? "Asesor · " : "Tú · ") + Fechas.hm(m.getEnviado()), "hint"));
        v.setAlignment(m.isDeAsesor() ? Pos.CENTER_LEFT : Pos.CENTER_RIGHT);
        HBox h = new HBox(v);
        h.setAlignment(m.isDeAsesor() ? Pos.CENTER_LEFT : Pos.CENTER_RIGHT);
        return h;
    }
}
