package pe.edu.unaj.andara.controller;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.util.Duration;
import lombok.RequiredArgsConstructor;
import org.kordamp.ikonli.javafx.FontIcon;
import org.springframework.stereotype.Component;
import pe.edu.unaj.andara.model.*;
import pe.edu.unaj.andara.service.*;
import pe.edu.unaj.andara.ui.*;
import pe.edu.unaj.andara.util.Fechas;
import pe.edu.unaj.andara.util.Montos;

import java.time.LocalDate;
import java.util.*;
import java.util.function.Predicate;

@Component
@RequiredArgsConstructor
public class ReservasController extends VistaBase {

    private record Filtro(String tipo, Integer capacidad, LocalDate fecha, String horario) {
    }

    private static final List<String[]> PRESETS = List.of(new String[]{"free", "Disponibles"}, new String[]{"fav", "Mis favoritos"},
            new String[]{"big", "Capacidad 50+"}, new String[]{"gen", "Uso general"});

    private final ContextoService contexto;
    private final CatalogoService catalogo;
    private final ReservaService reservas;
    private final OcupacionService ocupacion;
    private final FavoritoService favoritos;
    private final AccionesEspacio accionesEspacio;
    private final ConexionService conexion;

    private final EstadoFiltro filtros = new EstadoFiltro();
    private Filtro filtro;
    private String categoria = "all";
    private String vistaEspacios = "gal";
    private String franjaMapa = "now";
    private VBox seccionEspacios;
    private VBox formulario;
    private Timeline refrescoMapa;

    @Override
    public void salir() {
        if (refrescoMapa != null) {
            refrescoMapa.stop();
        }
    }

    public void mostrarMapa() {
        vistaEspacios = "map";
        refrescar();
        Platform.runLater(() -> Componentes.desplazarHasta(desplazable, seccionEspacios));
    }

    @Override
    protected List<Node> construir() {
        salir();
        List<Node> nodos = new ArrayList<>();
        nodos.add(Componentes.hero("En este apartado podrás reservar espacios de la universidad", "Reservas",
                "Encuentra y reserva aulas, laboratorios y otros espacios universitarios.",
                Componentes.lineaContexto(contexto.textoSede(), contexto.textoEscuela(), "ctx:sede")));
        nodos.add(buscador());
        seccionEspacios = espacios();
        nodos.add(seccionEspacios);
        nodos.add(misReservas());
        if ("map".equals(vistaEspacios)) {
            refrescoMapa = new Timeline(new KeyFrame(Duration.seconds(60), e -> {
                if (!notificador.hayModal() && conexion.enLinea()) {
                    redibujar();
                }
            }));
            refrescoMapa.setCycleCount(Animation.INDEFINITE);
            refrescoMapa.play();
        }
        return nodos;
    }

    private List<Espacio> enContexto() {
        return catalogo.espacios().stream().filter(e -> contexto.incluye(e.getSede(), e.getEscuela())).toList();
    }

    private String estadoDe(Espacio s) {
        return reservas.estadoGaleria(s, filtro == null ? null : filtro.fecha(), filtro == null ? null : filtro.horario());
    }

