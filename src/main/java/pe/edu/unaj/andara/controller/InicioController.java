package pe.edu.unaj.andara.controller;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import lombok.RequiredArgsConstructor;
import org.kordamp.ikonli.javafx.FontIcon;
import org.springframework.stereotype.Component;
import pe.edu.unaj.andara.model.Espacio;
import pe.edu.unaj.andara.model.Pago;
import pe.edu.unaj.andara.service.*;
import pe.edu.unaj.andara.ui.*;
import pe.edu.unaj.andara.util.Fechas;
import pe.edu.unaj.andara.util.Montos;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class InicioController extends VistaBase {

    private static final List<Carrusel.Datos> DIAPOSITIVAS = List.of(
            new Carrusel.Datos("Bienvenida", "Todo lo que necesitas de tu universidad, en un solo lugar",
                    "Trámites, reservas y préstamos con seguimiento en tiempo real.", "/img/carrusel/01-bienvenida-campus.jpg",
                    "Portada de ingreso al campus de la UNAJ", 0.5, 0.45),
            new Carrusel.Datos("Documentos", "Constancias y certificados con firma digital",
                    "Descárgalos con código QR y validez ante SUNEDU.", "/img/carrusel/02-documentos-firma.jpg",
                    "Firma de convenio institucional con documentos oficiales", 0.5, 0.35),
            new Carrusel.Datos("Reservas", "Aulas, laboratorios y auditorios disponibles hoy",
                    "Consulta horarios libres y reserva en segundos.", "/img/carrusel/03-reservas-laboratorio.jpg",
                    "Estudiante trabajando en un laboratorio de cómputo", 0.35, 0.4),
            new Carrusel.Datos("Equipos", "Préstamo de sonido, instrumentos, deportes y tecnología",
                    "Revisa el inventario y el punto de entrega.", "/img/carrusel/04-equipos-tuna.jpg",
                    "Tuna femenina universitaria tocando guitarras y bandurrias con su estandarte", 0.5, 0.4),
            new Carrusel.Datos("Mis Solicitudes", "Sigue cada expediente paso a paso",
                    "Recibe alertas cuando un trámite requiera tu acción.", "/img/carrusel/05-solicitudes-archivo.jpg",
                    "Archivo de expedientes ordenados por número", 0.7, 0.5));

    private final ContextoService contexto;
    private final CatalogoService catalogo;
    private final ReservaService reservas;
    private final PagoService pagos;
    private final OcupacionService ocupacion;

    private Carrusel carrusel;
    private String espacioElegido;
    private int dia;
    private String franja;

    @Override
    public void salir() {
        if (carrusel != null) {
            carrusel.detener();
        }
    }

    @Override
    protected List<Node> construir() {
        if (carrusel != null) {
            carrusel.detener();
        }
        List<Node> nodos = new ArrayList<>();
        carrusel = new Carrusel(DIAPOSITIVAS);
        Componentes.accesible(carrusel, "Novedades");
        VBox arriba = Componentes.seccion(carrusel);
        arriba.setPadding(new Insets(32, 24, 0, 24));
        Label intro = Componentes.parrafo("Solicita documentos, reserva aulas y laboratorios, gestiona préstamos de equipos y consulta tus solicitudes de manera rápida y sencilla.", "intro");
        intro.setAlignment(Pos.CENTER);
        VBox.setMargin(intro, new Insets(32, 0, 0, 0));
        HBox ctas = new HBox(Componentes.boton("Explorar reservas", "btn-primary", null, e -> ir("reservas")),
                Componentes.boton("Ver mis solicitudes", "btn-secondary", null, e -> ir("solicitudes")));
        ctas.getStyleClass().add("hero-ctas");
        VBox.setMargin(ctas, new Insets(24, 0, 0, 0));
        arriba.getChildren().addAll(intro, ctas);
        arriba.setAlignment(Pos.TOP_CENTER);
        List<Pago> pendientes = pagos.pendientes();
        if (!pendientes.isEmpty()) {
            int n = pendientes.size();
            String s = n > 1 ? "s" : "";
            Text marca = Componentes.texto("Andara Pay", "strong");
            marca.setStyle("-fx-font-weight: 700;");
            Text resto = Componentes.texto(" · Tienes " + n + " pago" + s + " pendiente" + s + ", incluida tu matrícula "
                    + pagos.matricula().getPeriodo() + ".");
            javafx.scene.text.TextFlow flujo = Componentes.flujo(marca, resto);
            HBox.setHgrow(flujo, Priority.ALWAYS);
            HBox banner = new HBox(flujo, Componentes.literal(Montos.soles(PagoService.total(pendientes)), "strong"), new FontIcon("fth-arrow-right"));
            banner.getStyleClass().add("pay-banner");
            banner.setFocusTraversable(true);
            banner.setOnMouseClicked(e -> ir("pay"));
            Componentes.accesible(banner, "Ir a Andara Pay");
            VBox.setMargin(banner, new Insets(24, 0, 0, 0));
            arriba.getChildren().add(banner);
        }
        OcupacionService.FranjaBnps fr = ocupacion.franjaActual();
        List<Espacio> enContexto = espaciosContexto();
        long libres = ocupacion.libres(enContexto, fr);
        HBox carne = rapido(Componentes.icono("fth-credit-card"), "Mi carné digital", () -> ir("carne"));
        Circle punto = new Circle(5);
        punto.getStyleClass().addAll("live-dot", "on");
        HBox mapa = rapido(punto, libres + " espacios libres " + (fr.enVivo() ? "ahora" : "en la próxima franja"), () -> ir("mapa"));
        mapa.getChildren().add(new FontIcon("fth-arrow-right"));
        HBox fila = new HBox(12, carne, mapa);
        fila.setAlignment(Pos.CENTER);
        VBox.setMargin(fila, new Insets(14, 0, 0, 0));
        arriba.getChildren().add(fila);
        nodos.add(arriba);
        nodos.add(servicios());
        nodos.add(buscador(enContexto));
        return nodos;
    }

    private HBox rapido(Node icono, String texto, Runnable accion) {
        HBox h = new HBox(icono, Componentes.etiqueta(texto));
        h.getStyleClass().add("quick");
        h.setFocusTraversable(true);
        h.setOnMouseClicked(e -> accion.run());
        h.setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ENTER) {
                accion.run();
            }
        });
        return h;
    }

    private List<Espacio> espaciosContexto() {
        return catalogo.espacios().stream().filter(e -> contexto.incluye(e.getSede(), e.getEscuela())).toList();
    }

    private VBox servicios() {
        Label titulo = Componentes.parrafo("¿Qué necesitas realizar?", "title-1");
        titulo.setAlignment(Pos.CENTER);
        titulo.setMaxWidth(Double.MAX_VALUE);
        HBox cab = new HBox(titulo);
        cab.setAlignment(Pos.CENTER);
        cab.setPadding(new Insets(0, 0, 24, 0));
        Rejilla g = new Rejilla(2, 320);
        String[][] datos = {
                {"fth-file-text", "Solicitar documento", "Solicita constancias y documentos académicos y realiza seguimiento de tu solicitud.", "Solicitar ahora", "documentos"},
                {"fth-calendar", "Reservar espacio", "Consulta la disponibilidad y reserva aulas, laboratorios y otros espacios universitarios.", "Ver disponibilidad", "reservas"},
                {"fth-box", "Solicitar equipo", "Consulta equipos disponibles y gestiona préstamos de laptops, proyectores y otros recursos.", "Ver equipos", "equipos"},
                {"fth-list", "Mis solicitudes", "Revisa el estado y el historial de tus solicitudes.", "Consultar", "solicitudes"}};
        for (String[] d : datos) {
            StackPane icono = new StackPane(new FontIcon(d[0]));
            icono.getStyleClass().add("sicon");
            Button b = Componentes.botonSm(d[3], "btn-secondary", "fth-arrow-right", e -> ir(d[4]));
            b.setContentDisplay(ContentDisplay.RIGHT);
            VBox tarjeta = new VBox(12, icono, Componentes.parrafo(d[1], "h3"), Componentes.parrafo(d[2], "service-texto"), Componentes.espacioV(), new HBox(b));
            tarjeta.getStyleClass().add("service");
            g.getChildren().add(tarjeta);
        }
        VBox s = Componentes.seccion(cab, g);
        s.setPadding(new Insets(64, 24, 32, 24));
        return s;
    }

    private VBox buscador(List<Espacio> espacios) {
        if (espacios.stream().noneMatch(e -> e.getCodigo().equals(espacioElegido))) {
            espacioElegido = espacios.isEmpty() ? null : espacios.get(0).getCodigo();
            franja = null;
        }
        LocalDate fecha = Fechas.hoy().plusDays(dia);
        List<Opcion> opciones = new ArrayList<>();
        espacios.forEach(e -> opciones.add(Opcion.de(e.getCodigo(), e.getNombre() + (e.getEscuela() == null ? " (uso general)" : ""))));
        Campo<ComboBox<Opcion>> selector = Formulario.selector("Espacio · Sede " + contexto.nombreSede(), opciones, null, espacioElegido);
        selector.control().valueProperty().addListener((o, a, b) -> {
            if (b != null && !b.valor().equals(espacioElegido)) {
                espacioElegido = b.valor();
                franja = null;
                redibujar();
            }
        });
        Button cambiar = Componentes.enlaceFlecha("Cambiar sede o escuela", e -> ir("ctx:sede"));
        cambiar.setGraphic(null);
        HBox meta = new HBox(6, Componentes.icono("fth-book-open"), Componentes.parrafo(contexto.escuela() != null
                ? "Mostrando espacios de EP " + contexto.escuela().getNombre() + " y de uso general." : "Mostrando todos los espacios de la sede.", "hint"), cambiar);
        meta.getStyleClass().add("meta-line");
        meta.setAlignment(Pos.CENTER_LEFT);
        Segmentado dias = new Segmentado(false).opcion("0", "Hoy", null, null).opcion("1", "Mañana", null, null)
                .opcion("2", "Pasado mañana", null, null).opcion("cal", dia > 2 ? Fechas.corto(fecha) : "Otra fecha", "fth-calendar", null)
                .seleccionar(dia > 2 ? "cal" : String.valueOf(dia));
        dias.alCambiar(v -> {
            if ("cal".equals(v)) {
                Calendario.abrir(notificador, fecha, f -> {
                    dia = (int) ChronoUnit.DAYS.between(Fechas.hoy(), f);
                    franja = null;
                    redibujar();
                });
                dias.seleccionar(dia > 2 ? "cal" : String.valueOf(dia));
                return;
            }
            dia = Integer.parseInt(v);
            franja = null;
            redibujar();
        });
        VBox campoDia = new VBox(6, Componentes.etiqueta("Día", "flabel"), dias, Componentes.etiqueta(Fechas.largo(fecha), "hint"));
        campoDia.getStyleClass().add("field");
        VBox izquierda = new VBox(16, new VBox(6, Componentes.parrafo("Buscar espacio", "h2"),
                Componentes.parrafo("Elige un espacio y un día para ver los horarios libres. Selecciona un horario disponible para reservarlo.", "muted", "small")),
                selector.nodo(), meta, campoDia);
        Espacio elegido = espacios.stream().filter(e -> e.getCodigo().equals(espacioElegido)).findFirst().orElse(null);
        VBox slots = new VBox();
        slots.getStyleClass().add("slots");
        for (String t : CatalogoService.FRANJAS) {
            boolean ok = elegido != null && reservas.libreEnInicio(elegido, dia, t);
            String fin = String.format("%02d:00", Integer.parseInt(t.substring(0, 2)) + 2);
            Label hora = Componentes.literal(t + " – " + fin, "slot-time");
            HBox contenido = new HBox(hora, Componentes.espacio(), Componentes.pill(ok ? "Disponible" : "Ocupado"));
            contenido.setAlignment(Pos.CENTER_LEFT);
            Button b = new Button();
            b.setGraphic(contenido);
            b.getStyleClass().add("slot");
            b.setMaxWidth(Double.MAX_VALUE);
            contenido.prefWidthProperty().bind(b.widthProperty().subtract(34));
            b.setDisable(!ok);
            Componentes.accesible(b, t + ", " + (ok ? "disponible" : "ocupado"));
            if (t.equals(franja)) {
                b.getStyleClass().add("on");
            }
            b.setOnAction(e -> {
                franja = t.equals(franja) ? null : t;
                redibujar();
            });
            slots.getChildren().add(b);
        }
        String finFranja = franja == null ? "" : String.format("%02d:00", Integer.parseInt(franja.substring(0, 2)) + 2);
        Button reservar = Componentes.bloque(Componentes.boton(franja == null ? "Reservar espacio" : "Reservar " + franja + " – " + finFranja,
                "btn-primary", null, e -> confirmar(elegido, fecha)));
        reservar.setDisable(franja == null || elegido == null);
        VBox.setMargin(reservar, new Insets(16, 0, 0, 0));
        VBox derecha = new VBox(slots, reservar);
        GridPane panel = new GridPane();
        panel.setHgap(48);
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setPercentWidth(50);
        ColumnConstraints c2 = new ColumnConstraints();
        c2.setPercentWidth(50);
        panel.getColumnConstraints().addAll(c1, c2);
        panel.add(izquierda, 0, 0);
        panel.add(derecha, 1, 0);
        GridPane.setValignment(izquierda, javafx.geometry.VPos.TOP);
        VBox caja = new VBox(panel);
        caja.getStyleClass().add("panel");
        return Componentes.seccion(caja);
    }

    private void confirmar(Espacio e, LocalDate fecha) {
        if (e == null || franja == null) {
            return;
        }
        String horario = CatalogoService.HORARIOS.get(CatalogoService.FRANJAS.indexOf(franja));
        notificador.confirmar("Confirmar reserva", e.getNombre() + " · " + Fechas.largo(fecha) + " · " + horario
                + ". La solicitud quedará pendiente de aprobación y podrás verla en Reservas.", "Confirmar reserva", false, () -> {
            if (intentar(() -> reservas.reservaRapida(e, fecha, franja))) {
                franja = null;
                redibujar();
                notificador.exito("Reserva enviada. Puedes verla en Reservas.");
            }
        });
    }
}
