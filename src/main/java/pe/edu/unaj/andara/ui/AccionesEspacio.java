package pe.edu.unaj.andara.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import lombok.RequiredArgsConstructor;
import org.kordamp.ikonli.javafx.FontIcon;
import org.springframework.stereotype.Component;
import pe.edu.unaj.andara.exception.NegocioException;
import pe.edu.unaj.andara.model.Espacio;
import pe.edu.unaj.andara.model.TipoEspacio;
import pe.edu.unaj.andara.model.TipoFavorito;
import pe.edu.unaj.andara.service.CatalogoService;
import pe.edu.unaj.andara.service.FavoritoService;
import pe.edu.unaj.andara.service.ReservaService;
import pe.edu.unaj.andara.util.Fechas;
import pe.edu.unaj.andara.util.Montos;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class AccionesEspacio {

    public record Precarga(LocalDate fecha, String horario, Integer asistentes) {
        public static final Precarga VACIA = new Precarga(null, null, null);
    }

    private static final Map<TipoEspacio, List<String>> EQUIPAMIENTO = Map.of(
            TipoEspacio.AUDITORIO, List.of("Sistema de sonido profesional", "Proyector láser y pantalla", "Escenario con iluminación",
                    "Butacas numeradas", "Acceso para silla de ruedas"),
            TipoEspacio.LABORATORIO, List.of("Estaciones de trabajo", "Proyector y pizarra", "Internet de alta velocidad",
                    "Software especializado de la escuela"),
            TipoEspacio.AULA, List.of("Proyector multimedia", "Pizarra acrílica", "Carpetas individuales", "Wi-Fi"),
            TipoEspacio.SALA, List.of("Mesa de reuniones", "Pantalla de 65\"", "Videoconferencia", "Pizarra"),
            TipoEspacio.TALLER, List.of("Mesas de trabajo", "Equipos y herramientas de la especialidad",
                    "Equipo de protección personal", "Supervisión de técnico"));

    private final Notificador notificador;
    private final ReservaService reservas;
    private final FavoritoService favoritos;
    private final CatalogoService catalogo;

    public TarjetaGaleria tarjeta(Espacio s, String estado, int indice, boolean destacado, Precarga precarga, Runnable alCambiar) {
        Node[] giro = new Node[1];
        Pane arte = Ilustraciones.marcoEspacio(s, giro);
        boolean fav = favoritos.es(TipoFavorito.ESPACIO, s.getCodigo());
        Button estrella = Componentes.estrella(fav, s.getNombre(), () -> {
            boolean ahora = favoritos.alternar(TipoFavorito.ESPACIO, s.getCodigo());
            notificador.exito(ahora ? "Agregado a favoritos." : "Quitado de favoritos.");
            alCambiar.run();
        });
        Label cap = Componentes.literal(Montos.miles(s.getCapacidad()), "tile-cap");
        cap.setGraphic(new FontIcon("fth-user"));
        return new TarjetaGaleria(arte, giro[0], s.getTipo().getEtiqueta(), Componentes.pill(estado), s.getNombre(),
                Componentes.metaLinea("fth-map-pin", "Sede " + s.getSede().getNombre() + " · " + s.getEdificio()), cap,
                destacado, destacado ? "Auditorio principal" : null, estrella, "Ver detalles de " + s.getNombre(),
                () -> detalle(s, precarga, alCambiar), indice);
    }

    public void detalle(Espacio s, Precarga precarga, Runnable alCambiar) {
        String estado = s.isFueraDeServicio() ? "Ocupado" : "Disponible";
        boolean fav = favoritos.es(TipoFavorito.ESPACIO, s.getCodigo());
        Node[] giro = new Node[1];
        Pane arte = Ilustraciones.marcoEspacio(s, giro);
        StackPane det = new StackPane(arte);
        det.getStyleClass().add("det-art");
        det.setPrefHeight(280);
        det.setMinHeight(220);
        javafx.scene.shape.Rectangle r = new javafx.scene.shape.Rectangle();
        r.setArcWidth(28);
        r.setArcHeight(28);
        r.widthProperty().bind(det.widthProperty());
        r.heightProperty().bind(det.heightProperty());
        det.setClip(r);

        FlowPane chips = Componentes.chips(Componentes.pill(estado), Componentes.tag(s.getTipo().getEtiqueta()),
                Componentes.tag(s.getEscuela() != null ? "EP " + s.getEscuela().getNombre() : "Uso general"));
        GridPane kv = Componentes.kv("Sede", Componentes.literalParrafo(s.getSede().getNombreCompleto(), "dd"),
                "Ubicación", Componentes.literalParrafo(s.getEdificio(), "dd"),
                "Capacidad", Montos.miles(s.getCapacidad()) + " personas");
        VBox equip = new VBox(6);
        for (String a : EQUIPAMIENTO.getOrDefault(s.getTipo(), List.of())) {
            Label l = Componentes.parrafo(a, "small");
            l.setGraphic(Componentes.icono("fth-check", "azul"));
            equip.getChildren().add(l);
        }
        VBox izquierda = new VBox(12, chips, kv, Componentes.mayus("Equipamiento", "label-caps"), equip);
        HBox cajitas = new HBox(6);
        for (String t : CatalogoService.FRANJAS) {
            boolean ok = "Disponible".equals(estado) && reservas.libreEnInicio(s, 0, t);
            VBox caja = new VBox(2, Componentes.literal(t, "strong"), Componentes.etiqueta(ok ? "Libre" : "Ocupado", "small"));
            caja.setAlignment(Pos.CENTER);
            caja.setPadding(new Insets(8, 10, 8, 10));
            caja.getStyleClass().add("room");
            caja.getStyleClass().add(ok ? "free" : "busy");
            caja.setStyle("-fx-min-height: 0;");
            HBox.setHgrow(caja, Priority.ALWAYS);
            caja.setMaxWidth(Double.MAX_VALUE);
            cajitas.getChildren().add(caja);
        }
        VBox derecha = new VBox(10, Componentes.mayus("Horarios de hoy", "label-caps"), cajitas,
                Componentes.parrafo(s.getTipo() == TipoEspacio.AUDITORIO ? "Los auditorios requieren aprobación de Secretaría General."
                        : "La reserva queda pendiente hasta la aprobación del responsable.", "hint"));
        VBox cuerpo = new VBox(18, det, izquierda, derecha);
        Button favorito = Componentes.boton(fav ? "Quitar de favoritos" : "Agregar a favoritos", "btn-secondary", "fth-star", e -> {
            boolean ahora = favoritos.alternar(TipoFavorito.ESPACIO, s.getCodigo());
            notificador.cerrarModal();
            notificador.exito(ahora ? "Agregado a favoritos." : "Quitado de favoritos.");
            alCambiar.run();
        });
        Button reservar = Componentes.boton("Reservar este espacio", "btn-primary", null, e -> reservar(s, precarga, alCambiar));
        reservar.setDisable(!"Disponible".equals(estado));
        notificador.modal(s.getNombre(), cuerpo, true, favorito, reservar);
    }

    public void reservar(Espacio s, Precarga pre, Runnable alCambiar) {
        Precarga p = pre == null ? Precarga.VACIA : pre;
        Formulario f = new Formulario();
        Campo<DatePicker> fecha = f.agregar(Formulario.fecha("Fecha", p.fecha() != null ? p.fecha() : Fechas.hoy(), Fechas.hoy(), null))
                .requerido("Elige una fecha.").regla(c -> c.getValue() != null && c.getValue().isBefore(Fechas.hoy()) ? "Elige hoy o una fecha posterior." : null);
        Campo<ComboBox<Opcion>> horario = f.agregar(Formulario.selector("Horario", Opcion.lista(CatalogoService.HORARIOS), "Seleccionar",
                p.horario())).requerido("Elige un horario.");
        Campo<TextField> asistentes = f.agregar(Formulario.numero("Número de asistentes", null,
                p.asistentes() == null ? "" : String.valueOf(p.asistentes()), 5)).requerido("Indica cuántas personas asistirán.")
                .regla(c -> {
                    String t = c.getText().trim();
                    if (t.isEmpty()) {
                        return null;
                    }
                    int n = Integer.parseInt(t);
                    if (n > s.getCapacidad()) {
                        return "El máximo permitido es " + Montos.miles(s.getCapacidad()) + ".";
                    }
                    return n < 1 ? "El mínimo es 1." : null;
                });
        Campo<TextArea> motivo = f.agregar(Formulario.area("Motivo de la reserva", "Ej. Práctica calificada del curso de Redes"))
                .requerido("Describe el motivo de la reserva.").minimo(10);
        VBox cuerpo = new VBox(14, Componentes.parrafo("Capacidad máxima: " + Montos.miles(s.getCapacidad())
                + " personas. La solicitud quedará pendiente hasta su aprobación.", "muted", "small"),
                Formulario.rejilla(2, fecha.nodo(), horario.nodo()), asistentes.nodo(), motivo.nodo());
        Button enviar = Componentes.boton("Enviar solicitud", "btn-primary", null, null);
        enviar.setOnAction(e -> {
            if (!f.validar()) {
                return;
            }
            try {
                reservas.reservar(s, fecha.control().getValue(), Formulario.valor(horario.control()),
                        Integer.parseInt(asistentes.control().getText().trim()), motivo.control().getText());
                notificador.cerrarModal();
                alCambiar.run();
                notificador.exito("Solicitud de reserva enviada. Estado: Pendiente.");
            } catch (NegocioException ex) {
                notificador.error(ex.getMessage());
            }
        });
        notificador.modal("Reservar " + s.getNombre(), cuerpo, false,
                Componentes.boton("Cancelar", "btn-secondary", null, e -> notificador.cerrarModal()), enviar);
    }
}
