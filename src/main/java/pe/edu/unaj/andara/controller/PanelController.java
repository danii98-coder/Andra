package pe.edu.unaj.andara.controller;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pe.edu.unaj.andara.model.*;
import pe.edu.unaj.andara.service.*;
import pe.edu.unaj.andara.ui.*;
import pe.edu.unaj.andara.util.Fechas;
import pe.edu.unaj.andara.util.Montos;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class PanelController extends VistaBase {

    private final AdminService admin;
    private final SesionService sesion;
    private final ContextoService contexto;
    private final AccionesAdmin acciones;
    private final AccionesPago accionesPago;

    @Override
    protected List<Node> construir() {
        AdminService.Panel p = admin.panel();
        List<Node> nodos = new ArrayList<>();
        nodos.add(Componentes.heroAdmin("Hola, " + sesion.administrador().getPrimerNombre(),
                Fechas.largo(Fechas.hoy()) + " · Esto es lo que pasa hoy en Andara.",
                Componentes.lineaContexto(contexto.textoSede(), contexto.textoEscuela(), "ctx:sede")));
        Rejilla kpis = new Rejilla(4, 200, 16);
        kpis.getChildren().addAll(Componentes.kpi("Recaudado hoy", Montos.soles(p.hoy()), "success", "Pagos confirmados"),
                Componentes.kpi("Recaudado en el mes", Montos.soles(p.mes()), null, Fechas.mesAnio(Fechas.hoy())),
                Componentes.kpi("Trámites por atender", String.valueOf(p.porAtender().size()), "warning", "Pagados o en revisión"),
                Componentes.kpi("Reservas y préstamos", String.valueOf(p.reservasPendientes().size() + p.prestamosEnRevision().size()), "warning",
                        p.reservasPendientes().size() + " reservas · " + p.prestamosEnRevision().size() + " préstamos"));
        VBox s0 = Componentes.seccion(kpis);
        s0.getStyleClass().add("sin-arriba");
        nodos.add(s0);
        VBox barras = new VBox(Componentes.cabeceraSeccion("Recaudación de los últimos 7 días", "En soles, pagos confirmados",
                Componentes.botonSm("Ver transacciones", "btn-secondary", null, e -> ir("a-pagos"))), Graficos.barrasDias(p.dias()));
        barras.getStyleClass().addAll("panel", "compacto");
        VBox metodos = new VBox(14, Componentes.parrafo("Por método de pago", "h4"), Graficos.porMetodo(p.porMetodo()),
                Componentes.parrafo("Por concepto", "h4"), Graficos.leyendaSimple(p.porConcepto()));
        metodos.getStyleClass().addAll("panel", "compacto");
        nodos.add(Componentes.seccion(dosColumnas(barras, metodos, 62)));
        List<Node> atencion = new ArrayList<>();
        p.porAtender().stream().limit(4).forEach(s -> atencion.add(Componentes.fila(Componentes.textosFila(s.getTitulo(),
                        s.getPersona().getNombre() + " · " + s.getCodigo() + " · " + s.getEstado()),
                Componentes.botonSm("Atender", "btn-primary", null, e -> acciones.revisar(s.getCodigo(), this::redibujar)))));
        p.reservasPendientes().stream().limit(3).forEach(r -> atencion.add(Componentes.fila(Componentes.textosFila("Reserva · " + r.getEspacio().getNombre(),
                        r.getPersona().getNombre() + " · " + Fechas.dmy(r.getFecha()) + " " + r.getHorario()),
                Componentes.botonSm("Aprobar", "btn-secondary", null, e -> acciones.aprobarReserva(r, this::redibujar)),
                Componentes.botonSm("Rechazar", "btn-danger", null, e -> acciones.rechazarReserva(r, this::redibujar)))));
        if (atencion.isEmpty()) {
            atencion.add(Componentes.fila(Componentes.etiqueta("Todo al día.", "muted")));
        }
        List<Node> recientes = new ArrayList<>();
        for (Pago pg : p.recientes()) {
            Circle punto = new Circle(4.5);
            punto.getStyleClass().add("pm-dot");
            if (pg.getMetodo() == MetodoPago.YAPE) {
                punto.getStyleClass().add("pm-yape");
            } else if (pg.getMetodo() == MetodoPago.PLIN) {
                punto.getStyleClass().add("pm-plin");
            }
            VBox textos = Componentes.textosFila(pg.getConcepto(), pg.getPersona().getNombre() + " · " + pg.getMetodo().getEtiqueta() + " · "
                    + Fechas.hm(pg.getPagado()) + " " + Fechas.dmy(pg.getPagado().toLocalDate()));
            HBox principal = new HBox(12, punto, textos);
            principal.setAlignment(Pos.CENTER_LEFT);
            HBox fila = Componentes.fila(principal, Componentes.literal(Montos.soles(pg.getTotal()), "strong"));
            fila.getStyleClass().add("clic");
            fila.setOnMouseClicked(e -> accionesPago.comprobante(pg.getCodigo()));
            recientes.add(fila);
        }
        VBox izq = new VBox(Componentes.cabeceraSeccion("Requieren tu atención", null), Componentes.lista(atencion));
        VBox der = new VBox(Componentes.cabeceraSeccion("Últimos pagos", null), Componentes.lista(recientes));
        nodos.add(Componentes.seccion(dosColumnas(izq, der, 58)));
        return nodos;
    }

    static GridPane dosColumnas(Node a, Node b, double porcentaje) {
        GridPane g = new GridPane();
        g.setHgap(20);
        g.setVgap(20);
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setPercentWidth(porcentaje);
        ColumnConstraints c2 = new ColumnConstraints();
        c2.setPercentWidth(100 - porcentaje);
        g.getColumnConstraints().addAll(c1, c2);
        g.add(a, 0, 0);
        g.add(b, 1, 0);
        GridPane.setValignment(a, javafx.geometry.VPos.TOP);
        GridPane.setValignment(b, javafx.geometry.VPos.TOP);
        return g;
    }
}
