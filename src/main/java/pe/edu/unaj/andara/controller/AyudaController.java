package pe.edu.unaj.andara.controller;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;
import lombok.RequiredArgsConstructor;
import org.kordamp.ikonli.javafx.FontIcon;
import org.springframework.stereotype.Component;
import pe.edu.unaj.andara.config.DatosInstitucion;
import pe.edu.unaj.andara.model.Expediente;
import pe.edu.unaj.andara.model.Sede;
import pe.edu.unaj.andara.service.*;
import pe.edu.unaj.andara.ui.*;
import pe.edu.unaj.andara.util.Fechas;
import pe.edu.unaj.andara.util.Portapapeles;

import java.util.*;

@Component
@RequiredArgsConstructor
public class AyudaController extends VistaBase {

    private final AyudaService ayuda;
    private final ContextoService contexto;
    private final CatalogoService catalogo;
    private final SesionService sesion;
    private final ExpedienteService expedientes;
    private final DatosInstitucion institucion;

    private TextField busqueda;
    private final Set<Integer> abiertas = new HashSet<>();
    private Boolean util;
    private VBox canales;

    @Override
    public void enfocarBusqueda() {
        if (busqueda != null) {
            busqueda.requestFocus();
        }
    }

    public void irACanales() {
        if (canales != null) {
            Componentes.desplazarHasta(desplazable, canales);
        }
    }

    private TextField busqueda() {
        if (busqueda == null) {
            busqueda = new TextField();
            Componentes.prompt(busqueda, "Busca por trámite, tema o palabra clave (ej. 'subsanación', 'carné', 'firma SUNEDU', 'tasa')…");
            Componentes.accesible(busqueda, "Buscar en la ayuda");
            PauseTransition espera = new PauseTransition(Duration.millis(300));
            espera.setOnFinished(e -> {
                int caret = busqueda.getCaretPosition();
                redibujar();
                Platform.runLater(() -> {
                    busqueda.requestFocus();
                    busqueda.positionCaret(caret);
                });
            });
            busqueda.textProperty().addListener((o, a, b) -> espera.playFromStart());
        }
        return busqueda;
    }

    @Override
    protected List<Node> construir() {
        String q = busqueda().getText() == null ? "" : busqueda.getText().trim();
        List<AyudaService.Area> areas = ayuda.areas(q);
        List<AyudaService.Guia> guias = ayuda.guias(q);
        List<AyudaService.Pregunta> preguntas = ayuda.preguntas(q);
        List<Node> nodos = new ArrayList<>();
        nodos.add(Componentes.hero(null, "Ayuda", "Centro de asistencia, preguntas frecuentes y contacto directo con encargados de Mesa de Partes.",
                Componentes.lineaContexto(contexto.textoSede(), contexto.textoEscuela(), "ctx:sede")));
        nodos.add(buscador(q));
        canales = null;
        if (!q.isBlank() && areas.isEmpty() && guias.isEmpty() && preguntas.isEmpty()) {
            nodos.add(Componentes.seccion(Componentes.vacio("fth-search", "Sin resultados para «" + q + "»",
                    "Prueba con otra palabra o escríbenos por el chat de Mesa de Partes.")));
        }
        if (!areas.isEmpty()) {
            nodos.add(areas(areas));
        }
        if (q.isBlank()) {
            canales = canales();
            nodos.add(canales);
        }
        if (!guias.isEmpty()) {
            nodos.add(guias(guias));
        }
        if (!preguntas.isEmpty()) {
            nodos.add(preguntas(preguntas, !q.isBlank()));
        }
        if (q.isBlank()) {
            nodos.add(estado());
            nodos.add(sedes());
        }
        nodos.add(extraordinaria());
        return nodos;
    }

