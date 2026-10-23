package pe.edu.unaj.andara.ui;

import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextFlow;
import org.kordamp.ikonli.javafx.FontIcon;
import pe.edu.unaj.andara.util.Fechas;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.function.Consumer;

public final class Calendario {

    private static final int DIAS_MAXIMOS = 90;

    private final Notificador notificador;
    private final Consumer<LocalDate> alElegir;
    private final VBox cuerpo = new VBox();
    private LocalDate seleccion;
    private YearMonth mes;

    private Calendario(Notificador notificador, LocalDate inicial, Consumer<LocalDate> alElegir) {
        this.notificador = notificador;
        this.alElegir = alElegir;
        this.seleccion = inicial;
        this.mes = YearMonth.from(inicial);
    }

    public static void abrir(Notificador notificador, LocalDate inicial, Consumer<LocalDate> alElegir) {
        Calendario c = new Calendario(notificador, inicial, alElegir);
        c.dibujar();
        notificador.modal("Elige una fecha", c.cuerpo, false,
                Componentes.boton("Cancelar", "btn-secondary", null, e -> notificador.cerrarModal()),
                Componentes.boton("Ver horarios", "btn-primary", null, e -> c.confirmar()));
    }

    private void confirmar() {
        notificador.cerrarModal();
        alElegir.accept(seleccion);
    }

    private void dibujar() {
        LocalDate hoy = Fechas.hoy();
        LocalDate maximo = hoy.plusDays(DIAS_MAXIMOS);
        Button anterior = Componentes.iconoRedondo("fth-chevron-left", "Mes anterior", e -> {
            mes = mes.minusMonths(1);
            dibujar();
        });
        anterior.setDisable(!mes.isAfter(YearMonth.from(hoy)));
        Button siguiente = Componentes.iconoRedondo("fth-chevron-right", "Mes siguiente", e -> {
            mes = mes.plusMonths(1);
            dibujar();
        });
        siguiente.setDisable(!mes.isBefore(YearMonth.from(maximo)));
        Label titulo = Componentes.etiqueta(Fechas.mesAnio(mes.atDay(1)), "cal-mes");
        HBox cabecera = new HBox(anterior, Componentes.espacio(), titulo, Componentes.espacio(), siguiente);
        cabecera.getStyleClass().add("cal-head");

        GridPane grilla = new GridPane();
        grilla.setHgap(4);
        grilla.setVgap(4);
        for (int i = 0; i < 7; i++) {
            ColumnConstraints col = new ColumnConstraints();
            col.setPercentWidth(100.0 / 7);
            col.setHalignment(HPos.CENTER);
            grilla.getColumnConstraints().add(col);
        }
        String[] dias = {"L", "M", "M", "J", "V", "S", "D"};
        for (int i = 0; i < 7; i++) {
            grilla.add(Componentes.literal(dias[i], "cal-wd"), i, 0);
        }
        int desfase = mes.atDay(1).getDayOfWeek().getValue() - DayOfWeek.MONDAY.getValue();
        for (int d = 1; d <= mes.lengthOfMonth(); d++) {
            LocalDate fecha = mes.atDay(d);
            Button dia = new Button(String.valueOf(d));
            dia.getStyleClass().add("cal-day");
            if (fecha.equals(hoy)) {
                dia.getStyleClass().add("today");
            }
            if (fecha.equals(seleccion)) {
                dia.getStyleClass().add("on");
            }
            dia.setDisable(fecha.isBefore(hoy) || fecha.isAfter(maximo));
            Componentes.accesible(dia, Fechas.largo(fecha) + (fecha.equals(hoy) ? ", hoy" : ""));
            dia.setMaxWidth(Double.MAX_VALUE);
            dia.setOnAction(e -> {
                seleccion = fecha;
                dibujar();
            });
            dia.setOnMouseClicked(e -> {
                if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2) {
                    seleccion = fecha;
                    confirmar();
                }
            });
            int posicion = desfase + d - 1;
            grilla.add(dia, posicion % 7, 1 + posicion / 7);
        }
        TextFlow elegido = Componentes.flujo(Componentes.texto("Seleccionado:", "hint"), Componentes.texto(" "),
                Componentes.texto(Fechas.largo(seleccion), "strong"));
        VBox.setMargin(elegido, new Insets(12, 0, 0, 0));
        cuerpo.getChildren().setAll(cabecera, grilla, elegido);
    }
}
