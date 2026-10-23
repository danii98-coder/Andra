package pe.edu.unaj.andara.controller;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pe.edu.unaj.andara.model.EstadoPrestamo;
import pe.edu.unaj.andara.model.Prestamo;
import pe.edu.unaj.andara.model.PrestamoActivo;
import pe.edu.unaj.andara.service.*;
import pe.edu.unaj.andara.ui.*;
import pe.edu.unaj.andara.util.Fechas;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

@Component
@RequiredArgsConstructor
public class PrestamosAdminController extends VistaBase {

    private final AdminService admin;
    private final ContextoService contexto;
    private final CatalogoService catalogo;
    private final AccionesAdmin acciones;

    private final EstadoFiltro filtros = new EstadoFiltro();

    @Override
    protected List<Node> construir() {
        List<Prestamo> l = admin.prestamos();
        Map<String, Function<Prestamo, String>> campos = new LinkedHashMap<>();
        campos.put("fac", p -> AdminFiltros.escuela(p.getPersona()));
        campos.put("tipo", p -> p.getPersona().getTipo());
        l = FiltroService.aplicarAvanzado(l, filtros, new FiltroService.Accesores<>(p -> null, p -> AdminFiltros.clave(p.getPersona()), campos));
        Map<String, Predicate<Prestamo>> pred = Map.of("rev", p -> p.getEstado() == EstadoPrestamo.EN_REVISION,
                "apr", p -> p.getEstado() == EstadoPrestamo.APROBADO, "back", p -> p.getEstado() == EstadoPrestamo.DEVUELTO, "lab", admin::esLaboratorio);
        l = FiltroService.aplicar(l, filtros, pred, p -> p.getNombre() + " " + p.getCodigo() + " " + p.getPersona().getNombre());
        List<Prestamo> curso = l.stream().filter(p -> p.getEstado().isEnCurso()).toList();
        List<Prestamo> resto = l.stream().filter(p -> !p.getEstado().isEnCurso()).toList();
        VBox s1 = new VBox();
        s1.getStyleClass().addAll("section", "sin-arriba");
        s1.setMaxWidth(1168);
        Optional<PrestamoActivo> custodia = admin.custodia();
        if (custodia.isPresent()) {
            PrestamoActivo a = custodia.get();
            HBox tit = new HBox(6, Componentes.etiqueta("En custodia · " + a.getEquipo(), "row-title"), Componentes.literal("(" + a.getCodigo() + ")", "muted"));
            tit.setAlignment(Pos.CENTER_LEFT);
            VBox textos = new VBox(2, tit, Componentes.etiqueta(a.getPersona().getNombre() + " · " + EquipoService.textoLimite(a.getLimite(), LocalDateTime.now(Fechas.reloj())), "row-sub"));
            VBox lst = Componentes.lista(List.of(Componentes.fila(textos, Componentes.pill("En uso", "info"),
                    Componentes.botonSm("Registrar devolución", "btn-secondary", null, e -> acciones.devolverCustodia(this::redibujar)))));
            VBox.setMargin(lst, new Insets(0, 0, 20, 0));
            s1.getChildren().add(lst);
        }
        HBox cab = Componentes.cabeceraSeccion("En curso (" + curso.size() + ")", null, Filtros.boton(filtros, AdminService.PRESETS_PRESTAMOS,
                AdminFiltros.persona(catalogo, "Código o DNI del solicitante", "Ej. 2024207009"), notificador, this::redibujar));
        s1.getChildren().addAll(cab, Filtros.resumen(filtros, AdminService.PRESETS_PRESTAMOS, catalogo::nombreEscuela, this::redibujar),
                curso.isEmpty() ? Componentes.vacio("fth-box", "Nada en curso", "No hay préstamos por revisar.") : lista(curso));
        List<Node> nodos = new ArrayList<>(List.of(Componentes.heroAdmin("Préstamos de equipos", "Revisa solicitudes, entregas y devoluciones del inventario.",
                Componentes.lineaContexto(contexto.textoSede(), contexto.textoEscuela(), "ctx:sede")), s1));
        if (!resto.isEmpty()) {
            nodos.add(Componentes.seccion(Componentes.cabeceraSeccion("Historial", null), lista(resto)));
        }
        return nodos;
    }

    private VBox lista(List<Prestamo> prestamos) {
        List<Node> filas = new ArrayList<>();
        for (Prestamo h : prestamos) {
            HBox tit = new HBox(6, Componentes.parrafo(h.getNombre(), "row-title"), Componentes.literal("(" + h.getCodigo() + ")", "muted"));
            tit.setAlignment(Pos.CENTER_LEFT);
            VBox textos = new VBox(2, tit, Componentes.parrafo(h.getPersona().getNombre() + " · " + h.getDetalle(), "row-sub"));
            List<Node> acc = new ArrayList<>(List.of(Componentes.pill(h.getEstado().getEtiqueta())));
            if (h.getEstado() == EstadoPrestamo.EN_REVISION) {
                acc.add(Componentes.botonSm("Aprobar", "btn-primary", null, e -> acciones.aprobarPrestamo(h, this::redibujar)));
                acc.add(Componentes.botonSm("Rechazar", "btn-danger", null, e -> acciones.rechazarPrestamo(h, this::redibujar)));
            } else if (h.getEstado() == EstadoPrestamo.APROBADO) {
                acc.add(Componentes.botonSm("Registrar devolución", "btn-secondary", null, e -> acciones.devolverPrestamo(h, this::redibujar)));
            }
            filas.add(Componentes.fila(textos, acc.toArray(new Node[0])));
        }
        return Componentes.lista(filas);
    }
}
