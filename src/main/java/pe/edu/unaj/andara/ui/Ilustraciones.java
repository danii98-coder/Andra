package pe.edu.unaj.andara.ui;

import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import org.kordamp.ikonli.javafx.FontIcon;
import pe.edu.unaj.andara.model.Equipo;
import pe.edu.unaj.andara.model.Espacio;
import pe.edu.unaj.andara.model.TipoEspacio;
import pe.edu.unaj.andara.util.Hash;

import java.util.Map;

public final class Ilustraciones {

    private static final Color P = Color.web("#0071E3");
    private static final Color SF = Color.web("#E8F2FD");
    private static final Color W = Color.WHITE;
    private static final Color B = Color.web("#D2D2D7");
    private static final Color T = Color.web("#1D1D1F");

    public static final Map<String, String> ICONOS_CATEGORIA = Map.of("speaker", "fth-speaker", "music", "fth-music",
            "ball", "fth-dribbble", "monitor", "fth-monitor", "lab", "fth-droplet");

    private Ilustraciones() {
    }

    public static String iconoCategoria(Equipo e) {
        return ICONOS_CATEGORIA.getOrDefault(e.getCategoria().getIcono(), "fth-box");
    }

    private static Rectangle rect(double x, double y, double w, double h, double r, Color c, double op) {
        Rectangle re = new Rectangle(x, y, w, h);
        re.setArcWidth(r * 2);
        re.setArcHeight(r * 2);
        re.setFill(c);
        re.setOpacity(op);
        return re;
    }

    public static Group espacio(Espacio s) {
        long h = Hash.numero(s.getCodigo());
        Group g = new Group();
        g.getChildren().add(rect(0, 0, 400, 250, 0, SF, 1));
        TipoEspacio tipo = s.getTipo();
        if (tipo == TipoEspacio.AUDITORIO) {
            int filas = s.getCapacidad() >= 1000 ? 7 : s.getCapacidad() >= 250 ? 5 : 4;
            Path arco = new Path(new MoveTo(60, 70), new QuadCurveTo(200, 30, 340, 70), new LineTo(340, 92),
                    new QuadCurveTo(200, 56, 60, 92), new ClosePath());
            arco.setFill(P);
            arco.setStroke(null);
            arco.setOpacity(0.9);
            g.getChildren().addAll(arco, rect(120, 86, 160, 16, 8, W, 1));
            for (int r = 0; r < filas; r++) {
                double y = 118 + r * (110.0 / filas);
                double w = 150 + r * (220.0 / filas);
                int n = (int) Math.round(8 + r * 2.6);
                for (int i = 0; i < n; i++) {
                    double x = 200 - w / 2 + i * (w / (n - 1));
                    boolean azul = (i + r + h) % 9 == 0;
                    g.getChildren().add(rect(x - 5, y, 10, 70.0 / filas, 3, azul ? P : T, azul ? 0.9 : 0.14 + r * 0.04));
                }
            }
        } else if (tipo == TipoEspacio.LABORATORIO) {
            g.getChildren().add(rect(0, 190, 400, 60, 0, W, 0.7));
            for (int r = 0; r < 2; r++) {
                for (int i = 0; i < 4; i++) {
                    double x = 40 + i * 86;
                    double y = 58 + r * 82;
                    g.getChildren().addAll(rect(x, y, 64, 42, 6, T, 0.85),
                            rect(x + 4, y + 4, 56, 30, 3, (i + r + h) % 5 == 0 ? P : W, 1),
                            rect(x + 28, y + 42, 8, 10, 0, T, 0.6), rect(x - 6, y + 52, 76, 6, 3, B, 1));
                }
            }
        } else if (tipo == TipoEspacio.AULA) {
            g.getChildren().add(rect(80, 26, 240, 60, 6, T, 0.85));
            Line l1 = new Line(100, 50, 180, 50);
            Line l2 = new Line(100, 64, 220, 64);
            for (Line l : new Line[]{l1, l2}) {
                l.setStroke(W);
                l.setStrokeWidth(5);
                l.setStrokeLineCap(StrokeLineCap.ROUND);
                l.setOpacity(0.8);
            }
            g.getChildren().addAll(l1, l2);
            for (int r = 0; r < 3; r++) {
                for (int i = 0; i < 5; i++) {
                    Rectangle carpeta = rect(60 + i * 58, 108 + r * 44, 42, 24, 5, (i * 3 + r + h) % 7 == 0 ? P : W, 1);
                    carpeta.setStroke(B);
                    g.getChildren().add(carpeta);
                }
            }
        } else if (tipo == TipoEspacio.SALA) {
            Ellipse mesa = new Ellipse(200, 140, 110, 54);
            mesa.setFill(W);
            mesa.setStroke(B);
            Ellipse centro = new Ellipse(200, 140, 60, 26);
            centro.setFill(P);
            centro.setOpacity(0.18);
            g.getChildren().addAll(mesa, centro);
            for (int i = 0; i < 10; i++) {
                double a = i / 10.0 * Math.PI * 2;
                Circle silla = new Circle(200 + Math.cos(a) * 140, 140 + Math.sin(a) * 78, 13);
                boolean azul = i == h % 10;
                silla.setFill(azul ? P : T);
                silla.setOpacity(azul ? 1 : 0.18);
                g.getChildren().add(silla);
            }
        } else {
            g.getChildren().addAll(rect(40, 150, 320, 18, 6, T, 0.8), rect(60, 168, 12, 60, 0, T, 0.6), rect(328, 168, 12, 60, 0, T, 0.6));
            Circle engranaje = new Circle(130, 100, 40);
            engranaje.setFill(null);
            engranaje.setStroke(P);
            engranaje.setStrokeWidth(14);
            engranaje.getStrokeDashArray().addAll(14.0, 9.0);
            Circle eje = new Circle(130, 100, 14, P);
            Rectangle caja = rect(220, 70, 90, 70, 12, W, 1);
            caja.setStroke(B);
            g.getChildren().addAll(engranaje, eje, caja, rect(236, 88, 58, 10, 5, P, 0.5), rect(236, 106, 40, 10, 5, T, 0.2));
        }
        return g;
    }

