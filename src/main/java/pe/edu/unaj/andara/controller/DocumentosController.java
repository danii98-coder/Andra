package pe.edu.unaj.andara.controller;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import lombok.RequiredArgsConstructor;
import org.kordamp.ikonli.javafx.FontIcon;
import org.springframework.stereotype.Component;
import pe.edu.unaj.andara.model.Documento;
import pe.edu.unaj.andara.model.EscuelaProfesional;
import pe.edu.unaj.andara.model.Tramite;
import pe.edu.unaj.andara.service.*;
import pe.edu.unaj.andara.ui.*;
import pe.edu.unaj.andara.util.Montos;

import java.time.LocalDate;
import java.util.*;

@Component
@RequiredArgsConstructor
public class DocumentosController extends VistaBase {

    private final ContextoService contexto;
    private final CatalogoService catalogo;
    private final DocumentoService documentos;
    private final AccionesDocumento accionesDocumento;
    private final AccionesPago accionesPago;

    private final EstadoFiltro filtros = new EstadoFiltro();
    private DocumentoService.Criterio aplicado = DocumentoService.Criterio.vacio();
    private String orden = "rel";
    private String publicoTramite = "all";
    private boolean todosTramites;
    private final Map<String, Boolean> mas = new HashMap<>();
    private TextField busqueda;

    @Override
    public void enfocarBusqueda() {
        if (busqueda != null) {
            Componentes.desplazarHasta(desplazable, busqueda);
            busqueda.requestFocus();
        }
    }

    @Override
    protected List<Node> construir() {
        List<Node> nodos = new ArrayList<>();
        nodos.add(Componentes.hero(null, "Documentos", "Solicita constancias y otros documentos académicos. Realiza el seguimiento de tus solicitudes.",
                Componentes.lineaContexto(null, contexto.textoEscuela(), "ctx:escuela")));
        nodos.add(tramites());
        nodos.add(buscador());
        nodos.add(resultados());
        nodos.add(misDocumentos());
        return nodos;
    }

    private VBox tramites() {
        String[][] publicos = {{"all", "Todos"}, {"Estudiante", "Estudiantes"}, {"Egresado", "Egresados"}, {"Docente", "Docentes"},
                {"Administrativo", "Administrativos"}, {"Público en general", "Público en general"}};
        FlowPane chips = Componentes.chips();
        ToggleGroup g = new ToggleGroup();
        for (String[] p : publicos) {
            ToggleButton b = new ToggleButton();
            Componentes.fijar(b, p[1]);
            b.getStyleClass().add("chip");
            b.setToggleGroup(g);
            b.setSelected(p[0].equals(publicoTramite));
            b.setOnAction(e -> {
                publicoTramite = p[0];
                todosTramites = false;
                redibujar();
            });
            chips.getChildren().add(b);
        }
        Componentes.accesible(chips, "Para quién");
        VBox.setMargin(chips, new Insets(0, 0, 16, 0));
        List<Tramite> lista = catalogo.tramites().stream().filter(t -> "all".equals(publicoTramite) || t.admite(publicoTramite)).toList();
        List<Tramite> visibles = todosTramites ? lista : lista.subList(0, Math.min(8, lista.size()));
        Rejilla tarjetas = new Rejilla(4, 230, 14);
        for (Tramite t : visibles) {
            HBox arriba = new HBox(Componentes.mayus(t.getCategoria(), "label-caps"), Componentes.espacio(),
                    Componentes.literal(Montos.soles(t.getMonto()), "tc-price"));
            arriba.setAlignment(Pos.CENTER_LEFT);
            VBox tarjeta = new VBox(8, arriba);
            if (t.getUnidad() != null) {
                Label u = Componentes.etiqueta(t.getUnidad(), "hint");
                HBox fila = new HBox(Componentes.espacio(), u);
                tarjeta.getChildren().add(fila);
            }
            Label entrega = Componentes.etiqueta(t.getTextoEntrega());
            entrega.setGraphic(new FontIcon("fth-clock"));
            Label quien = Componentes.etiqueta(t.getListaPublicos().size() > 3 ? "Varios solicitantes" : String.join(", ", t.getListaPublicos()));
            quien.setGraphic(new FontIcon("fth-user"));
            quien.setWrapText(true);
            VBox meta = new VBox(4, entrega, quien);
            meta.getStyleClass().add("tc-meta");
            Button solicitar = Componentes.bloque(Componentes.botonSm("Solicitar", "btn-primary", null, e -> accionesPago.iniciar(t, null, this::redibujar)));
            tarjeta.getChildren().addAll(Componentes.parrafo(t.getNombre(), "h3", "sm"), Componentes.parrafo(t.getDescripcion(), "muted", "small"),
                    meta, Componentes.espacioV(), solicitar);
            tarjeta.getStyleClass().add("tupa-card");
            tarjetas.getChildren().add(tarjeta);
        }
        Label eyebrow = Componentes.etiqueta("Con Andara Pay", "pay-eyebrow");
        HBox cab = Componentes.cabeceraSeccion("Solicita un documento oficial",
                "Se genera con tus datos, lo pagas con Yape, Plin o tarjeta y, por lo general, lo recoges el mismo día.");
        VBox panel = new VBox(eyebrow, cab, chips, tarjetas);
        if (lista.size() > 8) {
            Button masBtn = Componentes.botonSm(todosTramites ? "Mostrar menos" : "Ver los " + lista.size() + " trámites", "btn-secondary", null, e -> {
                todosTramites = !todosTramites;
                redibujar();
            });
            HBox centro = new HBox(masBtn);
            centro.setAlignment(Pos.CENTER);
            centro.setPadding(new Insets(16, 0, 0, 0));
            panel.getChildren().add(centro);
        }
        Label nota = Componentes.etiqueta("Montos referenciales de demostración.", "hint");
        VBox.setMargin(nota, new Insets(14, 0, 0, 0));
        panel.getChildren().add(nota);
        panel.getStyleClass().add("tupa-panel");
        VBox s = Componentes.seccion(panel);
        s.getStyleClass().add("sin-arriba");
        return s;
    }

