package pe.edu.unaj.andara.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import org.kordamp.ikonli.javafx.FontIcon;
import pe.edu.unaj.andara.model.Persona;

import java.io.ByteArrayInputStream;

public final class TarjetaCarne {

    private TarjetaCarne() {
    }

    public static StackPane crear(Persona p, boolean vigente, byte[] foto) {
        HBox top = new HBox(10, Componentes.logo(30), new VBox(Componentes.literal("UNAJ", "strong"),
                Componentes.etiqueta("Carné universitario digital", "small")), Componentes.espacio(),
                Componentes.literal("2026-2", "dcard-sem"));
        top.setAlignment(Pos.CENTER_LEFT);
        StackPane marcoFoto = new StackPane();
        marcoFoto.getStyleClass().add("dcard-foto");
        if (foto != null && foto.length > 0) {
            ImageView iv = new ImageView(new Image(new ByteArrayInputStream(foto)));
            iv.setFitWidth(84);
            iv.setFitHeight(104);
            Rectangle r = new Rectangle(84, 104);
            r.setArcWidth(28);
            r.setArcHeight(28);
            iv.setClip(r);
            marcoFoto.getChildren().add(iv);
        } else {
            marcoFoto.getChildren().add(Componentes.literal(p.getIniciales(), "dcard-ini"));
        }
        VBox datos = new VBox(1, Componentes.literal(p.getNombre(), "dcard-nombre"),
                Componentes.etiqueta("Código " + p.getCodigo(), "dcard-dato"), Componentes.etiqueta("DNI " + p.getDni(), "dcard-dato"),
                Componentes.literal("EP " + p.getEscuela().getNombre(), "dcard-dato"),
                Componentes.etiqueta("Sede " + p.getSede().getNombre(), "dcard-dato"));
        HBox cuerpo = new HBox(16, marcoFoto, datos);
        cuerpo.setAlignment(Pos.CENTER_LEFT);
        Label estado = Componentes.etiqueta(vigente ? "Vigente" : "Matrícula pendiente", "dc-state", vigente ? "ok" : "warn");
        estado.setGraphic(new FontIcon(vigente ? "fth-check" : "fth-clock"));
        HBox pie = new HBox(estado, Componentes.espacio(), Componentes.etiqueta("Válido hasta 31/03/2027", "small"));
        pie.setAlignment(Pos.CENTER_LEFT);
        VBox tarjeta = new VBox(top, Componentes.espacioV(), cuerpo, Componentes.espacioV(), pie);
        tarjeta.getStyleClass().add("dcard");
        tarjeta.setMaxWidth(520);
        tarjeta.setPrefHeight(328);
        tarjeta.setMinHeight(300);
        StackPane cont = new StackPane(tarjeta);
        return cont;
    }
}
