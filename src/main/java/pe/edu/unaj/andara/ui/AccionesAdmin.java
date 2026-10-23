package pe.edu.unaj.andara.ui;

import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.layout.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pe.edu.unaj.andara.exception.NegocioException;
import pe.edu.unaj.andara.model.*;
import pe.edu.unaj.andara.service.*;
import pe.edu.unaj.andara.util.Fechas;
import pe.edu.unaj.andara.util.Montos;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class AccionesAdmin {

    private final Notificador notificador;
    private final AdminService admin;
    private final CatalogoService catalogo;
    private final AccionesPago accionesPago;
    private final DocumentoGeneradoService generador;

    public DatosSolicitante solicitante(Expediente s) {
        if (s.getSolicitante() != null && s.getSolicitante().getNombre() != null) {
            return s.getSolicitante();
        }
        if (s.getPago() != null && s.getPago().getSolicitante() != null && s.getPago().getSolicitante().getNombre() != null) {
            return s.getPago().getSolicitante();
        }
        Persona u = s.getPersona();
        return DatosSolicitante.builder().tipo(u.getTipo()).nombre(u.getNombre()).dni(u.getDni()).codigo(u.getCodigo())
                .escuela(u.getEscuela() == null ? null : u.getEscuela().getCodigo()).correo(u.getCorreo()).build();
    }

    public void revisar(String codigo, Runnable alCambiar) {
        Expediente s = admin.expediente(codigo);
        Pago p = s.getPago();
        DatosSolicitante sol = solicitante(s);
        FlowPane chips = Componentes.chips(Componentes.pill(s.getEstado(), s.getTono()), Componentes.literal(s.getCodigo(), "tag"),
                Componentes.tag("Sede " + s.getSede().getNombre()));
        List<Object> datos = new ArrayList<>(List.of("Nombre", Componentes.literalParrafo(sol.getNombre(), "dd"), "Tipo", sol.getTipo(),
                "DNI", Componentes.literal(sol.getDni() == null ? "—" : sol.getDni(), "dd"), "Código", Componentes.literal(sol.getCodigo() == null ? "—" : sol.getCodigo(), "dd")));
        if (sol.getEscuela() != null && catalogo.escuela(sol.getEscuela()).isPresent()) {
            datos.add("Escuela");
            datos.add(Componentes.literalParrafo("EP " + catalogo.nombreEscuela(sol.getEscuela()), "dd"));
        }
        if (sol.getCorreo() != null) {
            datos.add("Correo");
            datos.add(Componentes.literal(sol.getCorreo(), "dd"));
        }
        VBox izquierda = new VBox(8, Componentes.mayus("Solicitante", "label-caps"), Componentes.kv(datos.toArray()));
        VBox derecha = new VBox(8, Componentes.mayus("Pago", "label-caps"));
        if (p != null) {
            List<Object> pagoDatos = new ArrayList<>(List.of("Orden", Componentes.literal(p.getCodigo(), "dd"), "Monto", Montos.soles(p.getTotal()),
                    "Estado", p.isPagado() ? Componentes.pill("Pagado · " + p.getMetodo().getEtiqueta(), "success") : Componentes.pill("Pendiente", "warning")));
            if (p.getBoleta() != null) {
                pagoDatos.add("Comprobante");
                pagoDatos.add(Componentes.literal(p.getBoleta(), "dd"));
            }
            derecha.getChildren().add(Componentes.kv(pagoDatos.toArray()));
        } else {
            derecha.getChildren().add(Componentes.parrafo("Este trámite no tiene costo.", "muted", "small"));
        }
        if (s.getObservacion() != null && s.getGrupo() == GrupoExpediente.OBSERVADO) {
            VBox obs = new VBox(Componentes.etiqueta("Observación", "obs-titulo"), Componentes.parrafo(s.getObservacion(), "obs-texto"));
            obs.getStyleClass().add("obs");
            derecha.getChildren().add(obs);
        }
        GridPane grid = new GridPane();
        grid.setHgap(24);
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setPercentWidth(50);
        ColumnConstraints c2 = new ColumnConstraints();
        c2.setPercentWidth(50);
        grid.getColumnConstraints().addAll(c1, c2);
        grid.add(izquierda, 0, 0);
        grid.add(derecha, 1, 0);
        GridPane.setValignment(derecha, javafx.geometry.VPos.TOP);
        VBox cuerpo = new VBox(14, chips, grid);
        if (p != null && p.getTramite() != null && p.getTramite().generaDocumento() && p.isPagado()) {
            cuerpo.getChildren().add(new HBox(Componentes.botonSm("Ver documento generado", "btn-tertiary", "fth-file-text", e -> accionesPago.documento(p.getCodigo()))));
        }
        List<Node> pie = new ArrayList<>();
        Button observar = Componentes.boton("Observar", "btn-danger", null, e -> observar(s, alCambiar));
        observar.setDisable(s.getGrupo() != GrupoExpediente.TRAMITE);
        pie.add(observar);
        if (s.getGrupo() == GrupoExpediente.LISTO && !s.isEntregado()) {
            pie.add(Componentes.boton("Marcar entregado", "btn-secondary", null, e -> {
                admin.entregar(s.getCodigo());
                notificador.cerrarModal();
                alCambiar.run();
                notificador.exito("Entrega registrada.");
            }));
        }
        boolean puede = AdminService.puedeEmitir(s);
        Button emitir = Componentes.boton(puede ? "Emitir y marcar listo para recoger" : p != null && !p.isPagado() ? "Esperando pago" : "Ya atendido",
                "btn-primary", null, e -> {
                    try {
                        admin.emitir(s.getCodigo());
                        notificador.cerrarModal();
                        alCambiar.run();
                        notificador.exito(s.getCodigo() + " emitido y listo para recoger.");
                    } catch (NegocioException ex) {
                        notificador.error(ex.getMessage());
                    }
                });
        emitir.setDisable(!puede);
        pie.add(emitir);
        notificador.modal(s.getTitulo(), cuerpo, true, pie.toArray(new Node[0]));
    }

    private void observar(Expediente s, Runnable alCambiar) {
        Formulario f = new Formulario();
        Campo<TextArea> motivo = f.agregar(Formulario.area("Motivo de la observación", "Ej. Falta adjuntar la copia del DNI legible."))
                .requerido("Explica qué debe corregir el solicitante.").minimo(15);
        VBox cuerpo = new VBox(14, Componentes.parrafo(s.getCodigo() + " · " + s.getTitulo() + ". El solicitante verá este motivo y podrá subsanar.", "muted", "small"), motivo.nodo());
        notificador.modal("Observar trámite", cuerpo, false, Componentes.boton("Cancelar", "btn-secondary", null, e -> notificador.cerrarModal()),
                Componentes.boton("Enviar observación", "btn-primary", null, e -> {
                    if (!f.validar()) {
                        return;
                    }
                    try {
                        admin.observar(s.getCodigo(), motivo.control().getText());
                        notificador.cerrarModal();
                        alCambiar.run();
                        notificador.exito("Observación enviada al solicitante.");
                    } catch (NegocioException ex) {
                        notificador.error(ex.getMessage());
                    }
                }));
    }

    public void reembolsar(Pago p, Runnable alCambiar) {
        notificador.confirmar("¿Reembolsar este pago?", p.getConcepto() + " · " + Montos.soles(p.getTotal()) + " a " + p.getPersona().getNombre()
                + ". Tesorería devolverá el monto por el mismo medio de pago.", "Reembolsar", true, () -> {
            admin.reembolsar(p.getCodigo());
            alCambiar.run();
            notificador.exito("Reembolso registrado.");
        });
    }

    public void aprobarReserva(Reserva r, Runnable alCambiar) {
        admin.aprobarReserva(r.getId());
        alCambiar.run();
        notificador.exito("Reserva aprobada.");
    }

    public void rechazarReserva(Reserva r, Runnable alCambiar) {
        Formulario f = new Formulario();
        Campo<TextArea> motivo = f.agregar(Formulario.area("Motivo", null)).requerido("Indica el motivo del rechazo.").minimo(10);
        VBox cuerpo = new VBox(14, Componentes.literalParrafo(r.getEspacio().getNombre() + " · " + Fechas.dmy(r.getFecha()) + " " + r.getHorario(), "muted", "small"), motivo.nodo());
        notificador.modal("Rechazar reserva", cuerpo, false, Componentes.boton("Cancelar", "btn-secondary", null, e -> notificador.cerrarModal()),
                Componentes.boton("Rechazar", "btn-primary", null, e -> {
                    if (f.validar()) {
                        admin.rechazarReserva(r.getId(), motivo.control().getText());
                        notificador.cerrarModal();
                        alCambiar.run();
                        notificador.exito("Reserva rechazada.");
                    }
                }));
    }

    public void aprobarPrestamo(Prestamo p, Runnable alCambiar) {
        admin.aprobarPrestamo(p.getId());
        alCambiar.run();
        notificador.exito("Préstamo aprobado.");
    }

    public void rechazarPrestamo(Prestamo p, Runnable alCambiar) {
        notificador.confirmar("¿Rechazar este préstamo?", p.getNombre() + " (" + p.getCodigo() + ").", "Rechazar", true, () -> {
            admin.rechazarPrestamo(p.getId());
            alCambiar.run();
            notificador.exito("Préstamo rechazado.");
        });
    }

    public void devolverPrestamo(Prestamo p, Runnable alCambiar) {
        admin.devolverPrestamo(p.getId());
        alCambiar.run();
        notificador.exito("Devolución registrada.");
    }

    public void devolverCustodia(Runnable alCambiar) {
        admin.devolverCustodia();
        alCambiar.run();
        notificador.exito("Devolución registrada.");
    }

    public DocumentoGeneradoService generador() {
        return generador;
    }
}