    private VBox buscador() {
        List<Espacio> pool = enContexto();
        List<Opcion> tipos = new ArrayList<>();
        new LinkedHashSet<>(pool.stream().map(Espacio::getTipo).toList()).forEach(t -> tipos.add(Opcion.de(t.name(), t.getEtiqueta())));
        Formulario f = new Formulario();
        Campo<ComboBox<Opcion>> tipo = f.agregar(Formulario.selector("Tipo de espacio", tipos, "Todos los tipos", filtro != null ? filtro.tipo() : null));
        Campo<TextField> cap = f.agregar(Formulario.numero("Capacidad", "Número de personas",
                filtro != null && filtro.capacidad() != null ? String.valueOf(filtro.capacidad()) : "", 5));
        Campo<DatePicker> fecha = f.agregar(Formulario.fecha("Fecha", filtro != null ? filtro.fecha() : Fechas.hoy(), Fechas.hoy(), null))
                .requerido("Elige una fecha.").regla(c -> c.getValue() != null && c.getValue().isBefore(Fechas.hoy()) ? "Elige hoy o una fecha posterior." : null);
        Campo<ComboBox<Opcion>> hora = f.agregar(Formulario.selector("Horario", Opcion.lista(CatalogoService.HORARIOS), "Cualquier horario",
                filtro != null ? filtro.horario() : null));
        long coinciden = filtro == null ? 0 : pool.stream().filter(s -> (filtro.tipo() == null || s.getTipo().name().equals(filtro.tipo()))
                && (filtro.capacidad() == null || s.getCapacidad() >= filtro.capacidad())).count();
        Label resultado = Componentes.parrafo(filtro != null ? coinciden + " espacio" + (coinciden == 1 ? "" : "s") + " coinciden con tu búsqueda."
                : "Completa al menos la fecha para buscar.", "hint");
        HBox.setHgrow(resultado, Priority.ALWAYS);
        resultado.setMaxWidth(Double.MAX_VALUE);
        Button buscar = Componentes.boton("Buscar disponibilidad", "btn-primary", "fth-search", e -> {
            if (!f.validar()) {
                return;
            }
            String t = Formulario.valor(tipo.control());
            String c = cap.control().getText().trim();
            String h = Formulario.valor(hora.control());
            filtro = new Filtro(t.isBlank() ? null : t, c.isBlank() ? null : Integer.valueOf(c), fecha.control().getValue(), h.isBlank() ? null : h);
            categoria = t.isBlank() ? "all" : t;
            vistaEspacios = "gal";
            refrescar();
            Platform.runLater(() -> Componentes.desplazarHasta(desplazable, seccionEspacios));
        });
        buscar.setDefaultButton(false);
        HBox pie = new HBox(resultado);
        if (filtro != null) {
            pie.getChildren().add(Componentes.boton("Limpiar filtros", "btn-tertiary", null, e -> {
                filtro = null;
                refrescar();
            }));
        }
        pie.getChildren().add(buscar);
        pie.getStyleClass().add("form-foot");
        formulario = new VBox(Componentes.parrafo("Buscar disponibilidad", "h2"), Formulario.rejilla(2, tipo.nodo(), cap.nodo(), fecha.nodo(), hora.nodo()), pie);
        formulario.setSpacing(20);
        formulario.getStyleClass().add("panel");
        formulario.getProperties().put("tipo", tipo.control());
        VBox s = Componentes.seccion(formulario);
        s.getStyleClass().add("sin-arriba");
        return s;
    }

