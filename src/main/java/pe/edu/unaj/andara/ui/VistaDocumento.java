package pe.edu.unaj.andara.ui;

import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import org.kordamp.ikonli.javafx.FontIcon;
import pe.edu.unaj.andara.service.DocumentoGeneradoService.DocumentoOficial;
import pe.edu.unaj.andara.service.QrService;

import java.io.ByteArrayInputStream;

public final class VistaDocumento {

    private VistaDocumento() {
    }

    private static Text t(String s, boolean negrita) {
        Text x = new Text(s);
        x.getStyleClass().add("dg-texto");
        if (negrita) {
            x.setStyle("-fx-font-weight: 700;");
        }
        return x;
    }

    public static StackPane crear(DocumentoOficial d) {
        if ("carne".equals(d.plantilla())) {
            return carne(d);
        }
        VBox textos = new VBox(Componentes.literal("Universidad Nacional de Juliaca", "strong"),
                Componentes.literal("Dirección de Servicios Académicos · Andara", "hint"));
        HBox cabecera = new HBox(12, Componentes.logo(40), textos);
        cabecera.getStyleClass().add("dg-head");
        Label titulo = Componentes.literalParrafo(d.titulo(), "dg-titulo");
        titulo.setMaxWidth(Double.MAX_VALUE);
        titulo.setAlignment(Pos.CENTER);
        TextFlow cuerpo = new TextFlow(t("La Dirección de Servicios Académicos hace constar que ", false),
                t(d.solicitante().getNombre(), true), t(", identificado(a) con DNI N.° ", false), t(d.solicitante().getDni(), true));
        if (d.conCodigo()) {
            cuerpo.getChildren().addAll(t(" y código universitario N.° ", false), t(d.solicitante().getCodigo(), true));
        }
        cuerpo.getChildren().add(t(", " + d.rol() + ", sede " + d.sede() + ".", false));
        cuerpo.setLineSpacing(4);
        VBox hoja = new VBox(14, cabecera, titulo, cuerpo);
        if (!d.semestres().isEmpty()) {
            GridPane tabla = new GridPane();
            for (double a : new double[]{50, 25, 25}) {
                ColumnConstraints c = new ColumnConstraints();
                c.setPercentWidth(a);
                tabla.getColumnConstraints().add(c);
            }
            tabla.getColumnConstraints().get(1).setHalignment(HPos.RIGHT);
            tabla.getColumnConstraints().get(2).setHalignment(HPos.RIGHT);
            String[] cab = {"SEMESTRE", "CRÉDITOS", "PROMEDIO"};
            for (int i = 0; i < 3; i++) {
                tabla.add(Componentes.literal(cab[i], "inv-th"), i, 0);
            }
            for (int i = 0; i < d.semestres().size(); i++) {
                var s = d.semestres().get(i);
                tabla.add(Componentes.literal(s.periodo()), 0, i + 1);
                tabla.add(Componentes.literal(String.valueOf(s.creditos())), 1, i + 1);
                tabla.add(Componentes.literal(s.promedio()), 2, i + 1);
            }
            tabla.setVgap(8);
            hoja.getChildren().add(tabla);
        }
        if ("certificado".equals(d.plantilla())) {
            hoja.getChildren().add(new TextFlow(t("Se certifica que ha aprobado las asignaturas correspondientes a los semestres 2024-2 al 2026-1, conforme al detalle registrado en el sistema académico.", false)));
        }
        hoja.getChildren().add(new TextFlow(t("Se expide el presente documento a solicitud del interesado(a) para fines de ", false),
                t(d.uso(), true), t(".", false)));
        Label fecha = Componentes.literal("Juliaca, " + d.fecha());
        HBox filaFecha = new HBox(Componentes.espacio(), fecha);
        filaFecha.setPadding(new Insets(6, 0, 0, 0));
        Region linea = new Region();
        linea.setMinSize(180, 1);
        linea.setMaxSize(180, 1);
        linea.setStyle("-fx-background-color: -color-text;");
        VBox firma = new VBox(4, linea, Componentes.literal("Firma digital", "small"), Componentes.literal("Director(a) de Servicios Académicos", "hint"));
        firma.setAlignment(Pos.CENTER);
        ImageView qr = new ImageView(QrService.imagen(d.contenidoQr(), 192));
        qr.setFitWidth(96);
        qr.setFitHeight(96);
        VBox qrCaja = new VBox(4, qr, Componentes.literal("Verificación: " + d.verificacion(), "hint"));
        qrCaja.setAlignment(Pos.CENTER);
        HBox pie = new HBox(firma, Componentes.espacio(), qrCaja);
        pie.setAlignment(Pos.BOTTOM_LEFT);
        pie.setPadding(new Insets(20, 0, 0, 0));
        hoja.getChildren().addAll(filaFecha, pie);
        hoja.getStyleClass().add("docgen");
        StackPane cont = new StackPane(hoja);
        cont.getStyleClass().add("sheet");
        Label marca = Componentes.literal("DEMOSTRACIÓN", "watermark");
        marca.setMouseTransparent(true);
        StackPane.setAlignment(marca, Pos.CENTER);
        cont.getChildren().add(marca);
        return cont;
    }

