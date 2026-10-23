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
import pe.edu.unaj.andara.model.Documento;
import pe.edu.unaj.andara.model.DocumentoPersonal;
import pe.edu.unaj.andara.model.TipoFavorito;
import pe.edu.unaj.andara.service.CatalogoService;
import pe.edu.unaj.andara.service.DocumentoGeneradoService;
import pe.edu.unaj.andara.service.FavoritoService;
import pe.edu.unaj.andara.service.SesionService;
import pe.edu.unaj.andara.util.Fechas;
import pe.edu.unaj.andara.util.Portapapeles;

@Component
@RequiredArgsConstructor
public class AccionesDocumento {

    private final Notificador notificador;
    private final Descargas descargas;
    private final DocumentoGeneradoService generador;
    private final FavoritoService favoritos;
    private final CatalogoService catalogo;
    private final SesionService sesion;
    private final DatosInstitucion institucion;

    public String meta(Documento d) {
        return d.getTipo() + " · " + catalogo.etiquetaDependencia(d.getDependencia()) + " · " + Fechas.dmy(d.getFecha()) + " · "
                + d.getPaginas() + " página" + (d.getPaginas() > 1 ? "s" : "");
    }

    public HBox tarjeta(Documento d, Runnable alCambiar) {
        VBox miniatura = new VBox();
        miniatura.getStyleClass().add("doc-thumb");
        for (double w : new double[]{1, 0.7, 1, 0.5}) {
            Region linea = new Region();
            linea.getStyleClass().add("linea");
            linea.setMaxWidth(66 * w);
            miniatura.getChildren().add(linea);
        }
        miniatura.getChildren().add(Componentes.literal("PDF", "pdf"));
        Button vista = Componentes.botonSm("Vista previa", "btn-secondary", "fth-eye", e -> vistaPrevia(d.getNombre(), meta(d)));
        Button bajar = Componentes.botonSm("Descargar", "btn-secondary", "fth-download", e -> descargar(d.getNombre(), meta(d)));
        Button compartir = Componentes.botonSm("Compartir", "btn-tertiary", "fth-link", e -> compartir(institucion.getUrlDocumentos() + "/documentos/" + d.getCodigo()));
        FlowPane acciones = new FlowPane(vista, bajar, compartir);
        acciones.getStyleClass().add("doc-actions");
        VBox cuerpo = new VBox(4, Componentes.mayus(d.getTipo(), "label-caps"), Componentes.parrafo(d.getNombre(), "h3", "sm"),
                Componentes.parrafo(d.getDescripcion(), "muted", "small"),
                Componentes.literalParrafo(catalogo.etiquetaDependencia(d.getDependencia()) + " · " + Fechas.dmy(d.getFecha()), "hint"), acciones);
        HBox.setHgrow(cuerpo, Priority.ALWAYS);
        boolean fav = favoritos.es(TipoFavorito.DOCUMENTO, d.getCodigo());
        Button estrella = Componentes.estrella(fav, d.getNombre(), () -> {
            boolean ahora = favoritos.alternar(TipoFavorito.DOCUMENTO, d.getCodigo());
            notificador.exito(ahora ? "Agregado a favoritos." : "Quitado de favoritos.");
            alCambiar.run();
        });
        estrella.getStyleClass().add("plano");
        HBox tarjeta = new HBox(16, miniatura, cuerpo, estrella);
        tarjeta.getStyleClass().add("doc-card");
        return tarjeta;
    }

    public HBox filaPersonal(DocumentoPersonal d, Runnable alCambiar) {
        HBox principal = new HBox(12, Componentes.icono("fth-file-text", "doc-icono"), Componentes.literal(d.getNombre(), "row-title"));
        principal.setAlignment(Pos.CENTER_LEFT);
        String meta = "Emitido a nombre de " + sesion.estudiante().getNombre() + " · Código " + sesion.estudiante().getCodigo();
        boolean fav = favoritos.es(TipoFavorito.MI_DOCUMENTO, d.getCodigo());
        Button estrella = Componentes.estrella(fav, d.getNombre(), () -> {
            boolean ahora = favoritos.alternar(TipoFavorito.MI_DOCUMENTO, d.getCodigo());
            notificador.exito(ahora ? "Agregado a favoritos." : "Quitado de favoritos.");
            alCambiar.run();
        });
        estrella.getStyleClass().add("plano");
        return Componentes.fila(principal,
                Componentes.botonSm("Compartir", "btn-tertiary", "fth-link", e -> compartir(institucion.getUrlDocumentos() + "/mis-documentos/" + d.getCodigo())),
                Componentes.botonSm("Descargar", "btn-secondary", "fth-download", e -> descargar(d.getNombre(), meta)),
                Componentes.botonSm("Vista previa", "btn-secondary", "fth-eye", e -> vistaPrevia(d.getNombre(), meta)), estrella);
    }

    public void vistaPrevia(String nombre, String meta) {
        VBox hoja = new VBox(10);
        hoja.getStyleClass().add("vista-doc");
        HBox arriba = new HBox(Componentes.literal("UNAJ · Andara", "strong"), Componentes.espacio(), Componentes.literal("PDF", "tag"));
        Region sep = new Region();
        sep.getStyleClass().add("vista-sep");
        hoja.getChildren().addAll(arriba, sep, Componentes.literalParrafo(nombre, "h3"));
        if (meta != null) {
            hoja.getChildren().add(Componentes.parrafo(meta, "muted", "small"));
        }
        for (double w : new double[]{1, 0.94, 0.98, 0.72, 0, 1, 0.88, 0.96, 0.6}) {
            Region linea = new Region();
            if (w > 0) {
                linea.getStyleClass().add("linea");
                linea.maxWidthProperty().bind(hoja.widthProperty().subtract(56).multiply(w));
            } else {
                linea.setMinHeight(8);
            }
            hoja.getChildren().add(linea);
        }
        VBox cuerpo = new VBox(12, hoja, Componentes.parrafo("Vista previa simulada. En el sistema real aquí se mostraría el PDF.", "hint"));
        notificador.modal(nombre, cuerpo, true, Componentes.boton("Cerrar", "btn-secondary", null, e -> notificador.cerrarModal()),
                Componentes.boton("Descargar", "btn-primary", "fth-download", e -> descargar(nombre, meta)));
    }

    public void descargar(String nombre, String meta) {
        descargas.guardarPdf(nombre, () -> generador.pdfSimple(nombre, meta));
    }

    public void compartir(String url) {
        Portapapeles.copiar(url);
        notificador.exito("Enlace del documento copiado.");
    }

    public static Label icono(String literal) {
        Label l = new Label(null, new FontIcon(literal));
        l.setPadding(new Insets(0));
        return l;
    }
}
