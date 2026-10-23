package pe.edu.unaj.andara.controller;

import javafx.animation.PauseTransition;
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

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class SolicitudesController extends VistaBase {

    private static final int POR_PAGINA = 5;

    private final ExpedienteService expedientes;
    private final AccionesExpediente acciones;
    private final ChatAsesor chat;

    private final EstadoFiltro filtros = new EstadoFiltro();
    private String grupo = "all";
    private int pagina = 1;
    private String abierta;
    private TextField busqueda;
    private HBox barra;

    @Override
    public void enfocarBusqueda() {
        if (busqueda != null) {
            Componentes.desplazarHasta(desplazable, busqueda);
            busqueda.requestFocus();
        }
    }

    private TextField busqueda() {
        if (busqueda == null) {
            busqueda = new TextField();
            Componentes.prompt(busqueda, "Busca por código, trámite o fecha…");
            Componentes.accesible(busqueda, "Buscar solicitudes");
            PauseTransition espera = new PauseTransition(Duration.millis(250));
            espera.setOnFinished(e -> {
                pagina = 1;
                int caret = busqueda.getCaretPosition();
                redibujar();
                Platform.runLater(() -> {
                    busqueda.requestFocus();
                    busqueda.positionCaret(caret);
                });
            });
            busqueda.textProperty().addListener((o, a, b) -> espera.playFromStart());
        }
        return busqueda;
    }

    @Override
    protected List<Node> construir() {
        List<Expediente> todos = expedientes.mios();
        String q = busqueda().getText() == null ? "" : busqueda.getText().trim();
        List<Expediente> lista = todos.stream().filter(s -> "all".equals(grupo) || s.getGrupo().getClave().equals(grupo))
                .filter(s -> pe.edu.unaj.andara.util.TextoPlano.contiene(ExpedienteService.textoBusqueda(s), q)).toList();
        lista = FiltroService.aplicar(lista, filtros, ExpedienteService.predicados(), s -> s.getCodigo() + " " + s.getTitulo() + " " + s.getEstado());
        int paginas = Math.max(1, (int) Math.ceil(lista.size() / (double) POR_PAGINA));
        pagina = Math.min(pagina, paginas);
        List<Expediente> visibles = lista.subList(Math.min((pagina - 1) * POR_PAGINA, lista.size()), Math.min(pagina * POR_PAGINA, lista.size()));
        Expediente observado = todos.stream().filter(s -> s.getGrupo() == GrupoExpediente.OBSERVADO).findFirst().orElse(null);

        List<Node> nodos = new ArrayList<>();
        nodos.add(Componentes.hero(null, "Mis Solicitudes", "Rastrea el estado de tus expedientes, descarga constancias firmadas digitalmente y gestiona tus requerimientos universitarios.", null));
        StackPane buscador = new StackPane(busqueda(), Componentes.icono("fth-search"));
        StackPane.setAlignment(buscador.getChildren().get(1), Pos.CENTER_LEFT);
        StackPane.setMargin(buscador.getChildren().get(1), new Insets(0, 0, 0, 14));
        buscador.getStyleClass().add("search");
        HBox.setHgrow(buscador, Priority.ALWAYS);
        barra = new HBox(12, Componentes.pillPlano(todos.size() + " registros", "neutral"), buscador,
                Filtros.boton(filtros, ExpedienteService.PRESETS, null, notificador, this::redibujar),
                Componentes.boton("Nueva solicitud", "btn-primary", "fth-plus", e -> acciones.nueva(this::redibujar)));
        barra.setAlignment(Pos.CENTER_LEFT);
        VBox cuerpo = new VBox(20, barra, Filtros.resumen(filtros, ExpedienteService.PRESETS, null, this::redibujar), pestanas(todos), kpis(todos, observado));
        if (observado != null) {
            cuerpo.getChildren().add(alerta(observado));
        }
        VBox tarjetas = new VBox(16);
        if (visibles.isEmpty()) {
            tarjetas.getChildren().add(Componentes.vacio("fth-search", "No hay expedientes que coincidan", "Prueba con otro código o cambia el filtro."));
        }
        visibles.forEach(s -> tarjetas.getChildren().add(tarjeta(s)));
        cuerpo.getChildren().addAll(tarjetas, Paginador.crear(pagina, paginas, lista.size(), POR_PAGINA, "expedientes", p -> {
            pagina = p;
            refrescar();
            Platform.runLater(() -> Componentes.desplazarHasta(desplazable, barra));
        }));
        VBox s1 = Componentes.seccion(cuerpo);
        s1.getStyleClass().add("sin-arriba");
        nodos.add(s1);
        nodos.add(oficinas());
        return nodos;
    }

    private FlowPane pestanas(List<Expediente> todos) {
        FlowPane chips = Componentes.chips();
        ToggleGroup g = new ToggleGroup();
        String[][] grupos = {{"all", "Todos los trámites", null}, {"tramite", "En trámite", "-color-warning"},
                {"listo", "Emitidos / Listos", "-color-success"}, {"observado", "Observados", "-color-error"}};
        for (String[] gr : grupos) {
            long n = "all".equals(gr[0]) ? todos.size() : todos.stream().filter(s -> s.getGrupo().getClave().equals(gr[0])).count();
            ToggleButton b = new ToggleButton();
            Componentes.fijar(b, gr[1] + " (" + n + ")");
            if (gr[2] != null) {
                Circle c = new Circle(3.5);
                c.setStyle("-fx-fill: " + gr[2] + ";");
                b.setGraphic(c);
            }
            b.getStyleClass().add("chip");
            b.setToggleGroup(g);
            b.setSelected(gr[0].equals(grupo));
            b.setOnAction(e -> {
                grupo = gr[0];
                pagina = 1;
                redibujar();
            });
            chips.getChildren().add(b);
        }
        Componentes.accesible(chips, "Filtrar trámites");
        return chips;
    }

    private Rejilla kpis(List<Expediente> todos, Expediente observado) {
        long tramite = ExpedienteService.contar(todos, GrupoExpediente.TRAMITE);
        long listo = ExpedienteService.contar(todos, GrupoExpediente.LISTO);
        long obs = ExpedienteService.contar(todos, GrupoExpediente.OBSERVADO);
        Rejilla g = new Rejilla(4, 200, 16);
        g.getChildren().addAll(Componentes.kpi("Total expedientes", String.valueOf(todos.size()), null, "Semestre Académico 2026-1"),
                Componentes.kpi("En evaluación", String.valueOf(tramite), "warning", "Plazo de respuesta: 48 hrs"),
                Componentes.kpi("Emitidos y listos", String.valueOf(listo), "success", "Disponibles con firma digital"),
                Componentes.kpi("Acción requerida", String.valueOf(obs), obs > 0 ? "error" : null, observado != null ? "Subsanación por descanso médico" : "Sin pendientes"));
        return g;
    }

    private HBox alerta(Expediente s) {
        VBox textos = new VBox(2, Componentes.parrafo("Atención requerida en Expediente " + s.getCodigo() + " (" + s.getTitulo() + ")", "alerta-titulo"),
                Componentes.parrafo(s.getObservacion() != null && !s.getObservacion().contains("Servicio Médico UNAJ (Bienestar") ? "Motivo: " + s.getObservacion()
                        : "Motivo de Jefatura: Falta visado del Servicio Médico UNAJ (Edificio Administrativo). Plazo límite de vencimiento: 36 horas.", "small"));
        HBox.setHgrow(textos, Priority.ALWAYS);
        HBox a = new HBox(new FontIcon("fth-alert-triangle"), textos,
                Componentes.botonSm("Subsanar ahora", "btn-primary", null, e -> acciones.subsanar(s.getCodigo(), this::redibujar)));
        a.getStyleClass().add("alerta");
        return a;
    }

    private VBox tarjeta(Expediente s) {
        FlowPane meta = new FlowPane();
        meta.getStyleClass().add("exp-meta");
        for (DatoExpediente d : s.getDatos()) {
            meta.getChildren().add(new VBox(2, Componentes.mayus(d.getEtiqueta(), "label-caps"), Componentes.parrafo(d.getValor(), "v")));
        }
        VBox codigo = new VBox(2, Componentes.mayus("Expediente N.°", "label-caps"), Componentes.literal(s.getCodigo(), "v", "codigo"));
        meta.getChildren().add(codigo);
        VBox icono = new VBox(AccionesExpediente.iconoExpediente(s.getIcono()), Componentes.etiqueta(s.getRotuloIcono(), "rotulo"));
        icono.getStyleClass().add("exp-icon");
        if ("error".equals(s.getTono())) {
            icono.getStyleClass().add("error");
        }
        VBox principal = new VBox(new HBox(Componentes.pill(s.getEstado(), s.getTono())), Componentes.parrafo(s.getTitulo(), "h3"));
        principal.getStyleClass().add("exp-main");
        if (s.getDescripcion() != null) {
            principal.getChildren().add(Componentes.parrafo(s.getDescripcion(), "exp-desc"));
        }
        if (!s.getEtiquetas().isEmpty()) {
            FlowPane tags = Componentes.chips();
            s.getEtiquetas().forEach(t -> tags.getChildren().add(Componentes.tag(t)));
            principal.getChildren().add(tags);
        }
        principal.getChildren().add(RastreadorCjsv.mini(SeguimientoService.calcular(s)));
        if (s.getObservacion() != null && s.getGrupo() == GrupoExpediente.OBSERVADO && s.getCodigo().equals(abierta)) {
            VBox obs = new VBox(Componentes.parrafo("Motivo de observación emitido por Jefatura Académica:", "obs-titulo"),
                    Componentes.parrafo("“" + s.getObservacion() + "”", "obs-texto"));
            obs.getStyleClass().add("obs");
            principal.getChildren().add(obs);
        }
        if (s.getNota() != null && !s.getNota().isBlank()) {
            principal.getChildren().add(Componentes.parrafo(s.getNota(), "hint"));
        }
        HBox.setHgrow(principal, Priority.ALWAYS);
        VBox botones = new VBox();
        botones.getStyleClass().add("exp-acts");
        for (AccionExpediente a : s.getAcciones()) {
            String estilo = switch (a.getEstilo()) {
                case "primary" -> "btn-primary";
                case "secondary" -> "btn-secondary";
                case "danger" -> "btn-danger";
                default -> "btn-tertiary";
            };
            String texto = "obs-sol".equals(a.getAccion()) && s.getCodigo().equals(abierta) ? "Ocultar observaciones" : a.getTexto();
            Button b = Componentes.bloque(Componentes.botonSm(texto, estilo, "download-sol".equals(a.getAccion()) ? "fth-download" : null,
                    e -> acciones.ejecutar(a.getAccion(), s, this::redibujar, () -> {
                        abierta = s.getCodigo().equals(abierta) ? null : s.getCodigo();
                        redibujar();
                    })));
            botones.getChildren().add(b);
        }
        if (ConsultaAsesorService.disponible(s)) {
            botones.getChildren().add(Componentes.bloque(Componentes.botonSm("Consulta en línea con asesor", "btn-secondary", "fth-message-circle",
                    e -> chat.abrir(s))));
        }
        HBox cuerpo = new HBox(icono, principal, botones);
        cuerpo.getStyleClass().add("exp-body");
        VBox tarjeta = new VBox(meta, cuerpo);
        tarjeta.getStyleClass().add("exp");
        if (s.isDestacado()) {
            tarjeta.getStyleClass().add("flag");
        }
        return tarjeta;
    }

    private VBox oficinas() {
        VBox mesa = new VBox(12, Componentes.parrafo("Mesa de Partes Virtual UNAJ", "h3"),
                Componentes.parrafo("Atención de lunes a viernes de 08:30 a 18:00 hrs. Los documentos ingresados fuera de horario se registran automáticamente en el siguiente día hábil institucional.", "muted", "small"),
                new HBox(Componentes.botonSm("Iniciar consulta de expediente", "btn-secondary", null, e -> enfocarBusqueda())));
        mesa.getStyleClass().add("office");
        VBox plazos = new VBox(12, Componentes.parrafo("Plazos oficiales de tramitación", "h3"),
                Componentes.vineta("Constancias automáticas con código QR: emisión inmediata dentro de 24 horas.", "muted", "small"),
                Componentes.vineta("Certificados oficiales con firma SUNEDU / Reniec: de 3 a 5 días hábiles.", "muted", "small"),
                Componentes.vineta("Justificaciones médicas y permisos extraordinarios: evaluación en 48 horas útiles.", "muted", "small"));
        plazos.getStyleClass().add("office");
        Rejilla g = new Rejilla(2, 320);
        g.getChildren().addAll(mesa, plazos);
        return Componentes.seccion(g);
    }
}