    private static StackPane carne(DocumentoOficial d) {
        HBox top = new HBox(10, Componentes.logo(30), new VBox(Componentes.literal("UNAJ", "strong"),
                Componentes.literal("Carné universitario 2026-2", "small")));
        top.setPadding(new Insets(14, 16, 14, 16));
        top.setStyle("-fx-background-color: -color-primary; -fx-background-radius: 18 18 0 0;");
        top.getChildren().get(1).setStyle("-fx-text-fill: white;");
        ((VBox) top.getChildren().get(1)).getChildren().forEach(n -> n.setStyle("-fx-text-fill: white;"));
        StackPane foto = new StackPane();
        foto.setMinSize(96, 118);
        foto.setMaxSize(96, 118);
        foto.setStyle("-fx-background-color: -color-fill; -fx-background-radius: 10;");
        if (d.foto() != null && d.foto().length > 0) {
            ImageView iv = new ImageView(new Image(new ByteArrayInputStream(d.foto())));
            iv.setFitWidth(96);
            iv.setFitHeight(118);
            iv.setPreserveRatio(false);
            Rectangle r = new Rectangle(96, 118);
            r.setArcWidth(20);
            r.setArcHeight(20);
            iv.setClip(r);
            foto.getChildren().add(iv);
        } else {
            foto.getChildren().add(new FontIcon("fth-user"));
        }
        VBox datos = new VBox(2, Componentes.literal(d.solicitante().getNombre(), "strong"),
                Componentes.literal("DNI " + d.solicitante().getDni(), "small"),
                Componentes.literal("Código " + (d.solicitante().getCodigo() == null ? "—" : d.solicitante().getCodigo()), "small"),
                Componentes.literal("EP " + d.escuela(), "small"), Componentes.literal("Sede " + d.sede(), "small"));
        HBox cuerpo = new HBox(16, foto, datos);
        cuerpo.setPadding(new Insets(16));
        HBox pie = new HBox(Componentes.literal("Válido hasta 31/03/2027", "hint"), Componentes.espacio(), Componentes.literal(d.verificacion(), "hint"));
        pie.setPadding(new Insets(10, 16, 10, 16));
        pie.setStyle("-fx-border-color: -color-border transparent transparent transparent;");
        VBox tarjeta = new VBox(top, cuerpo, pie);
        tarjeta.setMaxWidth(420);
        tarjeta.setStyle("-fx-background-color: white; -fx-background-radius: 18; -fx-border-color: -color-border; -fx-border-radius: 18;");
        StackPane cont = new StackPane(tarjeta);
        Label marca = Componentes.literal("DEMOSTRACIÓN", "watermark", "sm");
        marca.setMouseTransparent(true);
        cont.getChildren().add(marca);
        return cont;
    }

    public static LinearGradient degradadoCarne() {
        return new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE, new Stop(0, Color.web("#0A63C9")),
                new Stop(0.55, Color.web("#0071E3")), new Stop(1, Color.web("#2B8CF0")));
    }
}