    public static Pane marcoEspacio(Espacio s, Node[] objetivoGiro) {
        Group arte = espacio(s);
        Group giratorio = new Group(arte);
        objetivoGiro[0] = giratorio;
        Pane marco = new Pane(giratorio) {
            @Override
            protected void layoutChildren() {
                double w = getWidth();
                double h = getHeight();
                double escala = Math.max(w / 400, h / 250);
                arte.getTransforms().setAll(new javafx.scene.transform.Scale(escala, escala, 0, 0));
                giratorio.relocate((w - 400 * escala) / 2, (h - 250 * escala) / 2);
            }
        };
        Rectangle recorte = new Rectangle();
        recorte.widthProperty().bind(marco.widthProperty());
        recorte.heightProperty().bind(marco.heightProperty());
        marco.setClip(recorte);
        marco.setMinSize(0, 0);
        return marco;
    }

    public static StackPane equipo(Equipo e, Node[] objetivoGiro) {
        Circle r1 = new Circle();
        r1.getStyleClass().addAll("eq-ring", "r1");
        Circle r2 = new Circle();
        r2.getStyleClass().addAll("eq-ring", "r2");
        StackPane ico = new StackPane(new FontIcon(iconoCategoria(e)));
        ico.getStyleClass().add("eq-ico");
        objetivoGiro[0] = ico;
        StackPane arte = new StackPane(r1, r2, ico);
        arte.setAlignment(Pos.CENTER);
        arte.widthProperty().addListener((o, a, b) -> {
            r1.setRadius(b.doubleValue() * 0.29);
            r2.setRadius(b.doubleValue() * 0.43);
        });
        Rectangle recorte = new Rectangle();
        recorte.widthProperty().bind(arte.widthProperty());
        recorte.heightProperty().bind(arte.heightProperty());
        arte.setClip(recorte);
        arte.setMinSize(0, 0);
        return arte;
    }
}
