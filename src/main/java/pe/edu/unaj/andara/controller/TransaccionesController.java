package pe.edu.unaj.andara.controller;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pe.edu.unaj.andara.model.*;
import pe.edu.unaj.andara.service.*;
import pe.edu.unaj.andara.ui.*;
import pe.edu.unaj.andara.util.Fechas;
import pe.edu.unaj.andara.util.Montos;

import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

@Component
@RequiredArgsConstructor
public class TransaccionesController extends VistaBase {

    private final AdminService admin;
    private final ContextoService contexto;
    private final CatalogoService catalogo;
    private final AccionesAdmin acciones;
    private final AccionesPago accionesPago;

    private final EstadoFiltro filtros = new EstadoFiltro();

    private static LocalDate fecha(Pago p) {
        return p.getPagado() == null ? null : p.getPagado().toLocalDate();
    }

    @Override
    protected List<Node> construir() {
        List<Pago> base = admin.pagos();
        List<Pago> lista = base.stream().filter(p -> !p.isPendiente() && p.getPagado() != null).toList();
        Map<String, Function<Pago, String>> campos = new LinkedHashMap<>();
        campos.put("fac", p -> AdminFiltros.escuela(p.getPersona()));
        campos.put("tipo", p -> p.getPersona().getTipo());
        campos.put("kind", p -> p.getTipo().name().toLowerCase());
        lista = FiltroService.aplicarAvanzado(lista, filtros, new FiltroService.Accesores<>(TransaccionesController::fecha, p -> AdminFiltros.clave(p.getPersona()), campos));
        Map<String, Predicate<Pago>> pred = new HashMap<>(FiltroService.predicadosFecha(TransaccionesController::fecha));
        pred.put("yape", p -> p.getMetodo() == MetodoPago.YAPE);
        pred.put("plin", p -> p.getMetodo() == MetodoPago.PLIN);
        pred.put("card", p -> p.getMetodo() != null && p.getMetodo().isTarjeta());
        lista = new ArrayList<>(FiltroService.aplicar(lista, filtros, pred, p -> p.getCodigo() + " " + p.getConcepto() + " " + p.getOperacion() + " "
                + p.getBoleta() + " " + p.getPersona().getNombre()));
        lista.sort(Comparator.comparing(Pago::getPagado).reversed());
        List<Pago> pendientes = base.stream().filter(Pago::isPendiente).toList();
        List<Node> nodos = new ArrayList<>();
        nodos.add(Componentes.heroAdmin("Transacciones", "Todos los pagos de Andara Pay: matrículas, documentos, carnés y otros derechos.",
                Componentes.lineaContexto(contexto.textoSede(), contexto.textoEscuela(), "ctx:sede")));
        Rejilla kpis = new Rejilla(3, 220, 16);
        kpis.getChildren().addAll(Componentes.kpi("Total en la vista", Montos.soles(PagoService.total(lista.stream().filter(Pago::isPagado).toList())), "success",
                        lista.size() + " transacciones"),
                Componentes.kpi("Órdenes por cobrar", String.valueOf(pendientes.size()), "warning", Montos.soles(PagoService.total(pendientes)) + " pendientes"),
                Componentes.kpi("Reembolsos", String.valueOf(base.stream().filter(p -> p.getEstado() == EstadoPago.REEMBOLSADO).count()), null, "En la sede elegida"));
        VBox s0 = Componentes.seccion(kpis);
        s0.getStyleClass().add("sin-arriba");
        nodos.add(s0);
        List<Filtros.Avanzado> avanzados = AdminFiltros.fechas("Desde", "Hasta");
        avanzados.add(Filtros.Avanzado.texto("code", "Código o DNI del usuario", "Ej. 2023104512"));
        avanzados.add(Filtros.Avanzado.selector("fac", "Escuela profesional", "Todas las escuelas", AdminFiltros.escuelas(catalogo)));
        avanzados.add(Filtros.Avanzado.selector("kind", "Concepto", "Todos", List.of(Opcion.de("matricula", "Matrícula"), Opcion.de("documento", "Documento"),
                Opcion.de("carne", "Carné"), Opcion.de("tramite", "Trámite"))));
        HBox cab = Componentes.cabeceraSeccion("Pagos confirmados", null, Filtros.boton(filtros, AdminService.PRESETS_PAGOS, avanzados, notificador, this::redibujar));
        Node cuerpo;
        if (lista.isEmpty()) {
            cuerpo = Componentes.vacio("fth-file", "Sin transacciones", "Cambia de sede o quita filtros.");
        } else {
            List<Node[]> filas = new ArrayList<>();
            for (Pago p : lista) {
                Circle punto = new Circle(4.5);
                punto.getStyleClass().add("pm-dot");
                if (p.getMetodo() == MetodoPago.YAPE) {
                    punto.getStyleClass().add("pm-yape");
                } else if (p.getMetodo() == MetodoPago.PLIN) {
                    punto.getStyleClass().add("pm-plin");
                }
                HBox metodo = new HBox(6, punto, Componentes.literal(p.getMetodo().getEtiqueta(), "small"));
                metodo.setAlignment(Pos.CENTER_LEFT);
                HBox botones = new HBox(4, Componentes.botonSm("Ver", "btn-tertiary", null, e -> accionesPago.comprobante(p.getCodigo())));
                if (p.isPagado()) {
                    botones.getChildren().add(Componentes.botonSm("Reembolsar", "btn-danger", null, e -> acciones.reembolsar(p, this::redibujar)));
                }
                filas.add(new Node[]{Tabla.celda(Fechas.dmy(p.getPagado().toLocalDate()), Fechas.hm(p.getPagado())), Tabla.celda(p.getOperacion(), p.getBoleta()),
                        Tabla.celda(p.getPersona().getNombre(), p.getPersona().getTipo()), Componentes.parrafo(p.getConcepto(), "small"), metodo,
                        Componentes.literal(Montos.soles(p.getTotal()), "strong"),
                        p.getEstado() == EstadoPago.REEMBOLSADO ? Componentes.pill("Reembolsado", "neutral") : Componentes.pill("Confirmado", "success"), botones});
            }
            cuerpo = Tabla.crear(new String[]{"Fecha", "Operación", "Usuario", "Concepto", "Método", "Monto", "Estado", ""},
                    new double[]{10, 13, 17, 19, 10, 9, 10, 12}, new boolean[]{false, false, false, false, false, true, false, true}, filas);
        }
        nodos.add(Componentes.seccion(cab, Filtros.resumen(filtros, AdminService.PRESETS_PAGOS, catalogo::nombreEscuela, this::redibujar), cuerpo));
        Node porCobrar;
        if (pendientes.isEmpty()) {
            porCobrar = Componentes.vacio("fth-check", "Nada pendiente", "Todas las órdenes están pagadas.");
        } else {
            List<Node> filas = new ArrayList<>();
            pendientes.forEach(p -> filas.add(Componentes.fila(Componentes.textosFila(p.getConcepto(), p.getCodigo() + " · " + p.getPersona().getNombre()
                    + " · vence " + Fechas.dmy(p.getVence())), Componentes.literal(Montos.soles(p.getTotal()), "strong"), Componentes.pill("Pendiente", "warning"))));
            porCobrar = Componentes.lista(filas);
        }
        nodos.add(Componentes.seccion(Componentes.cabeceraSeccion("Órdenes por cobrar", null), porCobrar));
        return nodos;
    }
}