    private VBox espacios() {
        Segmentado vista = new Segmentado(false).opcion("gal", "Galería", "fth-image", null);
        Circle vivo = new Circle(4);
        vivo.getStyleClass().addAll("live-dot", "on");
        vista.opcionNodo("map", "Mapa en vivo", vivo).seleccionar(vistaEspacios);
        vista.alCambiar(v -> {
            vistaEspacios = v;
            redibujar();
        });
        if ("map".equals(vistaEspacios)) {
            return Componentes.seccion(Componentes.cabeceraSeccion("Espacios", "Mira qué está libre ahora mismo y toca un espacio para reservarlo.", vista), mapa());
        }
        List<Espacio> pool = enContexto().stream().filter(s -> filtro == null || filtro.capacidad() == null || s.getCapacidad() >= filtro.capacidad()).toList();
        List<Espacio> lista = pool.stream().filter(s -> "all".equals(categoria) || s.getTipo().name().equals(categoria)).toList();
        Set<String> fav = favoritos.codigos(TipoFavorito.ESPACIO);
        Map<String, Predicate<Espacio>> pred = Map.of("free", s -> "Disponible".equals(estadoDe(s)), "fav", s -> fav.contains(s.getCodigo()),
                "big", s -> s.getCapacidad() >= 50, "gen", s -> s.getEscuela() == null);
        lista = new ArrayList<>(FiltroService.aplicar(lista, filtros, pred, s -> s.getNombre() + " " + s.getEdificio() + " " + s.getTipo().getEtiqueta()
                + " " + (s.getEscuela() == null ? "Uso general" : s.getEscuela().getNombre())));
        Optional<Espacio> magno = lista.stream().filter(s -> "magno".equals(s.getCodigo())).findFirst();
        if (magno.isPresent()) {
            lista.remove(magno.get());
            lista.add(0, magno.get());
        }
        String sub = (filtro != null ? "Para el " + Fechas.dmy(filtro.fecha()) + (filtro.horario() != null ? ", " + filtro.horario() : "") + " · " : "")
                + "Sede " + contexto.nombreSede() + (contexto.escuela() != null ? " · EP " + contexto.escuela().getNombre() : "")
                + ". Pasa el cursor y elige uno para ver sus detalles.";
        Button btnFiltros = Filtros.boton(filtros, PRESETS, null, notificador, this::redibujar);
        HBox cab = Componentes.cabeceraSeccion("Espacios", sub, vista, btnFiltros);
        FlowPane pestanas = Componentes.chips();
        ToggleGroup grupo = new ToggleGroup();
        String[][] tabs = {{"all", "Todos"}, {"AULA", "Aulas"}, {"AUDITORIO", "Auditorios"}, {"LABORATORIO", "Laboratorios"}, {"SALA", "Salas"}, {"TALLER", "Talleres"}};
        for (String[] t : tabs) {
            long n = "all".equals(t[0]) ? pool.size() : pool.stream().filter(s -> s.getTipo().name().equals(t[0])).count();
            ToggleButton b = new ToggleButton();
            Componentes.fijar(b, t[1]);
            b.setGraphic(Componentes.literal(String.valueOf(n), "conteo"));
            b.setContentDisplay(ContentDisplay.RIGHT);
            b.getStyleClass().add("chip");
            b.setToggleGroup(grupo);
            b.setSelected(t[0].equals(categoria));
            b.setOnAction(e -> {
                categoria = t[0];
                if (filtro != null) {
                    filtro = new Filtro("all".equals(t[0]) ? null : t[0], filtro.capacidad(), filtro.fecha(), filtro.horario());
                }
                redibujar();
            });
            pestanas.getChildren().add(b);
        }
        VBox.setMargin(pestanas, new Insets(0, 0, 16, 0));
        Node resumen = Filtros.resumen(filtros, PRESETS, null, this::redibujar);
        Node contenido;
        if (lista.isEmpty()) {
            boolean auditorioFuera = "AUDITORIO".equals(categoria) && !"capilla".equals(contexto.codigoSede());
            contenido = Componentes.vacio("fth-search", "No hay espacios aquí", auditorioFuera
                    ? "Los auditorios están en la sede Capilla. Cambia de sede para reservarlos."
                    : "Prueba con otro tipo, quita filtros o cambia de sede.");
        } else {
            Galeria g = new Galeria();
            AccionesEspacio.Precarga pre = filtro == null ? AccionesEspacio.Precarga.VACIA
                    : new AccionesEspacio.Precarga(filtro.fecha(), filtro.horario(), filtro.capacidad());
            for (int i = 0; i < lista.size(); i++) {
                Espacio s = lista.get(i);
                boolean destacado = "magno".equals(s.getCodigo()) && ("all".equals(categoria) || "AUDITORIO".equals(categoria));
                g.getChildren().add(accionesEspacio.tarjeta(s, estadoDe(s), i, destacado, pre, this::redibujar));
            }
            contenido = g;
        }
        return Componentes.seccion(cab, pestanas, resumen, contenido);
    }