    private VBox buscador() {
        EscuelaProfesional fac = contexto.escuela();
        busqueda = new TextField(aplicado.texto());
        Componentes.prompt(busqueda, "Busca formatos, resoluciones, sílabos o palabras clave…");
        Componentes.accesible(busqueda, "Buscar documentos");
        StackPane caja = new StackPane(busqueda, Componentes.icono("fth-search"));
        StackPane.setAlignment(caja.getChildren().get(1), Pos.CENTER_LEFT);
        StackPane.setMargin(caja.getChildren().get(1), new Insets(0, 0, 0, 14));
        caja.getStyleClass().addAll("search", "search-lg");
        Formulario f = new Formulario();
        Campo<ComboBox<Opcion>> tipo = f.agregar(Formulario.selector("Tipo de documento", Opcion.lista(catalogo.tiposDocumento()), "Todos los tipos", aplicado.tipo()));
        List<Opcion> deps = new ArrayList<>();
        deps.add(Opcion.grupo("Escuelas profesionales"));
        catalogo.escuelas().forEach(e -> deps.add(Opcion.de(e.getNombre(), e.getNombre())));
        deps.add(Opcion.grupo("Dependencias administrativas"));
        catalogo.dependencias().forEach(d -> deps.add(Opcion.de(d, d)));
        Campo<ComboBox<Opcion>> dep = f.agregar(Formulario.selector("Dependencia / Escuela profesional", deps,
                fac != null ? "EP " + fac.getNombre() + " e institucionales" : "Todas las dependencias", aplicado.dependencia()));
        Campo<ComboBox<Opcion>> publico = f.agregar(Formulario.selector("Público objetivo", Opcion.lista(DocumentoService.PUBLICOS), "Todo el público", aplicado.publico()));
        Campo<ComboBox<Opcion>> rango = f.agregar(Formulario.selector("Rango de fechas", Opcion.pares(DocumentoService.RANGOS), null, aplicado.rango()));
        Campo<DatePicker> desde = f.agregar(Formulario.fecha("Desde", aplicado.desde(), null, pe.edu.unaj.andara.util.Fechas.hoy()));
        Campo<DatePicker> hasta = f.agregar(Formulario.fecha("Hasta", aplicado.hasta(), null, pe.edu.unaj.andara.util.Fechas.hoy()));
        GridPane personalizado = Formulario.rejilla(2, desde.nodo(), hasta.nodo());
        personalizado.setVisible("custom".equals(aplicado.rango()));
        personalizado.managedProperty().bind(personalizado.visibleProperty());
        rango.control().valueProperty().addListener((o, a, b) -> personalizado.setVisible(b != null && "custom".equals(b.valor())));
        FlowPane rapidos = Componentes.chips();
        for (String t : DocumentoService.TIPOS_RAPIDOS) {
            ToggleButton b = new ToggleButton();
            Componentes.fijar(b, t);
            b.getStyleClass().add("chip");
            b.setSelected(t.equals(aplicado.tipo()));
            b.setOnAction(e -> {
                aplicado = aplicado.conTipo(t.equals(aplicado.tipo()) ? "" : t);
                mas.clear();
                redibujar();
            });
            rapidos.getChildren().add(b);
        }
        Componentes.accesible(rapidos, "Accesos rápidos por tipo");
        int n = documentos.buscar(aplicado, fac, filtros, orden).size();
        Label conteo = Componentes.etiqueta(String.format("%02d", n) + " documento" + (n == 1 ? "" : "s") + " encontrado" + (n == 1 ? "" : "s"), "hint");
        HBox.setHgrow(conteo, Priority.ALWAYS);
        conteo.setMaxWidth(Double.MAX_VALUE);
        Button limpiar = Componentes.boton("Limpiar", "btn-tertiary", null, e -> {
            aplicado = DocumentoService.Criterio.vacio();
            mas.clear();
            redibujar();
        });
        Button buscar = Componentes.boton("Buscar documento", "btn-primary", null, e -> {
            DocumentoService.Criterio c = new DocumentoService.Criterio(busqueda.getText().trim(), Formulario.valor(tipo.control()),
                    Formulario.valor(dep.control()), Formulario.valor(publico.control()), Formulario.valor(rango.control()),
                    desde.control().getValue(), hasta.control().getValue());
            String err = DocumentoService.validarRangoPersonalizado(c);
            if (err != null) {
                hasta.mostrar(err);
                hasta.control().requestFocus();
                return;
            }
            aplicado = c;
            mas.clear();
            redibujar();
        });
        busqueda.setOnAction(e -> buscar.fire());
        HBox pie = new HBox(conteo, limpiar, buscar);
        pie.getStyleClass().add("form-foot");
        VBox panel = new VBox(16, Componentes.parrafo("Buscador de documentos", "h2"), caja,
                Formulario.rejilla(2, tipo.nodo(), dep.nodo(), publico.nodo(), rango.nodo()), personalizado, rapidos, pie);
        panel.getStyleClass().add("panel");
        return Componentes.seccion(panel);
    }

