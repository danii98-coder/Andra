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
import javafx.util.Duration;
import lombok.RequiredArgsConstructor;
import org.kordamp.ikonli.javafx.FontIcon;
import org.springframework.stereotype.Component;
import pe.edu.unaj.andara.model.*;
import pe.edu.unaj.andara.service.*;
import pe.edu.unaj.andara.ui.*;
import pe.edu.unaj.andara.util.Fechas;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

@Component
@RequiredArgsConstructor
public class EquiposController extends VistaBase {

    private final ContextoService contexto;
    private final CatalogoService catalogo;
    private final EquipoService equipos;
    private final SesionService sesion;
    private final AccionesEquipo accionesEquipo;
    private final DocumentoGeneradoService generador;
    private final Descargas descargas;

    private final EstadoFiltro filtros = new EstadoFiltro();
    private String categoria = "";
    private String punto = "";
    private String chip = "all";
    private LocalDate fechaUso;
    private String duracion = "4h";
    private Timeline reloj;
    private VBox catalogoSeccion;

    @Override
    public void salir() {
        if (reloj != null) {
            reloj.stop();
        }
    }

    @Override
    protected List<Node> construir() {
        salir();
        if (fechaUso == null || fechaUso.isBefore(Fechas.hoy())) {
            fechaUso = Fechas.hoy();
        }
        List<Equipo> pool = equipos.enContexto();
        List<String> puntos = new ArrayList<>(new LinkedHashSet<>(pool.stream().map(Equipo::getPuntoEntrega).toList()));
        if (!punto.isBlank() && !puntos.contains(punto)) {
            punto = "";
        }
        List<Node> nodos = new ArrayList<>();
        nodos.add(Componentes.hero(null, "Equipos", "Solicita equipos propios de la universidad: sonido, instrumentos, implementos deportivos, tecnología e instrumental de laboratorio por escuela.",
                Componentes.lineaContexto(contexto.textoSede(), contexto.textoEscuela(), "ctx:sede")));
        nodos.add(categorias(pool));
        List<Equipo> base = pool.stream().filter(e -> categoria.isBlank() || e.getCategoria().getCodigo().equals(categoria))
                .filter(e -> punto.isBlank() || e.getPuntoEntrega().equals(punto)).toList();
        nodos.add(buscador(base, puntos));
        catalogoSeccion = catalogo(base);
        nodos.add(catalogoSeccion);
        nodos.add(activo());
        nodos.add(historial());
        return nodos;
    }

    private VBox categorias(List<Equipo> pool) {
        List<Node> acciones = new ArrayList<>();
        if (!categoria.isBlank()) {
            acciones.add(Componentes.botonSm("Ver todas", "btn-tertiary", null, e -> {
                categoria = "";
                redibujar();
            }));
        }
        HBox cab = Componentes.cabeceraSeccion("Explorar por categoría", null, acciones.toArray(new Node[0]));
        Rejilla g = new Rejilla(5, 170, 12);
        for (CategoriaEquipo c : catalogo.categorias()) {
            int n = equipos.disponiblesEnCategoria(pool, c);
            ToggleButton b = new ToggleButton();
            HBox arriba = new HBox(Componentes.icono(Ilustraciones.ICONOS_CATEGORIA.getOrDefault(c.getIcono(), "fth-box"), "cat-icono"), Componentes.espacio(),
                    Componentes.pillPlano(n + " disponibles", n > 0 ? "success" : "neutral"));
            arriba.setAlignment(Pos.CENTER_LEFT);
            VBox contenido = new VBox(8, arriba, Componentes.parrafo(c.getNombre(), "cat-nombre"), Componentes.parrafo(c.getDescripcion(), "muted", "small"));
            b.setGraphic(contenido);
            contenido.prefWidthProperty().bind(b.widthProperty().subtract(34));
            b.getStyleClass().add("cat-tile");
            b.setMaxWidth(Double.MAX_VALUE);
            b.setMaxHeight(Double.MAX_VALUE);
            b.setSelected(c.getCodigo().equals(categoria));
            b.setOnAction(e -> {
                categoria = c.getCodigo().equals(categoria) ? "" : c.getCodigo();
                redibujar();
            });
            g.getChildren().add(b);
        }
        VBox s = Componentes.seccion(cab, g);
        s.getStyleClass().add("sin-arriba");
        return s;
    }

