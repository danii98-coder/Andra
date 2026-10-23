package pe.edu.unaj.andara.controller;

import javafx.scene.Node;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pe.edu.unaj.andara.model.*;
import pe.edu.unaj.andara.service.*;
import pe.edu.unaj.andara.ui.*;
import pe.edu.unaj.andara.util.Fechas;
import pe.edu.unaj.andara.util.Montos;
import pe.edu.unaj.andara.util.TextoPlano;

import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

@Component
@RequiredArgsConstructor
public class TramitesAdminController extends VistaBase {

    private final AdminService admin;
    private final ContextoService contexto;
    private final CatalogoService catalogo;
    private final AccionesAdmin acciones;

    private final EstadoFiltro filtros = new EstadoFiltro();
    private String pestana = "todo";

    @Override
    protected List<Node> construir() {
        List<Expediente> base = admin.expedientes();
        Map<String, Predicate<Expediente>> tabs = new LinkedHashMap<>();
        tabs.put("todo", s -> s.getGrupo() == GrupoExpediente.TRAMITE);
        tabs.put("obs", s -> s.getGrupo() == GrupoExpediente.OBSERVADO);
        tabs.put("listo", s -> s.getGrupo() == GrupoExpediente.LISTO);
        tabs.put("all", s -> true);
        String[][] nombres = {{"todo", "Por atender"}, {"obs", "Observados"}, {"listo", "Listos y entregados"}, {"all", "Todos"}};
        Segmentado seg = new Segmentado(true);
        for (String[] n : nombres) {
            seg.opcion(n[0], n[1], null, base.stream().filter(tabs.get(n[0])).count());
        }
        seg.seleccionar(pestana).alCambiar(v -> {
            pestana = v;
            redibujar();
        });
        seg.setMaxWidth(640);
        List<Expediente> lista = base.stream().filter(tabs.get(pestana)).toList();
        Map<String, Function<Expediente, String>> campos = new LinkedHashMap<>();
        campos.put("fac", s -> escuela(s));
        campos.put("tipo", s -> acciones.solicitante(s).getTipo());
        lista = FiltroService.aplicarAvanzado(lista, filtros, new FiltroService.Accesores<>(SeguimientoService::fechaDe, this::clave, campos));
        Map<String, Predicate<Expediente>> pred = new HashMap<>(FiltroService.predicadosFecha(SeguimientoService::fechaDe));
        pred.put("paid", s -> s.getPago() != null && s.getPago().isPagado());
        pred.put("unpaid", s -> s.getPago() != null && s.getPago().isPendiente());
        pred.put("carne", s -> s.getTramite() != null && s.getTramite().isCarne());
        lista = FiltroService.aplicar(lista, filtros, pred, s -> s.getCodigo() + " " + s.getTitulo() + " " + s.getPersona().getNombre() + " " + s.getEstado());
        List<Filtros.Avanzado> avanzados = AdminFiltros.fechas("Desde", "Hasta");
        avanzados.addAll(AdminFiltros.persona(catalogo, "Código o DNI del solicitante", "Ej. 2024207009"));
        HBox cab = new HBox(16, seg, Componentes.espacio(), Filtros.boton(filtros, AdminService.PRESETS_TRAMITES, avanzados, notificador, this::redibujar));
        cab.getStyleClass().add("section-h");
        Node cuerpo;
        if (lista.isEmpty()) {
            cuerpo = Componentes.vacio("fth-inbox", "No hay trámites aquí", "Cambia de pestaña, de sede o quita filtros.");
        } else {
            List<Node[]> filas = new ArrayList<>();
            for (Expediente s : lista) {
                DatosSolicitante sol = acciones.solicitante(s);
                LocalDate f = SeguimientoService.fechaDe(s);
                Pago p = s.getPago();
                Node pago = p == null ? Componentes.etiqueta("No aplica", "hint") : p.isPagado() ? Componentes.pill(Montos.soles(p.getTotal()), "success")
                        : Componentes.pill(p.getEstado() == EstadoPago.PENDIENTE ? "Pendiente" : p.getEstado().getEtiqueta(), p.isPendiente() ? "warning" : "neutral");
                String escuela = sol.getEscuela() != null && catalogo.escuela(sol.getEscuela()).isPresent() ? " · " + catalogo.nombreEscuela(sol.getEscuela()) : "";
                filas.add(new Node[]{Tabla.celda(s.getCodigo(), f != null ? Fechas.dmy(f) : s.getDatos().get(0).getValor()),
                        Tabla.celda(sol.getNombre(), sol.getTipo() + " · " + (sol.getCodigo() == null ? "—" : sol.getCodigo()) + escuela),
                        Componentes.parrafo(s.getTitulo(), "small"), Componentes.literal(s.getSede().getNombre(), "small"), pago,
                        Componentes.pill(TextoPlano.recortar(s.getEstado(), 34), s.getTono()),
                        Componentes.botonSm("Revisar", "btn-secondary", null, e -> acciones.revisar(s.getCodigo(), this::redibujar))});
            }
            cuerpo = Tabla.crear(new String[]{"Expediente", "Solicitante", "Trámite", "Sede", "Pago", "Estado", "Acciones"},
                    new double[]{14, 22, 20, 9, 11, 15, 9}, new boolean[]{false, false, false, false, false, false, true}, filas);
        }
        VBox s = Componentes.seccion(cab, Filtros.resumen(filtros, AdminService.PRESETS_TRAMITES, catalogo::nombreEscuela, this::redibujar), cuerpo);
        s.getStyleClass().add("sin-arriba");
        return List.of(Componentes.heroAdmin("Trámites y documentos", "Revisa lo que piden estudiantes, docentes, administrativos y público en general.",
                Componentes.lineaContexto(contexto.textoSede(), contexto.textoEscuela(), "ctx:sede")), s);
    }

    private String clave(Expediente s) {
        DatosSolicitante sol = acciones.solicitante(s);
        return (sol.getCodigo() == null ? "" : sol.getCodigo()) + " " + (sol.getDni() == null ? "" : sol.getDni()) + " " + sol.getNombre();
    }

    private String escuela(Expediente s) {
        DatosSolicitante sol = acciones.solicitante(s);
        return sol.getEscuela();
    }
}