    private VBox resultados() {
        EscuelaProfesional fac = contexto.escuela();
        List<Documento> l = documentos.buscar(aplicado, fac, filtros, orden);
        MenuButton ordenar = new MenuButton();
        Componentes.fijar(ordenar, "Ordenar por: " + DocumentoService.etiquetaOrden(orden));
        ordenar.setGraphic(new FontIcon("fth-bar-chart-2"));
        ordenar.getStyleClass().addAll("btn", "btn-secondary", "btn-sm");
        ToggleGroup grupoOrden = new ToggleGroup();
        for (String[] o : DocumentoService.ORDENES) {
            RadioMenuItem item = new RadioMenuItem();
            item.textProperty().bind(IdiomaService.enlace(o[1]));
            item.setToggleGroup(grupoOrden);
            item.setSelected(o[0].equals(orden));
            item.setOnAction(e -> {
                orden = o[0];
                redibujar();
            });
            ordenar.getItems().add(item);
        }
        HBox cab = Componentes.cabeceraSeccion("Resultado(s)", null, Filtros.boton(filtros, DocumentoService.PRESETS, null, notificador, this::redibujar), ordenar);
        VBox s = Componentes.seccion(cab, Filtros.resumen(filtros, DocumentoService.PRESETS, null, this::redibujar));
        if (l.isEmpty()) {
            s.getChildren().add(Componentes.vacio("fth-search", "No encontramos documentos", "Revisa la ortografía o quita algunos filtros."));
            return s;
        }
        boolean porEscuela = fac != null && (aplicado.dependencia() == null || aplicado.dependencia().isBlank());
        List<Documento> deEscuela = porEscuela ? l.stream().filter(d -> d.getEscuela() != null && d.getEscuela().getCodigo().equals(fac.getCodigo())).toList() : List.of();
        List<Documento> frecuentes = l.stream().filter(Documento::isFrecuente).filter(d -> !deEscuela.contains(d)).toList();
        List<Documento> generales = l.stream().filter(d -> !d.isFrecuente()).filter(d -> !deEscuela.contains(d)).toList();
        VBox grupos = new VBox(32);
        if (porEscuela) {
            agregarGrupo(grupos, "fac", "EP " + fac.getNombre(), deEscuela);
        }
        agregarGrupo(grupos, "freq", "Frecuente", frecuentes);
        agregarGrupo(grupos, "gen", porEscuela ? "Institucionales y generales" : "General", generales);
        s.getChildren().add(grupos);
        return s;
    }

    private void agregarGrupo(VBox destino, String clave, String titulo, List<Documento> lista) {
        if (lista.isEmpty()) {
            return;
        }
        int limite = 6;
        boolean abierto = mas.getOrDefault(clave, false);
        Label t = Componentes.mayus(titulo, "label-caps");
        Label c = Componentes.literal(" (" + lista.size() + ")", "hint");
        HBox cab = new HBox(t, c);
        cab.setAlignment(Pos.CENTER_LEFT);
        Rejilla g = new Rejilla(2, 360);
        (abierto ? lista : lista.subList(0, Math.min(limite, lista.size()))).forEach(d -> g.getChildren().add(accionesDocumento.tarjeta(d, this::redibujar)));
        VBox grupo = new VBox(12, cab, g);
        grupo.getStyleClass().add("fav-group");
        if (lista.size() > limite) {
            Button b = Componentes.botonSm(abierto ? "Mostrar menos" : "Mostrar " + (lista.size() - limite) + " más", "btn-secondary", null, e -> {
                mas.put(clave, !abierto);
                redibujar();
            });
            HBox centro = new HBox(b);
            centro.setAlignment(Pos.CENTER);
            grupo.getChildren().add(centro);
        }
        destino.getChildren().add(grupo);
    }

    private VBox misDocumentos() {
        List<Node> filas = new ArrayList<>();
        documentos.misDocumentos().forEach(d -> filas.add(accionesDocumento.filaPersonal(d, this::redibujar)));
        return Componentes.seccion(Componentes.cabeceraSeccion("Mis documentos", "Documentos emitidos a tu nombre"), Componentes.lista(filas));
    }
}