    private VBox buscador(List<Equipo> base, List<String> puntos) {
        Formulario f = new Formulario();
        List<Opcion> cats = new ArrayList<>();
        catalogo.categorias().forEach(c -> cats.add(Opcion.de(c.getCodigo(), c.getNombre())));
        Campo<ComboBox<Opcion>> cat = f.agregar(Formulario.selector("Categoría o tipo de equipo", cats, "Todos los equipos", categoria));
        Campo<ComboBox<Opcion>> pto = f.agregar(Formulario.selector("Punto de entrega · Sede " + contexto.nombreSede(), Opcion.lista(puntos),
                "Cualquier punto de la sede", punto));
        Campo<DatePicker> fecha = f.agregar(Formulario.fecha("Fecha requerida de uso", fechaUso, Fechas.hoy(), null)).requerido("Elige la fecha de uso.");
        fecha.ayuda(Fechas.largo(fechaUso));
        Campo<ComboBox<Opcion>> dur = f.agregar(Formulario.selector("Duración estimada del préstamo", Opcion.pares(EquipoService.DURACIONES_LARGAS), null, duracion));
        long nAll = base.size();
        long nNow = base.stream().filter(Equipo::isDisponibleAhora).count();
        FlowPane chips = Componentes.chips();
        ToggleGroup g = new ToggleGroup();
        for (String[] c : new String[][]{{"all", "Todos (" + nAll + ")"}, {"now", "Disponibles ahora (" + nNow + ")"}, {"res", "Requiere reserva (" + (nAll - nNow) + ")"}}) {
            ToggleButton b = new ToggleButton();
            Componentes.fijar(b, c[1]);
            b.getStyleClass().add("chip");
            b.setToggleGroup(g);
            b.setSelected(c[0].equals(chip));
            b.setOnAction(e -> {
                chip = c[0];
                redibujar();
            });
            chips.getChildren().add(b);
        }
        Componentes.accesible(chips, "Filtrar por disponibilidad");
        HBox.setHgrow(chips, Priority.ALWAYS);
        Button consultar = Componentes.boton("Consultar inventario", "btn-primary", null, e -> {
            if (!f.validar()) {
                return;
            }
            categoria = Formulario.valor(cat.control());
            punto = Formulario.valor(pto.control());
            fechaUso = fecha.control().getValue();
            duracion = Formulario.valor(dur.control());
            refrescar();
            Platform.runLater(() -> Componentes.desplazarHasta(desplazable, catalogoSeccion));
            notificador.exito("Inventario actualizado.");
        });
        HBox pie = new HBox(chips, consultar);
        pie.getStyleClass().add("form-foot");
        VBox panel = new VBox(20, Componentes.parrafo("Buscar equipo y verificar disponibilidad", "h2"),
                Formulario.rejilla(2, cat.nodo(), pto.nodo(), fecha.nodo(), dur.nodo()), pie);
        panel.getStyleClass().add("panel");
        return Componentes.seccion(panel);
    }

    private VBox catalogo(List<Equipo> base) {
        List<Equipo> porChip = base.stream().filter(e -> "all".equals(chip) || ("now".equals(chip) ? e.isDisponibleAhora() : !e.isDisponibleAhora())).toList();
        List<Equipo> lista = FiltroService.aplicar(porChip, filtros, equipos.predicados(),
                e -> e.getNombre() + " " + e.getEspecificaciones() + " " + e.getUbicacion() + " " + e.getRotulo());
        HBox cab = Componentes.cabeceraSeccion("Catálogo de equipamiento", lista.size() + " equipo" + (lista.size() == 1 ? "" : "s") + " · uso el "
                + Fechas.dmy(fechaUso) + " · pasa el cursor y elige uno para ver detalles",
                Filtros.boton(filtros, EquipoService.PRESETS, null, notificador, this::redibujar));
        Node contenido;
        if (lista.isEmpty()) {
            contenido = Componentes.vacio("fth-search", "No hay equipos con esos filtros",
                    "Cambia la categoría, el punto de entrega o la sede (ahora: " + contexto.nombreSede() + ").");
        } else {
            Galeria g = new Galeria();
            for (int i = 0; i < lista.size(); i++) {
                g.getChildren().add(accionesEquipo.tarjeta(lista.get(i), i, this::redibujar, fechaUso, duracion));
            }
            contenido = g;
        }
        return Componentes.seccion(cab, Filtros.resumen(filtros, EquipoService.PRESETS, null, this::redibujar), contenido);
    }

