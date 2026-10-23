package pe.edu.unaj.andara.controller;

import javafx.scene.Node;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pe.edu.unaj.andara.model.EstadoReserva;
import pe.edu.unaj.andara.model.Reserva;
import pe.edu.unaj.andara.model.TipoEspacio;
import pe.edu.unaj.andara.service.*;
import pe.edu.unaj.andara.ui.*;
import pe.edu.unaj.andara.util.Fechas;

import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

@Component
@RequiredArgsConstructor
public class ReservasAdminController extends VistaBase {

    private final AdminService admin;
    private final ContextoService contexto;
    private final CatalogoService catalogo;
    private final AccionesAdmin acciones;

    private final EstadoFiltro filtros = new EstadoFiltro();

    @Override
    protected List<Node> construir() {
        List<Reserva> l = admin.reservas();
        Map<String, Function<Reserva, String>> campos = new LinkedHashMap<>();
        campos.put("fac", r -> AdminFiltros.escuela(r.getPersona()));
        campos.put("tipo", r -> r.getPersona().getTipo());
        l = FiltroService.aplicarAvanzado(l, filtros, new FiltroService.Accesores<>(Reserva::getFecha, r -> AdminFiltros.clave(r.getPersona()), campos));
        LocalDate hoy = Fechas.hoy();
        Map<String, Predicate<Reserva>> pred = Map.of("today", r -> r.getFecha().equals(hoy),
                "d7", r -> !r.getFecha().isBefore(hoy) && !r.getFecha().isAfter(hoy.plusDays(7)),
                "big", r -> r.getAsistentes() >= 100, "aud", r -> r.getEspacio().getTipo() == TipoEspacio.AUDITORIO);
        l = FiltroService.aplicar(l, filtros, pred, r -> r.getEspacio().getNombre() + " " + r.getMotivo() + " " + r.getPersona().getNombre());
        List<Reserva> pendientes = l.stream().filter(r -> r.getEstado() == EstadoReserva.PENDIENTE).toList();
        List<Reserva> resto = l.stream().filter(r -> r.getEstado() != EstadoReserva.PENDIENTE).toList();
        List<Filtros.Avanzado> avanzados = AdminFiltros.fechas("Fecha desde", "Fecha hasta");
        avanzados.addAll(AdminFiltros.persona(catalogo, "Código o DNI del solicitante", "Ej. 2024207009"));
        HBox cab = Componentes.cabeceraSeccion("Por aprobar (" + pendientes.size() + ")", null,
                Filtros.boton(filtros, AdminService.PRESETS_RESERVAS, avanzados, notificador, this::redibujar));
        VBox s1 = Componentes.seccion(cab, Filtros.resumen(filtros, AdminService.PRESETS_RESERVAS, catalogo::nombreEscuela, this::redibujar),
                pendientes.isEmpty() ? Componentes.vacio("fth-calendar", "No hay reservas por aprobar", "Las nuevas solicitudes aparecerán aquí.") : lista(pendientes));
        s1.getStyleClass().add("sin-arriba");
        List<Node> nodos = new ArrayList<>(List.of(Componentes.heroAdmin("Reservas de espacios",
                "Aprueba o rechaza las solicitudes de aulas, laboratorios, salas, talleres y auditorios.",
                Componentes.lineaContexto(contexto.textoSede(), contexto.textoEscuela(), "ctx:sede")), s1));
        if (!resto.isEmpty()) {
            nodos.add(Componentes.seccion(Componentes.cabeceraSeccion("Historial", null), lista(resto)));
        }
        return nodos;
    }

    private VBox lista(List<Reserva> reservas) {
        List<Node> filas = new ArrayList<>();
        for (Reserva r : reservas) {
            VBox textos = new VBox(2, Componentes.literalParrafo(r.getEspacio().getNombre(), "row-title"),
                    Componentes.etiqueta(r.getPersona().getNombre() + " · " + Fechas.dmy(r.getFecha()) + " · " + r.getHorario() + " · " + r.getAsistentes()
                            + " personas · Sede " + r.getEspacio().getSede().getNombre(), "row-sub"),
                    Componentes.parrafo(r.getMotivo(), "row-sub"));
            List<Node> acc = new ArrayList<>(List.of(Componentes.pill(r.getEstado().getEtiqueta())));
            if (r.getEstado() == EstadoReserva.PENDIENTE) {
                acc.add(Componentes.botonSm("Aprobar", "btn-primary", null, e -> acciones.aprobarReserva(r, this::redibujar)));
                acc.add(Componentes.botonSm("Rechazar", "btn-danger", null, e -> acciones.rechazarReserva(r, this::redibujar)));
            }
            filas.add(Componentes.fila(textos, acc.toArray(new Node[0])));
        }
        return Componentes.lista(filas);
    }
}
