package pe.edu.unaj.andara.controller;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.*;
import lombok.RequiredArgsConstructor;
import org.kordamp.ikonli.javafx.FontIcon;
import org.springframework.stereotype.Component;
import pe.edu.unaj.andara.model.*;
import pe.edu.unaj.andara.service.*;
import pe.edu.unaj.andara.ui.*;
import pe.edu.unaj.andara.util.Montos;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class FavoritosController extends VistaBase {

    private final FavoritoService favoritos;
    private final CatalogoService catalogo;
    private final DocumentoService documentos;
    private final AccionesDocumento accionesDocumento;
    private final AccionesEquipo accionesEquipo;
    private final AccionesEspacio accionesEspacio;
    private final ReservaService reservas;

    private String pestana = "docs";

    @Override
    protected List<Node> construir() {
        Set<String> favDocs = favoritos.codigos(TipoFavorito.DOCUMENTO);
        Set<String> favMios = favoritos.codigos(TipoFavorito.MI_DOCUMENTO);
        Set<String> favEq = favoritos.codigos(TipoFavorito.EQUIPO);
        Set<String> favEsp = favoritos.codigos(TipoFavorito.ESPACIO);
        List<Documento> docs = catalogo.documentos().stream().filter(d -> favDocs.contains(d.getCodigo())).toList();
        List<DocumentoPersonal> mios = documentos.misDocumentos().stream().filter(d -> favMios.contains(d.getCodigo())).toList();
        List<Equipo> eqs = catalogo.equipos().stream().filter(e -> favEq.contains(e.getCodigo())).toList();
        List<Espacio> esps = catalogo.espacios().stream().filter(e -> favEsp.contains(e.getCodigo())).toList();
        Segmentado tabs = new Segmentado(true).opcion("docs", "Documentación", null, (long) (docs.size() + mios.size()))
                .opcion("eq", "Equipos", null, (long) eqs.size()).opcion("areas", "Áreas", null, (long) esps.size()).seleccionar(pestana);
        tabs.alCambiar(v -> {
            pestana = v;
            redibujar();
        });
        tabs.setMaxWidth(560);
        Componentes.accesible(tabs, "Tipo de favorito");
        HBox centro = new HBox(tabs);
        centro.setAlignment(Pos.CENTER);
        VBox.setMargin(centro, new Insets(0, 0, 32, 0));
        HBox.setHgrow(tabs, Priority.ALWAYS);
        Node cuerpo;
        if ("docs".equals(pestana)) {
            if (docs.isEmpty() && mios.isEmpty()) {
                cuerpo = vacio("Aún no tienes documentación favorita", "Marca la estrella en cualquier documento para guardarlo aquí.", "documentos", "Ir a Documentos", "fth-file-text");
            } else {
                VBox v = new VBox(32);
                if (!docs.isEmpty()) {
                    Rejilla g = new Rejilla(2, 360);
                    docs.forEach(d -> g.getChildren().add(accionesDocumento.tarjeta(d, this::redibujar)));
                    v.getChildren().add(new VBox(12, Componentes.mayus("Documentos y formatos", "label-caps"), g));
                }
                if (!mios.isEmpty()) {
                    List<Node> filas = new ArrayList<>();
                    mios.forEach(d -> filas.add(accionesDocumento.filaPersonal(d, this::redibujar)));
                    v.getChildren().add(new VBox(12, Componentes.mayus("Mis documentos", "label-caps"), Componentes.lista(filas)));
                }
                cuerpo = v;
            }
        } else if ("eq".equals(pestana)) {
            if (eqs.isEmpty()) {
                cuerpo = vacio("Aún no tienes equipos favoritos", "Marca la estrella en un equipo del catálogo para tenerlo a mano.", "equipos", "Ir a Equipos", "fth-box");
            } else {
                Rejilla g = new Rejilla(3, 260);
                eqs.forEach(e -> g.getChildren().add(tarjetaEquipo(e)));
                cuerpo = g;
            }
        } else {
            if (esps.isEmpty()) {
                cuerpo = vacio("Aún no tienes áreas favoritas", "Marca la estrella en un aula, laboratorio o auditorio para reservarlo más rápido.", "reservas", "Ir a Reservas", "fth-calendar");
            } else {
                Rejilla g = new Rejilla(4, 220);
                esps.forEach(e -> g.getChildren().add(tarjetaEspacio(e)));
                cuerpo = g;
            }
        }
        VBox s = Componentes.seccion(centro, cuerpo);
        s.getStyleClass().add("sin-arriba");
        return List.of(Componentes.hero(null, "Favoritos", "Tu documentación, equipos y áreas guardados en un solo lugar.", null), s);
    }

    private VBox vacio(String titulo, String sub, String ruta, String boton, String icono) {
        return Componentes.vacio("fth-star", titulo, sub, Componentes.botonSm(boton, "btn-secondary", icono, e -> ir(ruta)));
    }

    private StackPane miniatura(String icono) {
        StackPane t = new StackPane(new FontIcon(icono));
        t.getStyleClass().add("thumb");
        t.setMinHeight(140);
        return t;
    }

    private VBox tarjetaEquipo(Equipo e) {
        boolean ok = e.isDisponibleAhora();
        Button estrella = Componentes.estrella(true, e.getNombre(), () -> {
            favoritos.alternar(TipoFavorito.EQUIPO, e.getCodigo());
            notificador.exito("Quitado de favoritos.");
            redibujar();
        });
        StackPane arriba = new StackPane(miniatura(Ilustraciones.iconoCategoria(e)), estrella);
        StackPane.setAlignment(estrella, Pos.TOP_RIGHT);
        StackPane.setMargin(estrella, new Insets(10));
        HBox cab = new HBox(Componentes.mayus(e.getRotulo(), "label-caps"), Componentes.espacio(), accionesEquipo.estado(e, true));
        cab.setAlignment(Pos.CENTER_LEFT);
        VBox v = new VBox(8, arriba, cab, Componentes.parrafo(e.getNombre(), "h3", "sm"), Componentes.parrafo(e.getEspecificaciones(), "eq-specs"),
                Componentes.metaLinea("fth-map-pin", "Sede " + e.getSede().getNombre() + " · " + e.getUbicacion()));
        if (e.getEscuela() != null) {
            v.getChildren().add(Componentes.metaLinea("fth-book-open", "Uso preferente: EP " + e.getEscuela().getNombre()));
        }
        Button pedir = Componentes.bloque(Componentes.botonSm(e.isPorTurno() ? "Reservar turno" : ok ? "Solicitar préstamo" : "Sin unidades",
                ok ? "btn-primary" : "btn-secondary", null, ev -> accionesEquipo.solicitar(e, this::redibujar, null, null)));
        pedir.setDisable(!ok && !e.isPorTurno());
        v.getChildren().addAll(Componentes.espacioV(), pedir);
        v.getStyleClass().add("eq-card");
        return v;
    }

    private VBox tarjetaEspacio(Espacio s) {
        String estado = reservas.estadoGaleria(s, null, null);
        Button estrella = Componentes.estrella(true, s.getNombre(), () -> {
            favoritos.alternar(TipoFavorito.ESPACIO, s.getCodigo());
            notificador.exito("Quitado de favoritos.");
            redibujar();
        });
        String icono = switch (s.getTipo()) {
            case LABORATORIO -> "fth-droplet";
            case TALLER -> "fth-tool";
            case SALA -> "fth-list";
            default -> "fth-image";
        };
        StackPane arriba = new StackPane(miniatura(icono), estrella);
        StackPane.setAlignment(estrella, Pos.TOP_RIGHT);
        StackPane.setMargin(estrella, new Insets(10));
        HBox meta = new HBox(Componentes.etiqueta("Capacidad: " + Montos.miles(s.getCapacidad()) + " personas", "muted", "small"), Componentes.espacio(),
                Componentes.pill(estado));
        meta.setAlignment(Pos.CENTER_LEFT);
        boolean ok = "Disponible".equals(estado);
        Button reservar = Componentes.bloque(Componentes.botonSm(ok ? "Reservar" : "No disponible", ok ? "btn-primary" : "btn-secondary", null,
                e -> accionesEspacio.reservar(s, AccionesEspacio.Precarga.VACIA, this::redibujar)));
        reservar.setDisable(!ok);
        VBox v = new VBox(12, arriba, Componentes.literalParrafo(s.getNombre(), "h3", "sm"),
                Componentes.metaLinea("fth-map-pin", "Sede " + s.getSede().getNombre() + " · " + s.getEdificio()),
                Componentes.metaLinea("fth-book-open", s.getEscuela() != null ? "EP " + s.getEscuela().getNombre() : "Uso general"), meta,
                Componentes.espacioV(), reservar);
        v.getStyleClass().add("space-card");
        return v;
    }
}
