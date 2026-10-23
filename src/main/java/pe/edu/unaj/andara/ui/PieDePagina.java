package pe.edu.unaj.andara.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import lombok.RequiredArgsConstructor;
import org.kordamp.ikonli.javafx.FontIcon;
import org.springframework.stereotype.Component;
import pe.edu.unaj.andara.config.DatosInstitucion;
import pe.edu.unaj.andara.model.Rol;
import pe.edu.unaj.andara.service.SesionService;
import pe.edu.unaj.andara.util.Portapapeles;

import java.util.List;

@Component
@RequiredArgsConstructor
public class PieDePagina {

    private final Enlaces enlaces;
    private final Notificador notificador;
    private final DatosInstitucion institucion;
    private final SesionService sesion;

    public VBox crear() {
        Rol rol = sesion.rol();
        List<SesionService.Ruta> rutas = SesionService.rutas(rol);
        int corte = rol == Rol.ADMINISTRADOR ? 3 : 4;

        VBox marca = new VBox(2, Componentes.literal("Andara", "brand-name"), Componentes.literal("UNAJ", "brand-sub"),
                Componentes.literal("tus trámites más simples", "brand-sub"));
        HBox marcaFila = new HBox(10, Componentes.logo(34), marca);
        marcaFila.setAlignment(Pos.TOP_LEFT);
        Button youtube = social("fth-youtube", "YouTube de la UNAJ (abre en el navegador)", () -> enlaces.abrir(institucion.getUrlYoutube()));
        Button facebook = social("fth-facebook", "Facebook de la UNAJ (abre en el navegador)", () -> enlaces.abrir(institucion.getUrlFacebook()));
        Button telefono = social("fth-phone", "Ver número de la central telefónica", this::modalTelefono);
        Label numero = Componentes.literal(institucion.getTelefono(), "foot-phone");
        HBox redes = new HBox(youtube, facebook, telefono, numero);
        redes.getStyleClass().add("socials");
        VBox colMarca = new VBox(marcaFila, redes);

        GridPane grid = new GridPane();
        grid.getStyleClass().add("pie-grid");
        int columnas = rol == Rol.ADMINISTRADOR ? 3 : 4;
        for (int i = 0; i < columnas; i++) {
            ColumnConstraints c = new ColumnConstraints();
            c.setPercentWidth(i == 0 ? 34 : 66.0 / (columnas - 1));
            grid.getColumnConstraints().add(c);
        }
        grid.add(colMarca, 0, 0);
        grid.add(columna("Servicios", rutas.subList(0, Math.min(corte, rutas.size()))), 1, 0);
        grid.add(columna("Seguimiento", rutas.subList(Math.min(corte, rutas.size()), rutas.size())), 2, 0);
        if (rol != Rol.ADMINISTRADOR) {
            VBox soporte = new VBox(Componentes.etiqueta("Soporte", "pie-col-titulo"), enlace("Centro de ayuda", "ayuda"),
                    enlace("Contacto", "contacto"));
            soporte.getStyleClass().add("pie-col");
            grid.add(soporte, 3, 0);
        }
        grid.setMaxWidth(1168);
        HBox abajo = new HBox(Componentes.etiqueta("© 2026 Andara · UNAJ"));
        abajo.getStyleClass().add("pie-bottom");
        abajo.setMaxWidth(1120);
        VBox interior = new VBox(grid, abajo);
        interior.setAlignment(Pos.TOP_CENTER);
        interior.setMaxWidth(1168);
        VBox pie = new VBox(interior);
        pie.setAlignment(Pos.TOP_CENTER);
        pie.getStyleClass().add("pie");
        VBox.setMargin(pie, new Insets(48, 0, 0, 0));
        pie.setMaxWidth(Double.MAX_VALUE);
        return pie;
    }

    private VBox columna(String titulo, List<SesionService.Ruta> rutas) {
        VBox v = new VBox(Componentes.etiqueta(titulo, "pie-col-titulo"));
        v.getStyleClass().add("pie-col");
        rutas.forEach(r -> v.getChildren().add(enlace(r.etiqueta(), r.clave())));
        return v;
    }

    private Label enlace(String texto, String ruta) {
        Label l = Componentes.etiqueta(texto, "pie-link");
        l.setFocusTraversable(true);
        l.setOnMouseClicked(e -> Navegacion.ir(l, ruta));
        l.setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ENTER) {
                Navegacion.ir(l, ruta);
            }
        });
        return l;
    }

    private Button social(String icono, String descripcion, Runnable accion) {
        Button b = new Button(null, new FontIcon(icono));
        b.getStyleClass().add("social");
        Componentes.accesible(b, descripcion);
        Componentes.ayuda(b, descripcion);
        b.setOnAction(e -> accion.run());
        return b;
    }

    public void modalTelefono() {
        StackPane icono = new StackPane(new FontIcon("fth-phone"));
        icono.getStyleClass().add("icono-circulo");
        Label nombre = Componentes.mayus(institucion.getNombre(), "label-caps");
        Label numero = Componentes.literal(institucion.getTelefono(), "dato-grande");
        Label horario = Componentes.etiqueta("Lunes a viernes de 08:30 a 18:00 hrs", "muted", "small");
        VBox cuerpo = new VBox(8, icono, nombre, numero, horario);
        cuerpo.setAlignment(Pos.CENTER);
        cuerpo.setPadding(new Insets(8, 0, 4, 0));
        notificador.modal("Central telefónica", cuerpo, false,
                Componentes.boton("Cerrar", "btn-secondary", null, e -> notificador.cerrarModal()),
                Componentes.boton("Copiar número", "btn-primary", null, e -> {
                    Portapapeles.copiar(institucion.getTelefono());
                    notificador.exito("Número copiado.");
                }));
    }
}