    private VBox activo() {
        HBox cab = Componentes.cabeceraSeccion("Préstamo activo en posesión", null);
        var activo = equipos.activo();
        if (activo.isEmpty()) {
            return Componentes.seccion(cab, Componentes.vacio("fth-box", "No tienes préstamos activos", "Cuando retires un equipo aparecerá aquí con su hora límite."));
        }
        PrestamoActivo a = activo.get();
        Persona yo = sesion.estudiante();
        boolean vencido = a.getLimite().isBefore(LocalDateTime.now(Fechas.reloj()));
        VBox titulo = new VBox(4, Componentes.literalParrafo("Préstamo en custodia N.° " + a.getCodigo() + " • " + a.getEquipo(), "loan-titulo"),
                Componentes.parrafo("Retirado " + a.getRetiro() + " • Titular: " + yo.getNombre() + " (Código: " + yo.getCodigo() + ")", "muted", "small"));
        HBox.setHgrow(titulo, Priority.ALWAYS);
        Label estado = vencido ? Componentes.pill("Devolución vencida", "warning")
                : Componentes.pill(a.getIncidencia() != null ? "En uso • Incidencia reportada" : "En uso • A tiempo", "success");
        HBox arriba = new HBox(titulo, estado);
        Label limite = Componentes.etiqueta(EquipoService.textoLimite(a.getLimite(), LocalDateTime.now(Fechas.reloj())), "loan-deadline");
        reloj = new Timeline(new KeyFrame(Duration.seconds(30), e -> Componentes.fijar(limite, EquipoService.textoLimite(a.getLimite(), LocalDateTime.now(Fechas.reloj())))));
        reloj.setCycleCount(Animation.INDEFINITE);
        reloj.play();
        VBox info = new VBox(4, Componentes.mayus("Hora límite de devolución", "label-caps"), limite,
                Componentes.parrafo("Lugar de devolución: " + a.getLugar(), "muted", "small"));
        HBox.setHgrow(info, Priority.ALWAYS);
        Button renovar = Componentes.boton(a.isRenovado() ? "Tiempo renovado" : "Renovar tiempo (+2h)", "btn-secondary", "fth-refresh-cw", e ->
                notificador.confirmar("Renovar tiempo de préstamo", "Se agregarán 2 horas a la hora límite de devolución. Solo puedes renovar una vez por préstamo.",
                        "Renovar +2h", false, () -> {
                            if (intentar(equipos::renovar)) {
                                redibujar();
                                notificador.exito("Préstamo renovado. Nueva hora límite actualizada.");
                            }
                        }));
        renovar.setDisable(a.isRenovado());
        Button reportar = Componentes.boton("Reportar daño / falla", "btn-secondary", "fth-tool", e -> reportar(a));
        HBox caja = new HBox(info, new HBox(8, renovar, reportar));
        caja.getStyleClass().add("loan-box");
        VBox loan = new VBox(arriba, caja);
        loan.getStyleClass().add("loan");
        return Componentes.seccion(cab, loan);
    }

