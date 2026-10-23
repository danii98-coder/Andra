package pe.edu.unaj.andara.ui;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.util.Duration;
import lombok.RequiredArgsConstructor;
import org.kordamp.ikonli.javafx.FontIcon;
import org.springframework.stereotype.Component;
import pe.edu.unaj.andara.exception.NegocioException;
import pe.edu.unaj.andara.model.*;
import pe.edu.unaj.andara.service.*;
import pe.edu.unaj.andara.util.Fechas;
import pe.edu.unaj.andara.util.Montos;
import pe.edu.unaj.andara.util.Portapapeles;
import pe.edu.unaj.andara.util.TarjetaJemm;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class AccionesPago {

    private final Notificador notificador;
    private final PagoService pagos;
    private final CatalogoService catalogo;
    private final SesionService sesion;
    private final ContextoService contexto;
    private final ConexionService conexion;
    private final DocumentoGeneradoService generador;
    private final Descargas descargas;

    private Runnable alCambiar = () -> {
    };

    private static final class Borrador {
        Tramite tramite;
        DatosSolicitante sol;
        String motivo;
        String sede;
        int cantidad = 1;
        byte[] foto;
        String nombreFoto;
    }

    public void alCambiar(Runnable r) {
        this.alCambiar = r == null ? () -> {
        } : r;
    }

    public static FlowPane pasos(int actual) {
        String[] nombres = {"Datos", "Orden de pago", "Pago", "Listo"};
        FlowPane f = new FlowPane();
        f.getStyleClass().add("wz-steps");
        for (int i = 0; i < nombres.length; i++) {
            int n = i + 1;
            StackPane num = new StackPane(n < actual ? new FontIcon("fth-check") : Componentes.literal(String.valueOf(n)));
            num.getStyleClass().add("wz-num");
            HBox paso = new HBox(num, Componentes.etiqueta(nombres[i]));
            paso.getStyleClass().add("wz-step");
            if (n < actual) {
                paso.getStyleClass().add("done");
            } else if (n == actual) {
                paso.getStyleClass().add("cur");
            }
            f.getChildren().add(paso);
        }
        Componentes.accesible(f, "Paso " + actual + " de 4");
        return f;
    }

    public void iniciar(Tramite t, String motivo, Runnable alTerminar) {
        alCambiar(alTerminar);
        Persona yo = sesion.estudiante();
        Borrador b = new Borrador();
        b.tramite = t;
        b.sol = DatosSolicitante.builder().tipo("Estudiante").nombre(yo.getNombre()).dni(yo.getDni()).codigo(yo.getCodigo())
                .escuela(yo.getEscuela().getCodigo()).correo(yo.getCorreo()).build();
        b.motivo = motivo == null || motivo.isBlank() ? "Trámite personal" : motivo;
        b.sede = contexto.todasLasSedes() ? yo.getSede().getCodigo() : contexto.codigoSede();
        paso1(b);
    }

    private void paso1(Borrador b) {
        Tramite t = b.tramite;
        DatosSolicitante s = b.sol;
        boolean pideCodigo = List.of("Estudiante", "Egresado").contains(s.getTipo());
        boolean conEscuela = List.of("Estudiante", "Egresado", "Docente").contains(s.getTipo());
        VBox resumenTexto = new VBox(2, Componentes.mayus(t.getCategoria(), "label-caps"), Componentes.parrafo(t.getNombre(), "strong"),
                Componentes.parrafo(t.getDescripcion(), "hint"));
        HBox.setHgrow(resumenTexto, Priority.ALWAYS);
        VBox precio = new VBox(Componentes.literal(Montos.soles(t.getMonto()), "wz-price"));
        precio.setAlignment(Pos.CENTER_RIGHT);
        if (t.getUnidad() != null) {
            precio.getChildren().add(Componentes.etiqueta(t.getUnidad(), "hint"));
        }
        HBox resumen = new HBox(resumenTexto, precio);
        resumen.getStyleClass().add("wz-sum");

        Formulario f = new Formulario();
        Campo<ComboBox<Opcion>> tipo = f.agregar(Formulario.selector("Tipo de solicitante", Opcion.lista(CatalogoService.SOLICITANTES), null, s.getTipo()));
        Campo<TextField> nombre = f.agregar(Formulario.texto("Nombres y apellidos", null, s.getNombre())).requerido("Escribe tus nombres y apellidos.");
        Campo<TextField> dni = f.agregar(Formulario.numero("DNI", null, s.getDni(), 8)).requerido("Escribe tu DNI.")
                .patron("\\d{8}", "El DNI debe tener 8 dígitos.");
        String etiquetaCodigo = pideCodigo ? "Código universitario" : "Público en general".equals(s.getTipo())
                ? "Institución o empresa (opcional)" : "Código o área de trabajo";
        Campo<TextField> codigo = f.agregar(Formulario.texto(etiquetaCodigo, null, "Público en general".equals(s.getTipo()) ? "" : s.getCodigo()));
        if (pideCodigo) {
            codigo.requerido("Escribe tu código universitario.");
        }
        List<Opcion> escuelas = new ArrayList<>();
        if (conEscuela) {
            catalogo.escuelas().forEach(e -> escuelas.add(Opcion.de(e.getCodigo(), e.getNombre())));
        } else {
            catalogo.dependencias().forEach(d -> escuelas.add(Opcion.de(d, d)));
        }
        Campo<ComboBox<Opcion>> escuela = f.agregar(Formulario.selector(conEscuela ? "Escuela profesional" : "Dependencia", escuelas,
                conEscuela ? null : "No aplica", s.getEscuela()));
        if (conEscuela) {
            escuela.requerido("Elige tu escuela.");
        }
        Campo<TextField> correo = f.agregar(Formulario.texto("Correo electrónico", null, s.getCorreo())).requerido("Escribe tu correo.")
                .patron("[^@\\s]+@[^@\\s]+\\.[^@\\s]+", "Escribe un correo válido.");
        Campo<ComboBox<Opcion>> uso = f.agregar(Formulario.selector("Uso del documento", Opcion.lista(PagoService.USOS), null, b.motivo));
        List<Opcion> sedes = new ArrayList<>();
        catalogo.sedes().forEach(x -> sedes.add(Opcion.de(x.getCodigo(), x.getNombreCompleto() + " · Mesa de Partes")));
        Campo<ComboBox<Opcion>> sede = f.agregar(Formulario.selector("Sede de recojo", sedes, null, b.sede));
        List<Node> nodos = new ArrayList<>(List.of(tipo.nodo(), nombre.nodo(), dni.nodo(), codigo.nodo(), escuela.nodo(), correo.nodo(),
                uso.nodo(), sede.nodo()));
        Campo<TextField> cantidad = null;
        if (t.isPorCantidad()) {
            cantidad = f.agregar(Formulario.numero("Cantidad de cursos", null, String.valueOf(b.cantidad), 2));
            nodos.add(cantidad.nodo());
        }
        Campo<Formulario.Archivo> foto = null;
        if (t.isCarne()) {
            foto = f.agregar(Formulario.archivo("Foto tamaño carné", "*.jpg *.jpeg *.png", "Fondo blanco, de frente. JPG o PNG.", notificador, file -> {
                try {
                    b.foto = Files.readAllBytes(file.toPath());
                    b.nombreFoto = file.getName();
                } catch (IOException e) {
                    notificador.error("No se pudo leer la foto.");
                }
            }));
            nodos.add(foto.nodo());
        }
        Label errorPublico = Componentes.parrafo("", "err");
        errorPublico.setVisible(false);
        errorPublico.setManaged(false);
        Runnable recolectar = () -> {
            b.sol = DatosSolicitante.builder().tipo(Formulario.valor(tipo.control())).nombre(nombre.control().getText().trim())
                    .dni(dni.control().getText().trim()).codigo(codigo.control().getText().trim())
                    .escuela(Formulario.valor(escuela.control()).isBlank() ? null : Formulario.valor(escuela.control()))
                    .correo(correo.control().getText().trim()).build();
            b.motivo = Formulario.valor(uso.control());
            b.sede = Formulario.valor(sede.control());
        };
        tipo.control().valueProperty().addListener((o, a, n) -> {
            if (n == null || a == null || n.valor().equals(a.valor())) {
                return;
            }
            recolectar.run();
            b.sol.setTipo(n.valor());
            if ("Público en general".equals(n.valor())) {
                b.sol.setCodigo("");
                b.sol.setEscuela(null);
            } else if (!List.of("Estudiante", "Egresado", "Docente").contains(n.valor())) {
                b.sol.setEscuela(null);
            } else if (b.sol.getEscuela() == null || catalogo.escuela(b.sol.getEscuela()).isEmpty()) {
                b.sol.setEscuela(sesion.estudiante().getEscuela().getCodigo());
            }
            paso1(b);
        });
        GridPane rejilla = Formulario.rejilla(2, nodos.toArray(new Node[0]));
        Label subtitulo = Componentes.mayus("Datos del solicitante", "label-caps");
        Label aclaracion = Componentes.etiqueta("· se usan para generar el documento", "hint");
        HBox titulo = new HBox(6, subtitulo, aclaracion);
        titulo.setAlignment(Pos.CENTER_LEFT);
        titulo.setPadding(new Insets(8, 0, 0, 0));
        VBox cuerpo = new VBox(14, pasos(1), resumen, titulo, rejilla, errorPublico);
        Button siguiente = Componentes.boton("Generar orden de pago", "btn-primary", "fth-arrow-right", null);
        siguiente.setContentDisplay(ContentDisplay.RIGHT);
        Campo<TextField> cantidadFinal = cantidad;
        Campo<Formulario.Archivo> fotoFinal = foto;
        siguiente.setOnAction(e -> {
            if (!f.validar()) {
                return;
            }
            recolectar.run();
            if (cantidadFinal != null) {
                String q = cantidadFinal.control().getText().trim();
                b.cantidad = Math.max(1, Math.min(20, q.isEmpty() ? 1 : Integer.parseInt(q)));
            }
            Map<String, String> errores = pagos.validarSolicitante(t, b.sol, b.foto != null);
            if (errores.containsKey("publico")) {
                Componentes.fijar(errorPublico, errores.get("publico"));
                errorPublico.setVisible(true);
                errorPublico.setManaged(true);
                return;
            }
            if (errores.containsKey("foto") && fotoFinal != null) {
                fotoFinal.mostrar(errores.get("foto"));
                return;
            }
            paso2(b);
        });
        notificador.modal("Solicitar " + t.getNombre().toLowerCase(), cuerpo, true,
                Componentes.boton("Cancelar", "btn-secondary", null, e -> notificador.cerrarModal()), siguiente);
    }

    private void paso2(Borrador b) {
        Tramite t = b.tramite;
        List<ItemPago> items = PagoService.itemsPara(t, b.cantidad);
        BigDecimal total = PagoService.totalOrden(items);
        Sede sede = catalogo.sede(b.sede).orElseThrow();
        String codigo = pagos.siguienteCodigoOrden();
        PagoService.Recojo r = PagoService.recojo(t, Fechas.ahora());
        HBox recojo = new HBox(new FontIcon("fth-calendar"), new VBox(2, Componentes.etiqueta("Recojo estimado si pagas ahora", "strong"),
                Componentes.parrafo(r.fecha() != null ? r.texto() + " · Mesa de Partes, " + sede.getNombreCompleto() : r.texto(), "small")));
        recojo.getStyleClass().add("wz-pick");
        VBox cuerpo = new VBox(14, pasos(2), VistaFactura.crear(codigo, items, b.sol, Fechas.hoy().plusDays(3), sede, null, catalogo), recojo);
        Button volver = Componentes.boton("Volver", "btn-secondary", null, e -> paso1(b));
        Button despues = Componentes.boton("Pagar después", "btn-secondary", null, e -> {
            Pago p = crear(b);
            if (p == null) {
                return;
            }
            notificador.cerrarModal();
            alCambiar.run();
            notificador.exito("Orden " + p.getCodigo() + " guardada en Andara Pay. Págala antes del " + Fechas.dmy(p.getVence()) + ".");
        });
        Button pagar = Componentes.boton("Pagar " + Montos.soles(total), "btn-primary", "fth-credit-card", e -> {
            Pago p = crear(b);
            if (p != null) {
                alCambiar.run();
                pantalla(p.getCodigo(), alCambiar);
            }
        });
        notificador.modal("Orden de pago", cuerpo, true, volver, despues, pagar);
    }

    private Pago crear(Borrador b) {
        try {
            return pagos.crearOrden(b.tramite, b.sol, b.cantidad, b.motivo, b.sede, b.foto);
        } catch (NegocioException e) {
            notificador.error(e.getMessage());
            return null;
        }
    }

    public void sinConexion(Pago p) {
        notificador.modal("Necesitas conexión para pagar", Componentes.parrafo("Los pagos con Yape, Plin o tarjeta requieren internet. Tu orden "
                + p.getCodigo() + " quedó guardada: págala cuando vuelva la conexión.", "muted"), false,
                Componentes.boton("Entendido", "btn-primary", null, e -> notificador.cerrarModal()));
    }

    public void pantalla(String codigo, Runnable alTerminar) {
        alCambiar(alTerminar);
        Pago p = pagos.obtener(codigo);
        if (conexion.sinConexion()) {
            sinConexion(p);
            return;
        }
        if (p.isPagado()) {
            comprobante(codigo);
            return;
        }
        new PantallaPago(p).dibujar();
    }

    private final class PantallaPago {
        private final Pago pago;
        private String metodo;
        private String sub = "qr";
        private Timeline reloj;
        private final StackPane panel = new StackPane();

        PantallaPago(Pago pago) {
            this.pago = pago;
        }

        void detener() {
            if (reloj != null) {
                reloj.stop();
            }
        }

        void dibujar() {
            detener();
            BigDecimal total = pago.getTotal();
            VBox resumen = new VBox(6, Componentes.mayus(pago.getTipo().getEtiqueta(), "label-caps"),
                    Componentes.literalParrafo(pago.getConcepto(), "strong"), Componentes.literal(pago.getCodigo(), "hint"));
            VBox items = new VBox(8);
            for (ItemPago it : pago.getItems()) {
                HBox fila = new HBox(8, Componentes.literalParrafo(it.getConcepto() + (it.getCantidad() > 1 ? " × " + it.getCantidad() : ""), "small"),
                        Componentes.espacio(), Componentes.literal(Montos.soles(it.getImporte()), "strong", "small"));
                ((Label) fila.getChildren().get(2)).setMinWidth(Region.USE_PREF_SIZE);
                items.getChildren().add(fila);
            }
            VBox.setMargin(items, new Insets(8, 0, 0, 0));
            HBox tot = new HBox(Componentes.etiqueta("Total"), Componentes.espacio(), Componentes.literal(Montos.soles(total), "pay-total-valor"));
            tot.getStyleClass().add("pay-total");
            resumen.getChildren().addAll(items, tot);
            if (pago.getVence() != null) {
                resumen.getChildren().add(Componentes.etiqueta("Vence el " + Fechas.dmy(pago.getVence()), "hint"));
            }
            resumen.getStyleClass().add("pay-sum");

            ToggleGroup grupo = new ToggleGroup();
            VBox metodos = new VBox(10);
            for (String[] m : new String[][]{{"yape", "Yape", "Paga con QR o código de aprobación"}, {"plin", "Plin", "Paga con QR o tu número"},
                    {"tarjeta", "Tarjeta", "Visa, Mastercard, Amex, Diners"}}) {
                StackPane insignia = new StackPane("tarjeta".equals(m[0]) ? new FontIcon("fth-credit-card") : Componentes.literal(m[1].substring(0, 1)));
                insignia.getStyleClass().add("pm-badge");
                VBox textos = new VBox(Componentes.literal(m[1], "pm-tit"), Componentes.etiqueta(m[2], "pm-sub"));
                HBox contenido = new HBox(10, insignia, textos);
                contenido.setAlignment(Pos.CENTER_LEFT);
                ToggleButton b = new ToggleButton();
                b.setGraphic(contenido);
                b.getStyleClass().addAll("pm-tile", "pm-" + m[0]);
                b.setToggleGroup(grupo);
                b.setMaxWidth(Double.MAX_VALUE);
                b.setSelected(m[0].equals(metodo));
                b.setOnAction(e -> {
                    metodo = m[0];
                    sub = "qr";
                    dibujar();
                });
                Componentes.accesible(b, m[1] + ". " + m[2]);
                metodos.getChildren().add(b);
            }
            panel.getChildren().setAll(metodo == null ? vacio() : contenidoMetodo(total));
            VBox derecha = new VBox(10, Componentes.mayus("Elige cómo pagar", "label-caps"), metodos, panel);
            HBox.setHgrow(derecha, Priority.ALWAYS);
            HBox grid = new HBox(22, resumen, derecha);
            grid.getStyleClass().add("pay-grid");
            VBox cuerpo = new VBox(14);
            if (pago.getTramite() != null) {
                cuerpo.getChildren().add(pasos(3));
            }
            cuerpo.getChildren().add(grid);
            Label nota = Componentes.etiqueta("Pago simulado para demostración", "hint");
            nota.setGraphic(Componentes.icono("fth-shield"));
            HBox notaCaja = new HBox(nota);
            notaCaja.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(notaCaja, Priority.ALWAYS);
            Button despues = Componentes.boton("Pagar después", "btn-secondary", null, e -> notificador.cerrarModal());
            notificador.modal("Pagar con Andara Pay", cuerpo, true, this::detener, notaCaja, despues);
        }

        private Node vacio() {
            VBox v = new VBox(new FontIcon("fth-credit-card"), Componentes.parrafo("Selecciona Yape, Plin o tarjeta para continuar.", "muted", "small"));
            v.getStyleClass().add("pm-empty");
            return v;
        }

        private Node contenidoMetodo(BigDecimal total) {
            if ("tarjeta".equals(metodo)) {
                return tarjeta(total);
            }
            String nombre = "yape".equals(metodo) ? "Yape" : "Plin";
            Segmentado seg = new Segmentado(false).opcion("qr", "Código QR", null, null)
                    .opcion("code", "yape".equals(metodo) ? "Código de aprobación" : "Número celular", null, null).seleccionar(sub);
            seg.alCambiar(v -> {
                sub = v;
                dibujar();
            });
            Node contenido;
            if ("qr".equals(sub)) {
                ImageView qr = new ImageView(QrService.imagen("ANDARA-PAY|" + pago.getCodigo() + "|" + total.setScale(2) + "|DEMO", 336));
                qr.setFitWidth(168);
                qr.setFitHeight(168);
                StackPane marco = new StackPane(qr);
                marco.getStyleClass().add("qr-img");
                Componentes.accesible(marco, "Código QR de pago");
                Label tiempo = Componentes.literal("05:00", "strong");
                int[] segundos = {300};
                reloj = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
                    segundos[0]--;
                    if (segundos[0] <= 0) {
                        Componentes.fijar(tiempo, "vencido");
                        reloj.stop();
                    } else {
                        tiempo.setText(String.format("%02d:%02d", segundos[0] / 60, segundos[0] % 60));
                    }
                }));
                reloj.setCycleCount(Animation.INDEFINITE);
                reloj.play();
                Button listo = Componentes.boton("Ya realicé el pago", "btn-primary", "fth-check", e -> procesar(metodo));
                HBox vence = new HBox(4, Componentes.etiqueta("El código vence en", "hint"), tiempo);
                VBox texto = new VBox(6, Componentes.etiqueta("Escanea con tu app " + nombre, "strong"),
                        new HBox(4, Componentes.etiqueta("Monto:", "hint"), Componentes.literal(Montos.soles(total), "strong"),
                                Componentes.etiqueta("· Destino: UNAJ · Andara Pay", "hint")), vence, listo);
                VBox.setMargin(listo, new Insets(8, 0, 0, 0));
                HBox caja = new HBox(18, marco, texto);
                caja.getStyleClass().add("qr-box");
                contenido = caja;
            } else if ("yape".equals(metodo)) {
                Formulario f = new Formulario();
                Campo<TextField> cel = f.agregar(Formulario.numero("Celular Yape", null, sesion.estudiante().getCelular().replaceAll("\\s", ""), 9))
                        .requerido("Escribe tu celular.").patron("9\\d{8}", "Celular de 9 dígitos que empiece con 9.");
                Campo<TextField> cod = f.agregar(Formulario.numero("Código de aprobación", "••••••", "", 6))
                        .requerido("Escribe el código de 6 dígitos.").patron("\\d{6}", "El código tiene 6 dígitos.");
                Button pagar = Componentes.bloque(Componentes.boton("Pagar " + Montos.soles(total) + " con Yape", "btn-primary", null, e -> {
                    if (f.validar() && PagoService.validarYape(cel.control().getText(), cod.control().getText()).isEmpty()) {
                        procesar("yape");
                    }
                }));
                pagar.setDefaultButton(true);
                contenido = new VBox(12, Componentes.parrafo("Abre Yape, entra al menú y toca Código de aprobación. Tienes unos minutos para usarlo.", "hint"),
                        Componentes.parrafo("Abre Yape › Código de aprobación", "small", "azul"),
                        Formulario.rejilla(2, cel.nodo(), cod.nodo()), pagar);
            } else {
                Formulario f = new Formulario();
                Campo<TextField> cel = f.agregar(Formulario.numero("Celular Plin", null, sesion.estudiante().getCelular().replaceAll("\\s", ""), 9))
                        .requerido("Escribe tu celular.").patron("9\\d{8}", "Celular de 9 dígitos que empiece con 9.");
                Button enviar = Componentes.bloque(Componentes.boton("Enviar solicitud de cobro", "btn-primary", null, null));
                VBox caja = new VBox(12, Componentes.parrafo("Te enviaremos una solicitud de cobro a tu app Plin para que la apruebes.", "hint"), cel.nodo(), enviar);
                enviar.setOnAction(e -> {
                    if (!f.validar()) {
                        return;
                    }
                    ProgressIndicator spin = new ProgressIndicator();
                    spin.setPrefSize(38, 38);
                    Button aprobar = Componentes.boton("Simular aprobación en Plin", "btn-primary", null, x -> procesar("plin"));
                    VBox espera = new VBox(10, spin, Componentes.etiqueta("Solicitud enviada a tu Plin", "strong"),
                            Componentes.parrafo("Apruébala en tu app. En esta demostración puedes confirmarla aquí.", "hint"), aprobar);
                    espera.getStyleClass().add("wait");
                    panel.getChildren().setAll(espera);
                    aprobar.requestFocus();
                });
                contenido = caja;
            }
            VBox v = new VBox(14, seg, contenido);
            Movimiento.subir(v);
            return v;
        }

        private Node tarjeta(BigDecimal total) {
            Formulario f = new Formulario();
            Campo<TextField> numero = f.agregar(Formulario.texto("Número de tarjeta", "0000 0000 0000 0000", "")).requerido("Escribe el número de tu tarjeta.");
            Label marca = Componentes.literal("—", "card-brand");
            numero.control().textProperty().addListener((o, a, b) -> {
                String formato = TarjetaJemm.formatear(b);
                if (!formato.equals(b)) {
                    javafx.application.Platform.runLater(() -> {
                        numero.control().setText(formato);
                        numero.control().positionCaret(formato.length());
                    });
                }
                MetodoPago m = TarjetaJemm.marca(b);
                marca.setText(m == null ? "—" : m.getEtiqueta());
            });
            HBox filaNumero = new HBox(8, numero.nodo(), marca);
            HBox.setHgrow(numero.nodo(), Priority.ALWAYS);
            filaNumero.setAlignment(Pos.CENTER_LEFT);
            Campo<TextField> titular = f.agregar(Formulario.texto("Nombre del titular", null, sesion.estudiante().getNombre().toUpperCase()))
                    .requerido("Escribe el nombre como aparece en la tarjeta.");
            Campo<TextField> vence = f.agregar(Formulario.texto("Vencimiento", "MM/AA", "")).requerido("Escribe el vencimiento.");
            vence.control().textProperty().addListener((o, a, b) -> {
                String formato = TarjetaJemm.formatearVencimiento(b);
                if (!formato.equals(b) && !(a != null && a.length() > b.length())) {
                    javafx.application.Platform.runLater(() -> {
                        vence.control().setText(formato);
                        vence.control().positionCaret(formato.length());
                    });
                }
            });
            Campo<TextField> cvv = f.agregar(Formulario.numero("CVV", "123", "", 4)).requerido("Escribe el CVV.");
            Campo<TextField> correo = f.agregar(Formulario.texto("Correo para el comprobante", null, sesion.estudiante().getCorreo()))
                    .requerido("Escribe un correo.");
            Button pagar = Componentes.bloque(Componentes.boton("Pagar " + Montos.soles(total), "btn-primary", "fth-lock", null));
            pagar.setDefaultButton(true);
            pagar.setOnAction(e -> {
                if (!f.validar()) {
                    return;
                }
                Map<String, String> errores = PagoService.validarTarjeta(numero.control().getText(), titular.control().getText(),
                        vence.control().getText(), cvv.control().getText(), correo.control().getText(), YearMonth.now(Fechas.reloj()));
                if (errores.containsKey("numero")) {
                    numero.mostrar(errores.get("numero"));
                    numero.control().requestFocus();
                    return;
                }
                if (errores.containsKey("vencimiento")) {
                    vence.mostrar(errores.get("vencimiento"));
                    vence.control().requestFocus();
                    return;
                }
                if (errores.containsKey("cvv")) {
                    cvv.mostrar(errores.get("cvv"));
                    cvv.control().requestFocus();
                    return;
                }
                MetodoPago m = TarjetaJemm.marca(numero.control().getText());
                procesar(m.name().toLowerCase());
            });
            Label ayuda = Componentes.parrafo("Prueba con 4111 1111 1111 1111. No se guardan datos de tarjeta.", "hint");
            ayuda.setAlignment(Pos.CENTER);
            ayuda.setMaxWidth(Double.MAX_VALUE);
            VBox v = new VBox(12, filaNumero, titular.nodo(), Formulario.rejilla(2, vence.nodo(), cvv.nodo()), correo.nodo(), pagar, ayuda);
            Movimiento.subir(v);
            return v;
        }

        private void procesar(String clave) {
            detener();
            ProgressIndicator spin = new ProgressIndicator();
            spin.setPrefSize(38, 38);
            VBox espera = new VBox(10, spin, Componentes.etiqueta("Procesando tu pago…", "strong"), Componentes.etiqueta("No cierres esta ventana.", "hint"));
            espera.getStyleClass().add("wait");
            panel.getChildren().setAll(espera);
            PauseTransition p = new PauseTransition(Duration.millis(1400));
            p.setOnFinished(e -> {
                try {
                    pagos.completar(pago.getCodigo(), MetodoPago.desdeClave(clave));
                    alCambiar.run();
                    exito(pago.getCodigo());
                } catch (NegocioException ex) {
                    notificador.cerrarModal();
                    if (conexion.sinConexion()) {
                        sinConexion(pago);
                    } else {
                        notificador.error(ex.getMessage());
                    }
                }
            });
            p.play();
        }
    }

    public void exito(String codigo) {
        Pago p = pagos.obtener(codigo);
        StackPane check = new StackPane(new FontIcon("fth-check"));
        check.getStyleClass().add("ok-check");
        if (!Movimiento.reducido()) {
            check.setScaleX(0.4);
            check.setScaleY(0.4);
            ScaleTransition st = new ScaleTransition(Duration.millis(500), check);
            st.setToX(1);
            st.setToY(1);
            st.setInterpolator(javafx.animation.Interpolator.SPLINE(0.34, 1.56, 0.64, 1));
            st.play();
        }
        VBox hero = new VBox(6, check, Componentes.literal(Montos.soles(p.getTotal()), "ok-monto"), Componentes.literalParrafo(p.getConcepto()));
        hero.getStyleClass().add("ok-hero");
        Object filaFinal;
        String etiquetaFinal;
        if (p.getTipo() == TipoPago.MATRICULA) {
            etiquetaFinal = "Matrícula";
            filaFinal = Componentes.pill("Confirmada", "success");
        } else {
            etiquetaFinal = "Recojo";
            VBox r = new VBox(2, Componentes.parrafo(p.getRecojoTexto(), "dd"));
            if (p.getRecojoFecha() != null) {
                r.getChildren().add(Componentes.parrafo("Mesa de Partes · " + p.getSede().getNombreCompleto() + ". Lleva tu DNI.", "hint"));
            }
            filaFinal = r;
        }
        javafx.scene.layout.GridPane kv = Componentes.kv("Método", p.getMetodo().getEtiqueta(), "N.° de operación", Componentes.literal(p.getOperacion(), "dd"),
                "Comprobante", Componentes.literal(p.getBoleta(), "dd"), "Fecha y hora", Componentes.literal(Fechas.dmyHm(p.getPagado()), "dd"),
                etiquetaFinal, filaFinal);
        kv.setMaxWidth(520);
        VBox cuerpo = new VBox(18);
        if (p.getTramite() != null) {
            cuerpo.getChildren().add(pasos(4));
        }
        cuerpo.getChildren().addAll(hero, kv);
        cuerpo.setAlignment(Pos.TOP_CENTER);
        List<Node> pie = new ArrayList<>();
        pie.add(Componentes.boton("Ver comprobante", "btn-secondary", "fth-file", e -> comprobante(codigo)));
        if (p.getTramite() != null && p.getTramite().generaDocumento()) {
            pie.add(Componentes.boton("Ver documento", "btn-secondary", "fth-file-text", e -> documento(codigo)));
        }
        pie.add(Componentes.boton("Listo", "btn-primary", null, e -> {
            notificador.cerrarModal();
            alCambiar.run();
        }));
        notificador.modal("Pago confirmado", cuerpo, true, pie.toArray(new Node[0]));
    }

    public void comprobante(String codigo) {
        Pago p = pagos.obtener(codigo);
        StackPane hoja = VistaFactura.crear(p.getCodigo(), p.getItems(), generador.solicitante(p), p.getVence(), p.getSede(), p, catalogo);
        notificador.modal("Comprobante de pago", hoja, true,
                Componentes.boton("Copiar N.° de operación", "btn-secondary", null, e -> {
                    Portapapeles.copiar(p.getOperacion());
                    notificador.exito("Número copiado.");
                }),
                Componentes.boton("Descargar", "btn-primary", "fth-download", e -> descargas.guardarPdf("Comprobante " + p.getBoleta(),
                        () -> generador.pdfComprobante(p))));
    }

    public void documento(String codigo) {
        Pago p = pagos.obtener(codigo);
        VBox cuerpo = new VBox(12, Componentes.parrafo("Generado automáticamente con los datos del solicitante. Documento de demostración sin valor oficial.", "hint"),
                VistaDocumento.crear(generador.construir(p)));
        notificador.modal("Documento generado", cuerpo, true,
                Componentes.boton("Cerrar", "btn-secondary", null, e -> notificador.cerrarModal()),
                Componentes.boton("Descargar PDF", "btn-primary", "fth-download", e -> descargas.guardarPdf(p.getConcepto() + " " + (p.getBoleta() == null ? "" : p.getBoleta()),
                        () -> generador.pdfDocumento(p))));
    }

    public void detalle(String codigo, Runnable alTerminar) {
        alCambiar(alTerminar);
        Pago p = pagos.obtener(codigo);
        if (!p.isPendiente()) {
            comprobante(codigo);
            return;
        }
        FlowPane metodos = new FlowPane(6, 6, Componentes.etiqueta("Puedes pagar con", "hint"), Componentes.literal("Yape", "pm-chip", "y"),
                Componentes.literal("Plin", "pm-chip", "p"), Componentes.literal("Visa", "pm-chip"), Componentes.literal("Mastercard", "pm-chip"));
        metodos.setAlignment(Pos.CENTER);
        VBox cuerpo = new VBox(14, VistaFactura.crear(p.getCodigo(), p.getItems(), generador.solicitante(p), p.getVence(), p.getSede(), null, catalogo), metodos);
        notificador.modal("Detalle del derecho de pago", cuerpo, true,
                Componentes.boton("Cerrar", "btn-secondary", null, e -> notificador.cerrarModal()),
                Componentes.boton("Pagar " + Montos.soles(p.getTotal()), "btn-primary", "fth-credit-card", e -> pantalla(codigo, alCambiar)));
    }

    public void nuevoPago(Runnable alTerminar) {
        alCambiar(alTerminar);
        VBox lista = new VBox(8);
        for (Tramite t : catalogo.tramites()) {
            VBox textos = new VBox(2, Componentes.parrafo(t.getNombre(), "strong"), Componentes.etiqueta(t.getCategoria() + " · " + t.getTextoEntrega(), "hint"));
            HBox.setHgrow(textos, Priority.ALWAYS);
            HBox contenido = new HBox(10, textos, Componentes.literal(Montos.soles(t.getMonto()), "strong"));
            contenido.setAlignment(Pos.CENTER_LEFT);
            Button b = new Button();
            b.setGraphic(contenido);
            b.getStyleClass().add("tupa-pick");
            b.setMaxWidth(Double.MAX_VALUE);
            contenido.prefWidthProperty().bind(b.widthProperty().subtract(30));
            b.setOnAction(e -> iniciar(t, null, alCambiar));
            lista.getChildren().add(b);
        }
        VBox cuerpo = new VBox(14, Componentes.parrafo("Elige el documento o trámite. Generaremos la orden con tus datos.", "muted", "small"), lista);
        notificador.modal("Nuevo pago", cuerpo, true);
    }
}