    private VBox mapa() {
        OcupacionService.FranjaBnps ahora = ocupacion.franjaActual();
        OcupacionService.FranjaBnps sel = "now".equals(franjaMapa) ? ahora
                : new OcupacionService.FranjaBnps(Integer.parseInt(franjaMapa), ahora.dia(), false);
        List<Espacio> pool = enContexto();
        Map<String, List<Espacio>> grupos = ocupacion.porPabellon(pool);
        Map<Espacio, OcupacionService.EstadoOcupacion> estados = new LinkedHashMap<>();
        pool.forEach(s -> estados.put(s, ocupacion.estado(s, sel.indice(), sel.dia())));
        long libres = estados.values().stream().filter(x -> x == OcupacionService.EstadoOcupacion.LIBRE).count();
        long pronto = estados.values().stream().filter(x -> x == OcupacionService.EstadoOcupacion.POR_TERMINAR).count();
        long ocupados = estados.values().stream().filter(x -> x == OcupacionService.EstadoOcupacion.OCUPADO).count();
        Circle punto = new Circle(5);
        punto.getStyleClass().add("live-dot");
        if ("now".equals(franjaMapa) && ahora.enVivo()) {
            punto.getStyleClass().add("on");
            if (!Movimiento.reducido()) {
                javafx.animation.FadeTransition pulso = new javafx.animation.FadeTransition(Duration.millis(900), punto);
                pulso.setFromValue(1);
                pulso.setToValue(0.35);
                pulso.setAutoReverse(true);
                pulso.setCycleCount(Animation.INDEFINITE);
                pulso.play();
            }
        }
        String etiqueta = "now".equals(franjaMapa) ? (ahora.enVivo() ? "Ahora" : "Próxima franja") : "Vista a las " + sel.inicio();
        Label lbl = Componentes.etiqueta(etiqueta, "strong");
        Label det = Componentes.etiqueta(" · " + contexto.textoSede() + " · " + sel.inicio() + "–" + sel.fin() + (sel.dia() > 0 ? " de mañana" : ""));
        HBox izquierda = new HBox(8, punto, lbl, det);
        izquierda.setAlignment(Pos.CENTER_LEFT);
        HBox conteos = new HBox(6, Componentes.etiqueta(libres + " libres", "mc", "free"), Componentes.etiqueta(pronto + " por terminar", "mc", "soon"),
                Componentes.etiqueta(ocupados + " ocupados", "mc", "busy"));
        HBox cab = new HBox(izquierda, Componentes.espacio(), conteos);
        cab.getStyleClass().add("map-head");
        VBox caja = new VBox(cab);
        caja.getStyleClass().add("map-wrap");
        if (!ahora.enVivo() && "now".equals(franjaMapa)) {
            caja.getChildren().add(Componentes.parrafo("Fuera del horario de atención (08:00–18:00). Te mostramos la primera franja "
                    + (ahora.dia() > 0 ? "de mañana" : "de hoy") + ".", "hint"));
        }
        FlowPane chips = Componentes.chips();
        ToggleGroup g = new ToggleGroup();
        ToggleButton ya = new ToggleButton();
        Componentes.fijar(ya, "Ahora");
        ya.setGraphic(new FontIcon("fth-clock"));
        ya.getStyleClass().add("chip");
        ya.setToggleGroup(g);
        ya.setSelected("now".equals(franjaMapa));
        ya.setOnAction(e -> {
            franjaMapa = "now";
            redibujar();
        });
        chips.getChildren().add(ya);
        for (int i = 0; i < CatalogoService.FRANJAS.size(); i++) {
            int idx = i;
            ToggleButton b = new ToggleButton(CatalogoService.FRANJAS.get(i));
            b.getStyleClass().add("chip");
            b.setToggleGroup(g);
            b.setSelected(String.valueOf(i).equals(franjaMapa));
            b.setOnAction(e -> {
                franjaMapa = String.valueOf(idx);
                redibujar();
            });
            chips.getChildren().add(b);
        }
        caja.getChildren().add(chips);
        Rejilla campus = new Rejilla(4, 260, 16);
        campus.getStyleClass().add("campus");
        for (Map.Entry<String, List<Espacio>> grupo : grupos.entrySet()) {
            HBox bh = new HBox(Componentes.icono("fth-home"), Componentes.literal(grupo.getKey(), "strong"), Componentes.espacio(),
                    Componentes.literal(String.valueOf(grupo.getValue().size()), "hint"));
            bh.getStyleClass().add("bld-h");
            Rejilla salas = new Rejilla(4, 110, 8);
            for (Espacio s : grupo.getValue()) {
                OcupacionService.EstadoOcupacion st = estados.get(s);
                String texto = st == OcupacionService.EstadoOcupacion.POR_TERMINAR ? "Libre hasta " + sel.fin() : st.getEtiqueta();
                Label cap = Componentes.literal(Montos.miles(s.getCapacidad()), "room-c");
                cap.setGraphic(new FontIcon("fth-user"));
                VBox sala = new VBox(2, Componentes.literalParrafo(OcupacionService.nombreCorto(s.getNombre()), "room-n"),
                        Componentes.etiqueta(texto, "room-s"), Componentes.espacioV(), cap);
                sala.getStyleClass().addAll("room", st.getClase());
                sala.setFocusTraversable(true);
                Componentes.accesible(sala, s.getNombre() + " · " + texto);
                Tooltip.install(sala, new Tooltip(s.getNombre() + " · " + IdiomaService.t(texto)));
                sala.setOnMouseClicked(e -> accionesEspacio.detalle(s, AccionesEspacio.Precarga.VACIA, this::redibujar));
                sala.setOnKeyPressed(e -> {
                    if (e.getCode() == javafx.scene.input.KeyCode.ENTER) {
                        accionesEspacio.detalle(s, AccionesEspacio.Precarga.VACIA, this::redibujar);
                    }
                });
                salas.getChildren().add(sala);
            }
            VBox bld = new VBox(bh, salas);
            bld.getStyleClass().add("bld");
            campus.getChildren().add(bld);
        }
        caja.getChildren().add(campus);
        HBox leyenda = new HBox(14, leyenda("free", "Libre"), leyenda("soon", "Libre hasta el fin de la franja"), leyenda("busy", "Ocupado"),
                Componentes.etiqueta("Plano esquemático · se actualiza cada minuto · toca un espacio para reservarlo", "hint"));
        leyenda.setAlignment(Pos.CENTER_LEFT);
        caja.getChildren().add(leyenda);
        return caja;
    }

