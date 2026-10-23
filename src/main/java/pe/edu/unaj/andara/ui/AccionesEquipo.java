package pe.edu.unaj.andara.ui;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pe.edu.unaj.andara.exception.NegocioException;
import pe.edu.unaj.andara.model.Equipo;
import pe.edu.unaj.andara.model.Prestamo;
import pe.edu.unaj.andara.model.TipoFavorito;
import pe.edu.unaj.andara.service.EquipoService;
import pe.edu.unaj.andara.service.FavoritoService;
import pe.edu.unaj.andara.util.Fechas;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class AccionesEquipo {

    private final Notificador notificador;
    private final EquipoService equipos;
    private final FavoritoService favoritos;

    public Label estado(Equipo e, boolean largo) {
        if (e.isPorTurno()) {
            return Componentes.pill("Próximo turno: " + e.getProximoTurno(), "warning");
        }
        if (e.getStock() > 0) {
            return Componentes.pill(largo ? "Disponible (" + e.getStock() + " " + e.getUnidad() + ")" : e.getStock() + " " + e.getUnidad(), "success");
        }
        return Componentes.pill("Agotado", "neutral");
    }

    public TarjetaGaleria tarjeta(Equipo e, int indice, Runnable alCambiar, LocalDate fechaUso, String duracion) {
        Node[] giro = new Node[1];
        StackPane arte = Ilustraciones.equipo(e, giro);
        boolean fav = favoritos.es(TipoFavorito.EQUIPO, e.getCodigo());
        Button estrella = Componentes.estrella(fav, e.getNombre(), () -> {
            boolean ahora = favoritos.alternar(TipoFavorito.EQUIPO, e.getCodigo());
            notificador.exito(ahora ? "Agregado a favoritos." : "Quitado de favoritos.");
            alCambiar.run();
        });
        return new TarjetaGaleria(arte, giro[0], e.getRotulo(), estado(e, false), e.getNombre(),
                Componentes.metaLinea("fth-map-pin", "Sede " + e.getSede().getNombre() + " · " + e.getUbicacion()),
                Componentes.etiqueta(e.getEscuela() != null ? "EP " + e.getEscuela().getNombre() : "Uso general", "hint"),
                false, null, estrella, "Ver detalles de " + e.getNombre(), () -> detalle(e, alCambiar, fechaUso, duracion), indice);
    }

    public void detalle(Equipo e, Runnable alCambiar, LocalDate fechaUso, String duracion) {
        boolean fav = favoritos.es(TipoFavorito.EQUIPO, e.getCodigo());
        Node[] giro = new Node[1];
        StackPane arte = Ilustraciones.equipo(e, giro);
        arte.getStyleClass().add("det-art");
        arte.setPrefHeight(240);
        arte.setMinHeight(200);
        VBox incluye = new VBox(6);
        for (String x : e.getEspecificaciones().split("•")) {
            Label l = Componentes.literalParrafo(x.trim(), "small");
            l.setGraphic(Componentes.icono("fth-check", "azul"));
            incluye.getChildren().add(l);
        }
        VBox izquierda = new VBox(12, Componentes.chips(estado(e, true), Componentes.tag(e.getRotulo())),
                Componentes.mayus("Incluye", "label-caps"), incluye);
        GridPane kv = Componentes.kv("Sede", e.getSede().getNombreCompleto(), "Ubicación", Componentes.literalParrafo(e.getUbicacion(), "dd"),
                "Punto de entrega", Componentes.literalParrafo(e.getPuntoEntrega(), "dd"),
                "Uso", e.getEscuela() != null ? "Preferente para EP " + e.getEscuela().getNombre() : "General",
                "Préstamo", "Hasta 4 horas lectivas, renovable una vez");
        VBox cuerpo = new VBox(18, arte, izquierda, kv);
        Button favorito = Componentes.boton(fav ? "Quitar de favoritos" : "Agregar a favoritos", "btn-secondary", "fth-star", ev -> {
            boolean ahora = favoritos.alternar(TipoFavorito.EQUIPO, e.getCodigo());
            notificador.cerrarModal();
            notificador.exito(ahora ? "Agregado a favoritos." : "Quitado de favoritos.");
            alCambiar.run();
        });
        Button pedir = Componentes.boton(e.isPorTurno() ? "Reservar turno" : "Solicitar préstamo", "btn-primary", null,
                ev -> solicitar(e, alCambiar, fechaUso, duracion));
        pedir.setDisable(!e.isPorTurno() && e.getStock() <= 0);
        notificador.modal(e.getNombre(), cuerpo, true, favorito, pedir);
    }

    public void solicitar(Equipo e, Runnable alCambiar, LocalDate fechaUso, String duracion) {
        boolean turno = e.isPorTurno();
        Formulario f = new Formulario();
        Campo<DatePicker> fecha = f.agregar(Formulario.fecha("Fecha de uso", fechaUso != null ? fechaUso : Fechas.hoy(), Fechas.hoy(), null))
                .requerido("Elige la fecha.");
        Campo<ComboBox<Opcion>> hora = f.agregar(turno
                ? Formulario.selector("Turno", Opcion.lista(List.of("16:00 - 18:00", "18:00 - 20:00")), "Seleccionar", null).requerido("Elige un turno.")
                : Formulario.selector("Hora de retiro", Opcion.lista(List.of("08:00", "10:00", "12:00", "14:00", "16:00")), "Seleccionar", null)
                .requerido("Elige la hora de retiro."));
        List<Opcion> duraciones = new ArrayList<>();
        EquipoService.DURACIONES.forEach((k, v) -> duraciones.add(Opcion.de(k, v)));
        Campo<ComboBox<Opcion>> dur = f.agregar(Formulario.selector("Duración", duraciones, null, duracion == null ? "4h" : duracion));
        Campo<TextArea> motivo = f.agregar(Formulario.area("Motivo de uso", "Ej. Ensayo del elenco universitario"))
                .requerido("Describe para qué usarás el equipo.").minimo(10);
        Campo<CheckBox> acepto = f.agregar(Formulario.casilla("Acepto el reglamento de préstamos y la penalización por devolución tardía o deterioro."))
                .regla(c -> c.isSelected() ? null : "Debes aceptar el reglamento para continuar.");
        VBox cuerpo = new VBox(14, Componentes.parrafo(turno ? "Este equipo está en uso. Puedes reservar el siguiente turno libre. Sede "
                + e.getSede().getNombre() + "." : "Retiro en: Sede " + e.getSede().getNombre() + " · " + e.getUbicacion() + ".", "muted", "small"),
                Formulario.rejilla(2, fecha.nodo(), hora.nodo()), dur.nodo(), motivo.nodo(), acepto.nodo());
        Button enviar = Componentes.boton(turno ? "Reservar turno" : "Enviar solicitud", "btn-primary", null, null);
        enviar.setOnAction(ev -> {
            if (!f.validar()) {
                return;
            }
            try {
                Prestamo p = equipos.solicitar(e, fecha.control().getValue(), Formulario.valor(hora.control()),
                        Formulario.valor(dur.control()), motivo.control().getText(), acepto.control().isSelected());
                notificador.cerrarModal();
                alCambiar.run();
                notificador.exito("Solicitud " + p.getCodigo() + " registrada. Estado: En revisión.");
            } catch (NegocioException ex) {
                notificador.error(ex.getMessage());
            }
        });
        notificador.modal(turno ? "Reservar turno: " + e.getNombre() : "Solicitar préstamo: " + e.getNombre(), cuerpo, false,
                Componentes.boton("Cancelar", "btn-secondary", null, ev -> notificador.cerrarModal()), enviar);
    }
}
