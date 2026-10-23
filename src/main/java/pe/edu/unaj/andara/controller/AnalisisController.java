package pe.edu.unaj.andara.controller;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import lombok.RequiredArgsConstructor;
import org.kordamp.ikonli.javafx.FontIcon;
import org.springframework.stereotype.Component;
import pe.edu.unaj.andara.model.Persona;
import pe.edu.unaj.andara.service.*;
import pe.edu.unaj.andara.ui.*;
import pe.edu.unaj.andara.util.Montos;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class AnalisisController extends VistaBase {

    private final AnaliticaService analitica;
    private final ContextoService contexto;
    private final CarneService carne;

    private String periodo = "30";
    private TextField validar;
    private VBox resultado;

    @Override
    protected List<Node> construir() {
        AnaliticaService.Analisis a = analitica.analizar(periodo);
        List<Node> nodos = new ArrayList<>();
        nodos.add(Componentes.heroAdmin("Análisis", "Datos para decidir: qué se pide, cuándo se saturan las ventanillas y qué espacios se aprovechan poco.",
                Componentes.lineaContexto(contexto.textoSede(), contexto.textoEscuela(), "ctx:sede")));
        Segmentado per = new Segmentado(true).opcion("7", "7 días", null, null).opcion("30", "30 días", null, null)
                .opcion("sem", "Semestre", null, null).seleccionar(periodo);
        per.alCambiar(v -> {
            periodo = v;
            redibujar();
        });
        per.setMaxWidth(420);
        Componentes.accesible(per, "Periodo");
        VBox.setMargin(per, new javafx.geometry.Insets(0, 0, 20, 0));
        Rejilla kpis = new Rejilla(4, 200, 16);
        kpis.getChildren().addAll(Componentes.kpi("Trámites y órdenes", String.valueOf(a.tramitesYOrdenes()), null, "En el periodo"),
                Componentes.kpi("Recaudación", Montos.soles(a.recaudacion()), null, "Pagos confirmados"),
                Componentes.kpi("Atención promedio", "4 h 20 min", null, "Del pago a \"listo para recoger\""),
                Componentes.kpi("Uso de espacios", a.usoPromedio() + " %", null, "Promedio semanal"));
        VBox s0 = Componentes.seccion(per, kpis);
        s0.getStyleClass().add("sin-arriba");
        nodos.add(s0);
        VBox lista = new VBox(6);
        for (AnaliticaService.Hallazgo h : a.hallazgos()) {
            TextFlow f = new TextFlow();
            if (!h.antes().isBlank()) {
                f.getChildren().add(Componentes.texto(h.antes()));
            }
            if (!h.negrita().isBlank()) {
                Text n = Componentes.texto(h.negrita());
                n.setStyle("-fx-font-weight: 700;");
                f.getChildren().add(n);
            }
            if (!h.despues().isBlank()) {
                f.getChildren().add(Componentes.texto(h.despues()));
            }
            f.getChildren().forEach(t -> t.setStyle((t.getStyle() == null ? "" : t.getStyle()) + "-fx-font-size: 14.5px;"));
            lista.getChildren().add(new HBox(8, Componentes.literal("•"), f));
        }
        VBox textos = new VBox(8, Componentes.parrafo("Lo que dicen los datos", "h4"), lista);
        HBox.setHgrow(textos, Priority.ALWAYS);
        HBox insights = new HBox(new FontIcon("fth-zap"), textos);
        insights.getStyleClass().add("insights");
        nodos.add(Componentes.seccion(insights));
        VBox top = panel("Trámites más pedidos", "Cantidad de solicitudes en el periodo");
        top.getChildren().add(a.top().isEmpty() ? Componentes.parrafo("Sin datos en el periodo.", "hint")
                : Graficos.barrasHorizontales(a.top(), b -> String.valueOf((int) b.valor()), false));
        VBox sedes = panel("Recaudación por sede", "Soles, pagos confirmados");
        sedes.getChildren().add(Graficos.barrasHorizontales(a.porSede(), b -> Montos.soles(b.valor()), false));
        nodos.add(Componentes.seccion(PanelController.dosColumnas(top, sedes, 50)));
        VBox calor = panel("Horas punta en Mesa de Partes", "Atenciones estimadas por día y franja");
        calor.getChildren().add(Graficos.mapaCalorBnps(a));
        VBox uso = panel("Uso de espacios", "% del horario semanal ocupado");
        uso.getChildren().addAll(Componentes.mayus("Subutilizados", "label-caps"), Graficos.barrasHorizontales(a.subutilizados(), b -> (int) b.valor() + " %", true),
                Componentes.mayus("Más demandados", "label-caps"), Graficos.barrasHorizontales(a.demandados(), b -> (int) b.valor() + " %", true));
        nodos.add(Componentes.seccion(PanelController.dosColumnas(calor, uso, 50)));
        nodos.add(Componentes.seccion(validador()));
        return nodos;
    }

    private VBox panel(String titulo, String sub) {
        VBox v = new VBox(10, Componentes.parrafo(titulo, "h4"), Componentes.parrafo(sub, "hint"));
        v.getStyleClass().addAll("panel", "compacto");
        return v;
    }

    private VBox validador() {
        validar = new TextField();
        Componentes.prompt(validar, "ANDARA-ID|… o 2024207009");
        Componentes.accesible(validar, "Código del carné");
        HBox.setHgrow(validar, Priority.ALWAYS);
        resultado = new VBox();
        Button boton = Componentes.boton("Validar", "btn-primary", null, e -> mostrar());
        validar.setOnAction(e -> mostrar());
        HBox fila = new HBox(validar, boton);
        fila.getStyleClass().add("copy-row");
        VBox p = panel("Validar carné digital", "Pega el contenido del QR o escribe el código o DNI del estudiante.");
        p.getChildren().addAll(fila, resultado);
        return p;
    }

    private void mostrar() {
        CarneService.Verificacion v = carne.verificar(validar.getText());
        VBox textos = new VBox(2, Componentes.parrafo(v.mensaje(), "strong"));
        Persona u = v.persona();
        if (u != null) {
            textos.getChildren().add(Componentes.literalParrafo(u.getNombre() + " · " + u.getTipo() + " · " + u.getCodigo()
                    + (u.getEscuela() != null ? " · EP " + u.getEscuela().getNombre() : ""), "small"));
        }
        HBox caja = new HBox(new FontIcon(v.ok() ? "fth-check-circle" : "fth-alert-triangle"), textos);
        caja.getStyleClass().addAll("vc", v.ok() ? "ok" : "no");
        caja.setAlignment(Pos.TOP_LEFT);
        resultado.getChildren().setAll(caja);
        Movimiento.subir(caja);
    }
}