    private HBox leyenda(String clase, String texto) {
        Region r = new Region();
        r.getStyleClass().addAll("lg", clase);
        HBox h = new HBox(6, r, Componentes.etiqueta(texto, "small"));
        h.setAlignment(Pos.CENTER_LEFT);
        return h;
    }

    private VBox misReservas() {
        List<Reserva> mias = reservas.mias();
        Button nueva = Componentes.botonSm("Reservar espacio", "btn-secondary", "fth-plus", e -> {
            Componentes.desplazarHasta(desplazable, formulario);
            Object tipo = formulario.getProperties().get("tipo");
            if (tipo instanceof Node n) {
                n.requestFocus();
            }
        });
        HBox cab = Componentes.cabeceraSeccion("Mis reservas", mias.size() + " reserva" + (mias.size() == 1 ? "" : "s") + " registradas", nueva);
        Node cuerpo;
        if (mias.isEmpty()) {
            cuerpo = Componentes.vacio("fth-calendar", "Aún no tienes reservas", "Busca un espacio disponible y resérvalo.");
        } else {
            List<Node> filas = new ArrayList<>();
            for (Reserva r : mias) {
                VBox textos = new VBox(2, Componentes.literalParrafo(r.getEspacio().getNombre(), "row-title"),
                        Componentes.etiqueta(Fechas.dmy(r.getFecha()) + " · " + Fechas.hora12(r.getInicio()) + " – " + Fechas.hora12(r.getFin())
                                + " · Sede " + r.getEspacio().getSede().getNombre(), "row-sub"));
                boolean activa = r.getEstado().isActiva();
                filas.add(Componentes.fila(textos, Componentes.pill(r.getEstado().getEtiqueta()),
                        Componentes.botonSm("Detalles", "btn-secondary", null, e -> detalle(r)),
                        Componentes.botonIcono("fth-trash-2", (activa ? "Cancelar" : "Eliminar") + " reserva " + r.getEspacio().getNombre(), e -> eliminar(r))));
            }
            cuerpo = Componentes.lista(filas);
        }
        return Componentes.seccion(cab, cuerpo);
    }

    private void detalle(Reserva r) {
        Espacio sp = r.getEspacio();
        Object[] kv = {"Espacio", Componentes.literalParrafo(sp.getNombre(), "dd"), "Sede", Componentes.literalParrafo(sp.getSede().getNombreCompleto() + " · " + sp.getEdificio(), "dd"),
                "Escuela", sp.getEscuela() != null ? "EP " + sp.getEscuela().getNombre() : "Uso general", "Fecha", Fechas.largo(r.getFecha()),
                "Horario", r.getHorario(), "Asistentes", String.valueOf(r.getAsistentes()), "Motivo", Componentes.parrafo(r.getMotivo(), "dd"),
                "Estado", Componentes.pill(r.getEstado().getEtiqueta()), r.getNota() != null ? "Observación" : null, r.getNota() == null ? "" : Componentes.parrafo(r.getNota(), "dd")};
        notificador.modal("Detalle de la reserva", Componentes.kv(kv), false, Componentes.boton("Cerrar", "btn-primary", null, e -> notificador.cerrarModal()));
    }

    private void eliminar(Reserva r) {
        boolean activa = r.getEstado().isActiva();
        notificador.confirmar(activa ? "¿Cancelar esta reserva?" : "¿Eliminar del historial?", activa
                        ? "Se liberará " + r.getEspacio().getNombre() + " el " + Fechas.dmy(r.getFecha()) + ". Recuerda cancelar con al menos 30 minutos de anticipación."
                        : "La reserva de " + r.getEspacio().getNombre() + " se quitará de tu lista.",
                activa ? "Cancelar reserva" : "Eliminar", true, () -> {
                    reservas.eliminar(r.getId());
                    redibujar();
                    notificador.exito(activa ? "Reserva cancelada." : "Reserva eliminada del historial.");
                });
    }
}