    private VBox buscador(String q) {
        Label titulo = Componentes.parrafo("¿En qué podemos ayudarte hoy?", "h2");
        titulo.setAlignment(Pos.CENTER);
        titulo.setMaxWidth(Double.MAX_VALUE);
        Label sub = Componentes.parrafo("Busca trámites, normativas, requisitos o soluciones rápidas para tu gestión académica.", "muted", "small");
        sub.setAlignment(Pos.CENTER);
        sub.setMaxWidth(Double.MAX_VALUE);
        StackPane caja = new StackPane(busqueda(), Componentes.icono("fth-search"));
        StackPane.setAlignment(caja.getChildren().get(1), Pos.CENTER_LEFT);
        StackPane.setMargin(caja.getChildren().get(1), new Insets(0, 0, 0, 14));
        caja.getStyleClass().addAll("search", "search-lg");
        FlowPane chips = Componentes.chips(Componentes.etiqueta("Búsquedas populares:", "hint"));
        chips.getStyleClass().add("centro");
        for (String p : AyudaService.POPULARES) {
            ToggleButton b = new ToggleButton();
            Componentes.fijar(b, p);
            b.getStyleClass().add("chip");
            b.setSelected(p.equals(q));
            b.setOnAction(e -> busqueda.setText(p.equals(q) ? "" : p));
            chips.getChildren().add(b);
        }
        if (!q.isBlank()) {
            chips.getChildren().add(Componentes.botonSm("Limpiar", "btn-tertiary", null, e -> busqueda.setText("")));
        }
        VBox panel = new VBox(12, titulo, sub, caja, chips);
        panel.getStyleClass().add("panel");
        VBox s = Componentes.seccion(panel);
        s.getStyleClass().add("sin-arriba");
        return s;
    }

    private VBox areas(List<AyudaService.Area> areas) {
        Rejilla g = new Rejilla(2, 340);
        for (AyudaService.Area a : areas) {
            StackPane ic = new StackPane(new FontIcon(a.icono()));
            ic.getStyleClass().add("area-ic");
            HBox arriba = new HBox(10, ic, Componentes.mayus(a.etiqueta(), "label-caps"), Componentes.espacio(), Componentes.tag("Guía oficial"));
            arriba.setAlignment(Pos.CENTER_LEFT);
            VBox puntos = new VBox(4);
            a.puntos().forEach(p -> puntos.getChildren().add(Componentes.vineta(p, "vineta")));
            VBox tarjeta = new VBox(12, arriba, Componentes.parrafo(a.titulo(), "h3"), Componentes.parrafo(a.descripcion(), "muted", "small"), puntos,
                    Componentes.espacioV(), Componentes.enlaceFlecha(a.enlace(), e -> modalArea(a)));
            tarjeta.getStyleClass().add("area");
            g.getChildren().add(tarjeta);
        }
        return Componentes.seccion(Componentes.cabeceraSeccion("Explorar por áreas temáticas", "Selecciona una categoría para ver reglamentos y guías oficiales."), g);
    }

    private void modalArea(AyudaService.Area a) {
        VBox puntos = new VBox(6);
        a.puntos().forEach(p -> puntos.getChildren().add(Componentes.vineta(p, "small")));
        VBox cuerpo = new VBox(14, Componentes.parrafo(a.descripcion(), "muted"), puntos,
                Componentes.parrafo("Guía oficial resumida. El reglamento completo está disponible en Documentos.", "hint"));
        notificador.modal(a.titulo(), cuerpo, false, Componentes.boton("Cerrar", "btn-secondary", null, e -> notificador.cerrarModal()),
                Componentes.boton("Ir a Documentos", "btn-primary", null, e -> {
                    notificador.cerrarModal();
                    ir("documentos");
                }));
    }

    private VBox canales() {
        Sede sede = contexto.sedeOPrincipal();
        VBox chat = canal("fth-message-circle", Componentes.pill("En línea · Espera: 2 min", "success"), "Chat en vivo con Mesa de Partes",
                "Conversa en tiempo real con un operador académico para subsanar observaciones.", "Horario: lunes a viernes de 08:30 a 20:00 hrs",
                Componentes.boton("Iniciar chat con asesor", "btn-primary", null, e -> chat()));
        VBox llamada = canal("fth-phone", Componentes.pill("Central telefónica activa", "info"), "Línea telefónica y asesoría",
                "Llama directamente a la central para consultar el avance de tus solicitudes o solicita que un especialista de secretaría te devuelva la llamada.",
                "Central: " + institucion.getTelefono() + " | Anexos: " + institucion.getAnexos(),
                Componentes.boton("Solicitar llamada de un encargado", "btn-secondary", null, e -> llamada()));
        VBox turno = canal("fth-tag", Componentes.pill("Turno digital sin colas", "neutral"), "Atención presencial en ventanilla",
                "Reserva un ticket digital para atención directa en ventanilla física.", "Sede " + sede.getNombre() + " · " + sede.getOficina(),
                Componentes.boton("Reservar turno presencial", "btn-secondary", null, e -> turno()));
        Rejilla g = new Rejilla(3, 260);
        g.getChildren().addAll(chat, llamada, turno);
        return Componentes.seccion(Componentes.cabeceraSeccion("Comunícate con un asesor o encargado", "Canales oficiales de soporte para resolución de dudas e incidencias."), g);
    }

