package pe.edu.unaj.andara.ui;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

import java.util.function.IntConsumer;

public final class Paginador {

    private Paginador() {
    }

    public static HBox crear(int pagina, int paginas, int total, int porPagina, String unidad, IntConsumer ir) {
        int desde = total == 0 ? 0 : (pagina - 1) * porPagina + 1;
        int hasta = Math.min(pagina * porPagina, total);
        Label resumen = Componentes.etiqueta("Mostrando " + desde + " - " + hasta + " de " + total + " " + unidad, "hint");
        HBox botones = new HBox();
        botones.getStyleClass().add("pager-btns");
        Button anterior = Componentes.botonSm("‹ Anterior", "btn-secondary", null, e -> ir.accept(pagina - 1));
        anterior.setDisable(pagina <= 1);
        botones.getChildren().add(anterior);
        for (int i = 1; i <= paginas; i++) {
            final int p = i;
            Button b = Componentes.botonSm(null, "btn-secondary", null, e -> ir.accept(p));
            b.setText(String.valueOf(i));
            b.getStyleClass().add("btn-icon");
            if (i == pagina) {
                b.getStyleClass().add("actual");
            }
            botones.getChildren().add(b);
        }
        Button siguiente = Componentes.botonSm("Siguiente ›", "btn-secondary", null, e -> ir.accept(pagina + 1));
        siguiente.setDisable(pagina >= paginas);
        botones.getChildren().add(siguiente);
        HBox pager = new HBox(resumen, Componentes.espacio(), botones);
        pager.getStyleClass().add("pager");
        return pager;
    }
}
