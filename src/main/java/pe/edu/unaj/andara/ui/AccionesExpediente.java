package pe.edu.unaj.andara.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import lombok.RequiredArgsConstructor;
import org.kordamp.ikonli.javafx.FontIcon;
import org.springframework.stereotype.Component;
import pe.edu.unaj.andara.exception.NegocioException;
import pe.edu.unaj.andara.model.*;
import pe.edu.unaj.andara.service.*;
import pe.edu.unaj.andara.util.Fechas;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AccionesExpediente {

    private final Notificador notificador;
    private final ExpedienteService expedientes;
    private final SeguimientoService seguimiento;
    private final SesionService sesion;
    private final CatalogoService catalogo;
    private final ContextoService contexto;
    private final AccionesPago accionesPago;
    private final Descargas descargas;
    private final DocumentoGeneradoService generador;

    public void seguimiento(String codigo) {
        Expediente s = expedientes.obtener(codigo);
        SeguimientoService.SeguimientoCjsv t = SeguimientoService.calcular(s);
        VBox hero = new VBox(4, Componentes.literal(s.getCodigo(), "label-caps"), Componentes.parrafo(t.estimado(), "trk-hero-titulo"),
                Componentes.literalParrafo(s.getTitulo(), "muted", "small"));
        hero.getStyleClass().add("trk-hero");
        if (t.observado()) {
            hero.getStyleClass().add("trk-obs");
        }
        VBox tiempo = RastreadorCjsv.lineaTiempo(t);
        Persona yo = s.getPersona();
        SeguimientoService.Preferencias pref = seguimiento.preferencias();
        VBox avisos = new VBox(Componentes.mayus("Avísame cuando cambie", "label-caps"),
                interruptor("fth-message-circle", "WhatsApp", yo.getCelular() == null ? null : "+51 " + yo.getCelular(), pref.whatsapp(), "wa"),
                interruptor("fth-mail", "Correo", yo.getCorreo(), pref.correo(), "mail"),
                interruptor("fth-bell", "Notificación en Andara", null, pref.app(), "app"));
        VBox mensajes = new VBox(8);
        List<MensajeEnviado> lista = seguimiento.mensajes(s);
        if (lista.isEmpty()) {
            mensajes.getChildren().add(Componentes.parrafo("Aún no hay mensajes. Te escribiremos cuando tu trámite avance.", "hint"));
        }
        for (MensajeEnviado m : lista) {
            VBox burbuja = new VBox(Componentes.etiqueta(m.getCanal().getEtiqueta() + " · " + Fechas.hm(m.getEnviado()), "wa-c"),
                    Componentes.literalParrafo(m.getTexto(), "small"));
            burbuja.getStyleClass().add("wa-msg");
            if (m.getCanal() == CanalAviso.CORREO) {
                burbuja.getStyleClass().add("mail");
            }
            mensajes.getChildren().add(burbuja);
        }
        Label titMensajes = Componentes.mayus("Mensajes enviados", "label-caps");
        VBox.setMargin(titMensajes, new Insets(16, 0, 8, 0));
        VBox derecha = new VBox(avisos, titMensajes, mensajes, Componentes.parrafo("Envío simulado en esta demostración.", "hint"));
        GridPane grid = new GridPane();
        grid.setHgap(24);
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setPercentWidth(50);
        ColumnConstraints c2 = new ColumnConstraints();
        c2.setPercentWidth(50);
        grid.getColumnConstraints().addAll(c1, c2);
        grid.add(tiempo, 0, 0);
        grid.add(derecha, 1, 0);
        GridPane.setValignment(tiempo, javafx.geometry.VPos.TOP);
        GridPane.setValignment(derecha, javafx.geometry.VPos.TOP);
        VBox cuerpo = new VBox(18, hero, grid);
        notificador.modal("Seguimiento en vivo", cuerpo, true, Componentes.boton("Entendido", "btn-primary", null, e -> notificador.cerrarModal()));
    }

    private HBox interruptor(String icono, String nombre, String detalle, boolean activo, String canal) {
        CheckBox cb = new CheckBox();
        cb.setSelected(activo);
        Componentes.accesible(cb, nombre);
        cb.selectedProperty().addListener((o, a, b) -> {
            seguimiento.cambiarPreferencia(canal, b);
            notificador.info(b ? "Aviso activado." : "Aviso desactivado.");
        });
        HBox fila = new HBox(10, cb, Componentes.icono(icono), Componentes.etiqueta(nombre));
        if (detalle != null) {
            fila.getChildren().add(Componentes.literal(detalle, "hint"));
        }
        fila.getStyleClass().add("sw");
        fila.setOnMouseClicked(e -> {
            if (e.getTarget() != cb) {
                cb.setSelected(!cb.isSelected());
            }
        });
        return fila;
    }

    public void cargo(String codigo) {
        Expediente s = expedientes.obtener(codigo);
        List<Object> pares = new ArrayList<>(List.of("Expediente", Componentes.literal(s.getCodigo(), "dd"), "Trámite", Componentes.parrafo(s.getTitulo(), "dd")));
        s.getDatos().forEach(d -> {
            pares.add(d.getEtiqueta());
            pares.add(Componentes.parrafo(d.getValor(), "dd"));
        });
        pares.add("Titular");
        pares.add(Componentes.literal(s.getPersona().getNombre() + " (" + s.getPersona().getCodigo() + ")", "dd"));
        String titulo = s.getClase() == ClaseExpediente.EQ ? "Cargo digital" : "Cargo de entrega";
        notificador.modal(titulo, Componentes.kv(pares.toArray()), false,
                Componentes.boton("Cerrar", "btn-secondary", null, e -> notificador.cerrarModal()),
                Componentes.boton("Descargar", "btn-primary", "fth-download", e -> descargas.guardarPdf("Cargo " + s.getCodigo(),
                        () -> generador.pdfSimple(titulo + " " + s.getCodigo(), s.getTitulo()))));
    }

    public void estado(String codigo) {
        Expediente s = expedientes.obtener(codigo);
        int paso = Math.max(1, Math.min(4, s.getPaso()));
        VBox cuerpo = new VBox(12, Componentes.pill(s.getEstado(), s.getTono()), Componentes.kv("Expediente", Componentes.literal(s.getCodigo(), "dd"),
                "Paso actual", paso + ". " + ExpedienteService.PASOS.get(paso - 1)[1], "Plazo de respuesta", "48 horas útiles",
                "Última actualización", "Hoy"));
        notificador.modal("Estado del expediente", cuerpo, false, Componentes.boton("Cerrar", "btn-primary", null, e -> notificador.cerrarModal()));
    }

    public void descargar(String codigo) {
        Expediente s = expedientes.obtener(codigo);
        descargas.guardarPdf(s.getTitulo(), () -> generador.pdfSimple(s.getTitulo(), s.getCodigo() + " · " + s.getEstado()));
    }

    public void subsanar(String codigo, Runnable alCambiar) {
        Expediente s = expedientes.obtener(codigo);
        Formulario f = new Formulario();
        Campo<Formulario.Archivo> archivo = f.agregar(Formulario.archivo("Documento de subsanación (PDF)", "*.pdf", "Solo PDF. Máximo 5 MB.", notificador, null))
                .requerido("Adjunta el documento en PDF.");
        Campo<TextArea> comentario = f.agregar(Formulario.area("Comentario para Secretaría (opcional)", "Ej. Adjunto certificado con visado del Servicio Médico."));
        VBox cuerpo = new VBox(14, Componentes.parrafo("Expediente " + s.getCodigo() + ". Adjunta la constancia visada por el Servicio Médico UNAJ.", "muted", "small"),
                archivo.nodo(), comentario.nodo());
        Button enviar = Componentes.boton("Enviar subsanación", "btn-primary", null, e -> {
            if (!f.validar()) {
                return;
            }
            try {
                expedientes.subsanar(codigo, archivo.texto());
                notificador.cerrarModal();
                alCambiar.run();
                notificador.exito("Subsanación enviada. El plazo del trámite continúa.");
            } catch (NegocioException ex) {
                notificador.error(ex.getMessage());
            }
        });
        notificador.modal("Subsanar documento", cuerpo, false, Componentes.boton("Cancelar", "btn-secondary", null, e -> notificador.cerrarModal()), enviar);
    }

    public void desistir(String codigo, Runnable alCambiar) {
        notificador.confirmar("¿Desistir de este trámite?", "El expediente " + codigo + " se anulará. Si pagaste una tasa, Tesorería la reintegrará en 5 días hábiles.",
                "Desistir del trámite", true, () -> {
                    expedientes.desistir(codigo);
                    alCambiar.run();
                    notificador.exito("Trámite anulado.");
                });
    }

    public void cancelar(String codigo, Runnable alCambiar) {
        Expediente s = expedientes.obtener(codigo);
        notificador.confirmar(s.getClase() == ClaseExpediente.LAB ? "¿Cancelar la reserva?" : "¿Cancelar el préstamo?",
                "El expediente " + codigo + " quedará cancelado. Hazlo con al menos 30 minutos de anticipación para evitar penalizaciones.",
                "Sí, cancelar", true, () -> {
                    expedientes.cancelar(codigo);
                    alCambiar.run();
                    notificador.exito("Cancelado correctamente.");
                });
    }

    public void consultar(String codigo) {
        Formulario f = new Formulario();
        Campo<TextArea> msg = f.agregar(Formulario.area("Tu consulta", null)).requerido("Escribe tu consulta.").minimo(10);
        VBox cuerpo = new VBox(14, Componentes.parrafo("Expediente " + codigo + ". Responderemos en un plazo de 48 horas útiles.", "muted", "small"), msg.nodo());
        notificador.modal("Consultar a Secretaría", cuerpo, false, Componentes.boton("Cancelar", "btn-secondary", null, e -> notificador.cerrarModal()),
                Componentes.boton("Enviar consulta", "btn-primary", null, e -> {
                    if (f.validar()) {
                        notificador.cerrarModal();
                        notificador.exito("Consulta enviada a Secretaría.");
                    }
                }));
    }

    public void pase(String codigo) {
        Expediente s = expedientes.obtener(codigo);
        ImageView qr = new ImageView(QrService.imagen("ANDARA-LAB|" + s.getCodigo() + "|DEMO", 320));
        qr.setFitWidth(160);
        qr.setFitHeight(160);
        StackPane marco = new StackPane(qr);
        marco.getStyleClass().add("qr-img");
        marco.setMaxWidth(Region.USE_PREF_SIZE);
        String clave = s.getEtiquetas().stream().filter(x -> x.contains("LAB-")).map(x -> x.substring(x.indexOf("LAB-"))).findFirst().orElse("LAB-992-K");
        String lugar = s.getDatos().size() > 1 ? s.getDatos().get(1).getValor() : "";
        String fecha = s.getDatos().isEmpty() ? "" : s.getDatos().get(0).getValor();
        VBox cuerpo = new VBox(8, marco, Componentes.mayus("Código de llave / acceso", "label-caps"), Componentes.literal(clave, "dato-grande"),
                Componentes.literalParrafo(lugar + " · " + fecha, "muted", "small"), Componentes.parrafo("Código ilustrativo de demostración.", "hint"));
        cuerpo.setAlignment(Pos.CENTER);
        notificador.modal("Pase de acceso", cuerpo, false, Componentes.boton("Listo", "btn-primary", null, e -> notificador.cerrarModal()));
    }

    public void reprogramar(String codigo, Runnable alCambiar) {
        Expediente s = expedientes.obtener(codigo);
        Formulario f = new Formulario();
        Campo<DatePicker> fecha = f.agregar(Formulario.fecha("Nueva fecha", Fechas.hoy(), Fechas.hoy(), null)).requerido("Elige una fecha.");
        Campo<ComboBox<Opcion>> hora = f.agregar(Formulario.selector("Nuevo horario", Opcion.lista(ExpedienteService.horariosReprogramacion(s)),
                "Seleccionar", null)).requerido("Elige un horario.");
        VBox cuerpo = new VBox(14, Componentes.parrafo("Expediente " + s.getCodigo() + ". El cambio queda sujeto a disponibilidad.", "muted", "small"),
                Formulario.rejilla(2, fecha.nodo(), hora.nodo()));
        notificador.modal(s.getClase() == ClaseExpediente.LAB ? "Modificar horario" : "Modificar retiro", cuerpo, false,
                Componentes.boton("Cancelar", "btn-secondary", null, e -> notificador.cerrarModal()),
                Componentes.boton("Guardar cambio", "btn-primary", null, e -> {
                    if (!f.validar()) {
                        return;
                    }
                    try {
                        expedientes.reprogramar(codigo, fecha.control().getValue(), Formulario.valor(hora.control()));
                        notificador.cerrarModal();
                        alCambiar.run();
                        notificador.exito("Horario actualizado.");
                    } catch (NegocioException ex) {
                        notificador.error(ex.getMessage());
                    }
                }));
    }

    public void nueva(Runnable alCambiar) {
        Formulario f = new Formulario();
        List<Opcion> tipos = new ArrayList<>();
        for (ExpedienteService.GrupoOpciones g : ExpedienteService.TIPOS_SOLICITUD) {
            tipos.add(Opcion.grupo(g.grupo()));
            g.opciones().forEach(o -> tipos.add(Opcion.de(o, o)));
        }
        Campo<ComboBox<Opcion>> tipo = f.agregar(Formulario.selector("Tipo de trámite", tipos, "Seleccionar", null)).requerido("Elige el tipo de trámite.");
        List<Opcion> unidades = new ArrayList<>();
        unidades.add(Opcion.grupo("Escuelas profesionales"));
        catalogo.escuelas().forEach(e -> unidades.add(Opcion.de("EP " + e.getNombre(), e.getNombre())));
        unidades.add(Opcion.grupo("Dependencias administrativas"));
        catalogo.dependencias().forEach(d -> unidades.add(Opcion.de(d, d)));
        EscuelaProfesional escuela = contexto.escuela();
        String unidadInicial = "EP " + (escuela != null ? escuela.getNombre() : sesion.estudiante().getEscuela().getNombre());
        Campo<ComboBox<Opcion>> unidad = f.agregar(Formulario.selector("Unidad académica", unidades, "Seleccionar", unidadInicial))
                .requerido("Elige la unidad académica.");
        List<Opcion> sedes = new ArrayList<>();
        catalogo.sedes().forEach(s -> sedes.add(Opcion.de(s.getCodigo(), s.getNombreCompleto())));
        Campo<ComboBox<Opcion>> sede = f.agregar(Formulario.selector("Sede de atención", sedes, null,
                contexto.todasLasSedes() ? sesion.estudiante().getSede().getCodigo() : contexto.codigoSede()));
        Campo<TextArea> detalle = f.agregar(Formulario.area("Detalle de la solicitud", "Ej. Necesito la constancia para presentarla a mi centro de prácticas."))
                .requerido("Describe tu solicitud.").minimo(15);
        Campo<Formulario.Archivo> adjunto = f.agregar(Formulario.archivo("Documento de sustento (opcional)", "*.pdf *.jpg *.png", null, notificador, null));
        VBox cuerpo = new VBox(14, Componentes.parrafo("Tu expediente ingresará a Mesa de Partes Virtual. Si el trámite tiene costo (constancias, certificados, carné), generaremos la orden de pago con tus datos para pagarla en Andara Pay.", "muted", "small"),
                tipo.nodo(), unidad.nodo(), sede.nodo(), detalle.nodo(), adjunto.nodo());
        Button registrar = Componentes.boton("Registrar solicitud", "btn-primary", null, e -> {
            String t = Formulario.valor(tipo.control());
            Optional<Tramite> conPago = expedientes.tramitePara(t);
            if (conPago.isPresent()) {
                accionesPago.iniciar(conPago.get(), null, alCambiar);
                return;
            }
            if (!f.validar()) {
                return;
            }
            try {
                Expediente nuevo = expedientes.registrar(t, Formulario.valor(unidad.control()), Formulario.valor(sede.control()),
                        detalle.control().getText(), adjunto.texto());
                notificador.cerrarModal();
                alCambiar.run();
                notificador.exito("Solicitud " + nuevo.getCodigo() + " registrada.");
            } catch (NegocioException ex) {
                notificador.error(ex.getMessage());
            }
        });
        notificador.modal("Nueva solicitud", cuerpo, false, Componentes.boton("Cancelar", "btn-secondary", null, e -> notificador.cerrarModal()), registrar);
    }

    public void ejecutar(String accion, Expediente s, Runnable alCambiar, Runnable alternarObservacion) {
        switch (accion) {
            case "route-sol", "track-sol" -> seguimiento(s.getCodigo());
            case "download-sol" -> descargar(s.getCodigo());
            case "cargo-sol" -> cargo(s.getCodigo());
            case "state-sol" -> estado(s.getCodigo());
            case "desist-sol" -> desistir(s.getCodigo(), alCambiar);
            case "fix-sol" -> subsanar(s.getCodigo(), alCambiar);
            case "obs-sol" -> alternarObservacion.run();
            case "ask-sol" -> consultar(s.getCodigo());
            case "pass-sol" -> pase(s.getCodigo());
            case "resched-sol" -> reprogramar(s.getCodigo(), alCambiar);
            case "cancel-sol" -> cancelar(s.getCodigo(), alCambiar);
            case "pay-sol" -> accionesPago.pantalla(s.getPago().getCodigo(), alCambiar);
            case "rcpt-sol" -> accionesPago.comprobante(s.getPago().getCodigo());
            case "gen-sol" -> accionesPago.documento(s.getPago().getCodigo());
            default -> notificador.info("Acción no disponible en la demostración.");
        }
    }

    public static Node iconoExpediente(String icono) {
        return new FontIcon(switch (icono) {
            case "alert" -> "fth-alert-triangle";
            case "lab" -> "fth-droplet";
            case "box" -> "fth-box";
            case "idcard" -> "fth-credit-card";
            default -> "fth-file-text";
        });
    }
}