    private VBox canal(String icono, Label estado, String titulo, String texto, String pie, Button accion) {
        HBox arriba = new HBox(Componentes.icono(icono, "chan-icono"), Componentes.espacio(), estado);
        arriba.setAlignment(Pos.CENTER_LEFT);
        VBox v = new VBox(12, arriba, Componentes.parrafo(titulo, "h3"), Componentes.parrafo(texto, "muted", "small"), Componentes.parrafo(pie, "hint"),
                Componentes.espacioV(), Componentes.bloque(accion));
        v.getStyleClass().add("chan");
        return v;
    }

    private void chat() {
        VBox log = new VBox(8);
        log.getStyleClass().add("chat-log");
        Label saludo = Componentes.parrafo("Hola, " + sesion.estudiante().getPrimerNombre() + ". Soy el asesor de Mesa de Partes. ¿En qué te ayudo?", "msg", "op");
        log.getChildren().add(burbuja(saludo, false));
        ScrollPane sp = new ScrollPane(log);
        sp.setFitToWidth(true);
        sp.setPrefViewportHeight(260);
        sp.getStyleClass().add("chat-scroll");
        TextField entrada = new TextField();
        Componentes.prompt(entrada, "Escribe tu mensaje…");
        Componentes.accesible(entrada, "Mensaje");
        HBox.setHgrow(entrada, Priority.ALWAYS);
        Button enviar = Componentes.boton("Enviar", "btn-primary", null, null);
        Runnable mandar = () -> {
            String t = entrada.getText().trim();
            if (t.isEmpty()) {
                return;
            }
            log.getChildren().add(burbuja(Componentes.literalParrafo(t, "msg", "me"), true));
            entrada.clear();
            Platform.runLater(() -> sp.setVvalue(1));
            PauseTransition p = new PauseTransition(Duration.millis(700));
            p.setOnFinished(e -> {
                log.getChildren().add(burbuja(Componentes.parrafo(AyudaService.respuestaChat(t), "msg", "op"), false));
                Platform.runLater(() -> sp.setVvalue(1));
            });
            p.play();
        };
        enviar.setOnAction(e -> mandar.run());
        entrada.setOnAction(e -> mandar.run());
        HBox fila = new HBox(entrada, enviar);
        fila.getStyleClass().add("copy-row");
        VBox cuerpo = new VBox(10, Componentes.parrafo("Simulación: las respuestas son automáticas de demostración.", "hint"), sp, fila);
        notificador.modal("Chat con Mesa de Partes", cuerpo, false);
        Platform.runLater(entrada::requestFocus);
    }