    private void reportar(PrestamoActivo a) {
        Formulario f = new Formulario();
        Campo<ComboBox<Opcion>> tipo = f.agregar(Formulario.selector("Tipo de incidencia", Opcion.lista(EquipoService.TIPOS_INCIDENCIA), "Seleccionar", null))
                .requerido("Elige el tipo de incidencia.");
        Campo<TextArea> desc = f.agregar(Formulario.area("Describe lo ocurrido", "Ej. El proyector se apaga después de 10 minutos de uso."))
                .requerido("Describe la incidencia.").minimo(15);
        Campo<Formulario.Archivo> foto = f.agregar(Formulario.archivo("Foto de evidencia (opcional)", "*.jpg *.png *.pdf", null, notificador, null));
        VBox cuerpo = new VBox(14, Componentes.parrafo("Préstamo " + a.getCodigo() + " · " + a.getEquipo() + ". Se levantará un acta de conformidad técnica.", "muted", "small"),
                tipo.nodo(), desc.nodo(), foto.nodo());
        notificador.modal("Reportar daño o falla", cuerpo, false, Componentes.boton("Cancelar", "btn-secondary", null, e -> notificador.cerrarModal()),
                Componentes.boton("Enviar reporte", "btn-primary", null, e -> {
                    if (f.validar() && intentar(() -> equipos.reportar(Formulario.valor(tipo.control()), desc.control().getText()))) {
                        notificador.cerrarModal();
                        redibujar();
                        notificador.exito("Reporte enviado. Un técnico te contactará.");
                    }
                }));
    }

    private VBox historial() {
        HBox cab = Componentes.cabeceraSeccion("Historial de préstamos", null);
        List<Prestamo> lista = equipos.historial();
        if (lista.isEmpty()) {
            return Componentes.seccion(cab, Componentes.vacio("fth-box", "Sin historial", "Tus préstamos aparecerán aquí."));
        }
        List<Node> filas = new ArrayList<>();
        Persona yo = sesion.estudiante();
        for (Prestamo h : lista) {
            HBox tit = new HBox(6, Componentes.parrafo(h.getNombre(), "row-title"), Componentes.literal("(" + h.getCodigo() + ")", "muted"));
            tit.setAlignment(Pos.CENTER_LEFT);
            VBox textos = new VBox(2, tit, Componentes.parrafo(h.getDetalle(), "row-sub"));
            boolean activo = h.getEstado().isEnCurso();
            filas.add(Componentes.fila(textos, Componentes.pill(h.getEstado().getEtiqueta()),
                    Componentes.botonSm("Ver cargo", "btn-secondary", null, e -> cargo(h, yo)),
                    Componentes.botonIcono("fth-trash-2", (activo ? "Cancelar solicitud" : "Eliminar del historial") + " " + h.getCodigo(), e -> eliminar(h))));
        }
        return Componentes.seccion(cab, Componentes.lista(filas));
    }

    private void cargo(Prestamo h, Persona yo) {
        notificador.modal("Cargo de préstamo", Componentes.kv("Código", Componentes.literal(h.getCodigo(), "dd"), "Equipo", Componentes.parrafo(h.getNombre(), "dd"),
                        "Detalle", Componentes.parrafo(h.getDetalle(), "dd"), "Titular", Componentes.literal(yo.getNombre() + " (" + yo.getCodigo() + ")", "dd"),
                        "Estado", Componentes.pill(h.getEstado().getEtiqueta())), false,
                Componentes.boton("Cerrar", "btn-secondary", null, e -> notificador.cerrarModal()),
                Componentes.boton("Descargar cargo", "btn-primary", "fth-download", e -> descargas.guardarPdf("Cargo " + h.getCodigo(),
                        () -> generador.pdfSimple("Cargo de préstamo " + h.getCodigo(), h.getNombre() + " · " + h.getDetalle()))));
    }

    private void eliminar(Prestamo h) {
        boolean activo = h.getEstado().isEnCurso();
        notificador.confirmar(activo ? "¿Cancelar esta solicitud de préstamo?" : "¿Eliminar del historial?",
                activo ? "La solicitud " + h.getCodigo() + " se cancelará y el equipo quedará libre para otros usuarios."
                        : "El registro " + h.getCodigo() + " se quitará de tu historial.",
                activo ? "Cancelar solicitud" : "Eliminar", true, () -> {
                    equipos.eliminar(h.getId());
                    redibujar();
                    notificador.exito(activo ? "Solicitud cancelada." : "Registro eliminado.");
                });
    }
}