    private HBox burbuja(Label l, boolean mio) {
        l.setMaxWidth(380);
        HBox h = new HBox(l);
        h.setAlignment(mio ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        return h;
    }

    private void llamada() {
        Formulario f = new Formulario();
        Campo<TextField> tel = f.agregar(Formulario.numero("Número de celular", "9 dígitos", "", 9)).requerido("Ingresa tu número.")
                .patron("9\\d{8}", "Ingresa un celular válido de 9 dígitos que empiece con 9.");
        Campo<ComboBox<Opcion>> franja = f.agregar(Formulario.selector("Franja horaria", Opcion.lista(List.of("08:30 - 11:00", "11:00 - 14:00",
                "14:00 - 17:00", "17:00 - 20:00")), "Seleccionar", null)).requerido("Elige una franja.");
        Campo<TextArea> tema = f.agregar(Formulario.area("Tema", null)).requerido("Cuéntanos el tema de la llamada.").minimo(10);
        VBox cuerpo = new VBox(14, Componentes.parrafo("Un especialista de secretaría te llamará en la franja elegida.", "muted", "small"),
                tel.nodo(), franja.nodo(), tema.nodo());
        notificador.modal("Solicitar llamada", cuerpo, false, Componentes.boton("Cancelar", "btn-secondary", null, e -> notificador.cerrarModal()),
                Componentes.boton("Solicitar llamada", "btn-primary", null, e -> {
                    if (f.validar()) {
                        notificador.cerrarModal();
                        notificador.exito("Llamada programada (" + Formulario.valor(franja.control()) + "). Te llamaremos al " + tel.control().getText() + ".");
                    }
                }));
    }

    private void turno() {
        Formulario f = new Formulario();
        List<Opcion> sedes = new ArrayList<>();
        catalogo.sedes().forEach(s -> sedes.add(Opcion.de(s.getCodigo(), s.getNombreCompleto() + " · " + s.getOficina())));
        Campo<ComboBox<Opcion>> sede = f.agregar(Formulario.selector("Sede", sedes, null, contexto.sedeOPrincipal().getCodigo()));
        Campo<DatePicker> fecha = f.agregar(Formulario.fecha("Fecha", Fechas.hoy(), Fechas.hoy(), null)).requerido("Elige una fecha.");
        Campo<ComboBox<Opcion>> hora = f.agregar(Formulario.selector("Hora", Opcion.lista(List.of("08:30", "09:30", "10:30", "11:30", "14:00", "15:00", "16:00", "17:00")),
                "Seleccionar", null)).requerido("Elige una hora.");
        Campo<ComboBox<Opcion>> motivo = f.agregar(Formulario.selector("Motivo", Opcion.lista(List.of("Entrega de documentos", "Consulta de expediente",
                "Pagos y tasas", "Otro")), "Seleccionar", null)).requerido("Elige el motivo.");
        VBox cuerpo = new VBox(14, Componentes.parrafo("Elige la sede donde quieres ser atendido.", "muted", "small"), sede.nodo(),
                Formulario.rejilla(2, fecha.nodo(), hora.nodo()), motivo.nodo());
        notificador.modal("Reservar turno presencial", cuerpo, false, Componentes.boton("Cancelar", "btn-secondary", null, e -> notificador.cerrarModal()),
                Componentes.boton("Reservar turno", "btn-primary", null, e -> {
                    if (!f.validar()) {
                        return;
                    }
                    String ticket = "T-" + (100 + new Random().nextInt(900));
                    Sede s = catalogo.sede(Formulario.valor(sede.control())).orElseThrow();
                    VBox t = new VBox(6, Componentes.mayus("Tu ticket", "label-caps"), Componentes.literal(ticket, "dato-grande"),
                            Componentes.parrafo(s.getNombreCompleto() + " · " + Fechas.largo(fecha.control().getValue()) + " · " + Formulario.valor(hora.control()), "muted"),
                            Componentes.parrafo("Preséntate 5 minutos antes con tu carné universitario.", "hint"));
                    t.setAlignment(Pos.CENTER);
                    notificador.modal("Turno reservado", t, false, Componentes.boton("Listo", "btn-primary", null, x -> notificador.cerrarModal()));
                }));
    }

    private VBox guias(List<AyudaService.Guia> guias) {
        Rejilla g = new Rejilla(3, 260);
        for (AyudaService.Guia gu : guias) {
            VBox pasos = new VBox(4);
            for (int i = 0; i < gu.pasos().size(); i++) {
                Label n = Componentes.literal((i + 1) + ".", "vineta");
                Label t = Componentes.parrafo(gu.pasos().get(i), "vineta");
                HBox.setHgrow(t, Priority.ALWAYS);
                pasos.getChildren().add(new HBox(6, n, t));
            }
            VBox tarjeta = new VBox(12, new HBox(Componentes.tag(gu.paso())), Componentes.parrafo(gu.titulo(), "h3"), pasos, Componentes.espacioV(),
                    Componentes.enlaceFlecha(gu.enlace(), e -> modalGuia(gu)));
            tarjeta.getStyleClass().add("guide");
            g.getChildren().add(tarjeta);
        }
        return Componentes.seccion(Componentes.cabeceraSeccion("Guías esenciales de resolución rápida", "Procedimientos clave para gestionar tus trámites universitarios."), g);
    }

    private void modalGuia(AyudaService.Guia g) {
        VBox lista = new VBox(10);
        for (int i = 0; i < g.pasos().size(); i++) {
            StackPane n = new StackPane(Componentes.literal(String.valueOf(i + 1)));
            n.getStyleClass().add("tl-dot");
            Label t = Componentes.parrafo(g.pasos().get(i));
            HBox.setHgrow(t, Priority.ALWAYS);
            HBox fila = new HBox(14, n, t);
            fila.getStyleClass().add("tl-item");
            lista.getChildren().add(fila);
        }
        String destino = "documentos".equals(g.destino()) ? "Documentos" : "Mis Solicitudes";
        notificador.modal(g.titulo(), lista, false, Componentes.boton("Cerrar", "btn-secondary", null, e -> notificador.cerrarModal()),
                Componentes.boton("Ir a " + destino, "btn-primary", null, e -> {
                    notificador.cerrarModal();
                    ir(g.destino());
                }));
    }

    private VBox preguntas(List<AyudaService.Pregunta> preguntas, boolean abrirTodas) {
        List<Node> filas = new ArrayList<>();
        for (AyudaService.Pregunta p : preguntas) {
            int indice = AyudaService.PREGUNTAS.indexOf(p);
            boolean abierta = abrirTodas || abiertas.contains(indice);
            VBox textos = new VBox(2, Componentes.mayus(p.categoria(), "label-caps"), Componentes.parrafo(p.pregunta(), "faq-pregunta"));
            HBox.setHgrow(textos, Priority.ALWAYS);
            FontIcon flecha = new FontIcon(abierta ? "fth-chevron-up" : "fth-chevron-down");
            HBox q = new HBox(textos, flecha);
            q.getStyleClass().add("faq-q");
            q.setFocusTraversable(true);
            Label respuesta = Componentes.parrafo(p.respuesta(), "faq-a");
            respuesta.setVisible(abierta);
            respuesta.setManaged(abierta);
            Componentes.accesible(q, p.pregunta());
            Runnable alternar = () -> {
                boolean ahora = !respuesta.isVisible();
                respuesta.setVisible(ahora);
                respuesta.setManaged(ahora);
                flecha.setIconLiteral(ahora ? "fth-chevron-up" : "fth-chevron-down");
                if (ahora) {
                    abiertas.add(indice);
                } else {
                    abiertas.remove(indice);
                }
            };
            q.setOnMouseClicked(e -> alternar.run());
            q.setOnKeyPressed(e -> {
                if (e.getCode() == javafx.scene.input.KeyCode.ENTER || e.getCode() == javafx.scene.input.KeyCode.SPACE) {
                    alternar.run();
                }
            });
            VBox faq = new VBox(q, respuesta);
            faq.getStyleClass().add("faq");
            filas.add(faq);
        }
        return Componentes.seccion(Componentes.cabeceraSeccion("Preguntas frecuentes sobre trámites y servicios", "Respuestas a las consultas más comunes de la comunidad universitaria."),
                Componentes.lista(filas));
    }

    private VBox estado() {
        VBox textos = new VBox(2, Componentes.parrafo("Todos los sistemas de atención y trámites se encuentran operativos al 100%", "status-titulo"),
                Componentes.parrafo("Mesa de Partes Virtual, Validación Reniec, Pasarela de Pagos y Custodia funcionando sin demoras.", "small", "muted"));
        HBox.setHgrow(textos, Priority.ALWAYS);
        FlowPane chips = Componentes.chips();
        for (String t : List.of("Trámites", "SUNEDU API", "Reservas", "Equipos")) {
            chips.getChildren().add(Componentes.pill(t + ": OK", "success"));
        }
        chips.setMaxWidth(380);
        HBox barra = new HBox(new FontIcon("fth-check-circle"), textos, chips);
        barra.getStyleClass().add("status-bar");
        return Componentes.seccion(barra);
    }

    private VBox sedes() {
        Sede actual = contexto.sede();
        List<Sede> orden = new ArrayList<>();
        if (actual != null) {
            orden.add(actual);
        }
        catalogo.sedes().stream().filter(s -> actual == null || !s.getId().equals(actual.getId())).forEach(orden::add);
        Rejilla g = new Rejilla(2, 340);
        for (Sede s : orden) {
            HBox cab = new HBox(8, Componentes.parrafo("Mesa de Partes · " + s.getNombreCompleto(), "h3"), Componentes.espacio());
            if (actual != null && s.getId().equals(actual.getId())) {
                cab.getChildren().add(Componentes.pill("Tu sede", "info"));
            }
            String atiende = String.join(", ", catalogo.escuelasDe(s).stream().map(e -> e.getNombre()).toList());
            VBox o = new VBox(12, cab, Componentes.kv("Ubicación", s.getOficina(), "Horario", s.getHorario(),
                    "Atiende a", atiende.isBlank() ? "Auditorios, eventos y oficinas centrales" : atiende, "Correo", copiar(institucion.getCorreoMesaPartes())));
            o.getStyleClass().add("office");
            g.getChildren().add(o);
        }
        VBox ti = new VBox(12, Componentes.parrafo("Defensoría Universitaria y Mesa de Ayuda de TI", "h3"), Componentes.kv(
                "Ubicación", "Edificio de Bienestar Universitario · Piso 1 (Oficina 100)", "Atención técnica", "Lunes a viernes de 09:00 a 16:30 hrs",
                "Soporte y credenciales", copiar(institucion.getCorreoSoporte()), "Defensoría legal", copiar(institucion.getCorreoDefensoria())));
        ti.getStyleClass().add("office");
        g.getChildren().add(ti);
        return Componentes.seccion(Componentes.cabeceraSeccion("Sedes y directorio institucional",
                "Mesa de Partes en Ayabacas, Capilla y Salida Cuzco. Primero aparece tu sede seleccionada."), g);
    }

    private HBox copiar(String correo) {
        Button b = Componentes.botonSm("Copiar", "btn-tertiary", null, e -> {
            Portapapeles.copiar(correo);
            notificador.exito("Correo copiado.");
        });
        HBox h = new HBox(6, Componentes.literal(correo, "dd"), b);
        h.setAlignment(Pos.CENTER_LEFT);
        return h;
    }

    private VBox extraordinaria() {
        VBox izquierda = new VBox(8, Componentes.parrafo("¿No encontraste la respuesta a tu trámite o consulta?", "cta-titulo"),
                Componentes.parrafo("Si tu caso corresponde a una excepción académica, solicitud de gracia, convalidación especial o reclamo formal, ingresa un expediente extraordinario en Mesa de Partes Virtual para evaluación de Decanato.", "muted", "small"),
                new HBox(Componentes.tag("Tiempo promedio de respuesta: 3 a 5 días hábiles")));
        izquierda.setMaxWidth(640);
        HBox.setHgrow(izquierda, Priority.ALWAYS);
        Button ingresar = Componentes.boton("Ingresar solicitud extraordinaria", "btn-primary", "fth-arrow-right", e -> modalExtraordinaria());
        ingresar.setContentDisplay(ContentDisplay.RIGHT);
        VBox utilidad;
        if (util == null) {
            utilidad = new VBox(6, Componentes.etiqueta("¿Te resultó útil esta sección de ayuda?", "hint"), new HBox(6,
                    Componentes.botonSm("Sí", "btn-secondary", null, e -> {
                        util = true;
                        redibujar();
                    }), Componentes.botonSm("No", "btn-secondary", null, e -> {
                        util = false;
                        redibujar();
                    })));
        } else {
            utilidad = new VBox(Componentes.parrafo(util ? "Gracias por tu respuesta." : "Gracias. Usaremos tu respuesta para mejorar esta sección.", "hint"));
        }
        VBox derecha = new VBox(12, ingresar, utilidad);
        HBox banda = new HBox(izquierda, derecha);
        banda.getStyleClass().add("cta-band");
        return Componentes.seccion(banda);
    }

    private void modalExtraordinaria() {
        Formulario f = new Formulario();
        Campo<ComboBox<Opcion>> tipo = f.agregar(Formulario.selector("Tipo de caso", Opcion.lista(List.of("Excepción académica", "Solicitud de gracia",
                "Convalidación especial", "Reclamo formal")), "Seleccionar", null)).requerido("Elige el tipo de caso.");
        Campo<TextArea> desc = f.agregar(Formulario.area("Describe tu caso", null)).requerido("Describe tu caso.").minimo(20);
        Campo<Formulario.Archivo> adj = f.agregar(Formulario.archivo("Sustento (opcional)", "*.pdf *.jpg *.png", null, notificador, null));
        VBox cuerpo = new VBox(14, Componentes.parrafo("Será evaluada por Decanato. Tiempo promedio de respuesta: 3 a 5 días hábiles.", "muted", "small"),
                tipo.nodo(), desc.nodo(), adj.nodo());
        notificador.modal("Solicitud extraordinaria", cuerpo, false, Componentes.boton("Cancelar", "btn-secondary", null, e -> notificador.cerrarModal()),
                Componentes.boton("Enviar solicitud", "btn-primary", null, e -> {
                    if (!f.validar()) {
                        return;
                    }
                    try {
                        Expediente x = expedientes.extraordinaria(Formulario.valor(tipo.control()), desc.control().getText(), adj.texto());
                        notificador.cerrarModal();
                        notificador.exito("Expediente " + x.getCodigo() + " registrado. Puedes seguirlo en Mis Solicitudes.");
                    } catch (pe.edu.unaj.andara.exception.NegocioException ex) {
                        notificador.error(ex.getMessage());
                    }
                }));
    }
}
