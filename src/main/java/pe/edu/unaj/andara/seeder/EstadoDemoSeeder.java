package pe.edu.unaj.andara.seeder;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.unaj.andara.config.JsonRecursos;
import pe.edu.unaj.andara.model.*;
import pe.edu.unaj.andara.repository.*;
import pe.edu.unaj.andara.util.Fechas;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class EstadoDemoSeeder {

    private static final Pattern FECHA_DMY = Pattern.compile("(\\d{2})/(\\d{2})/(\\d{4})");

    private final JsonRecursos json;
    private final SedeRepository sedes;
    private final EspacioRepository espacios;
    private final EquipoRepository equipos;
    private final TramiteRepository tramites;
    private final PersonaRepository personas;
    private final PagoRepository pagos;
    private final ExpedienteRepository expedientes;
    private final ReservaRepository reservas;
    private final PrestamoRepository prestamos;
    private final PrestamoActivoRepository activos;
    private final FavoritoRepository favoritos;
    private final DocumentoPersonalRepository documentosPersonales;
    private final NotificacionRepository notificaciones;
    private final MatriculaRepository matriculas;
    private final MensajeEnviadoRepository mensajes;
    private final ConsultaAsesorRepository consultas;
    private final CambioPendienteRepository cambios;
    private final UsoCarneRepository usos;
    private final AjustesRepository ajustes;

    private long desfaseDias;

    @Transactional
    public boolean sembrar() {
        if (ajustes.existsById(1L)) {
            return false;
        }
        cargar(false);
        return true;
    }

    @Transactional
    public void restablecer() {
        mensajes.deleteAllInBatch();
        consultas.deleteAll();
        matriculas.deleteAllInBatch();
        expedientes.deleteAll();
        reservas.deleteAllInBatch();
        prestamos.deleteAllInBatch();
        activos.deleteAllInBatch();
        favoritos.deleteAllInBatch();
        documentosPersonales.deleteAllInBatch();
        notificaciones.deleteAllInBatch();
        cambios.deleteAllInBatch();
        usos.deleteAllInBatch();
        pagos.deleteAll();
        expedientes.flush();
        pagos.flush();
        equipos.findAll().forEach(e -> e.setStock(e.getUnidadesIniciales()));
        cargar(true);
    }

    private void cargar(boolean conservar) {
        JsonNode d = json.demo();
        LocalDate referencia = LocalDate.parse(d.path("fechaReferencia").asText());
        desfaseDias = ChronoUnit.DAYS.between(referencia, Fechas.hoy());
        Persona yo = personas.findFirstByPrincipalTrue().orElseThrow();

        for (JsonNode p : d.path("pagos")) {
            List<ItemPago> items = new ArrayList<>();
            for (JsonNode i : p.path("items")) {
                items.add(new ItemPago(i.get(0).asText(), i.get(1).asInt(), new BigDecimal(i.get(2).asText())));
            }
            pagos.save(Pago.builder().codigo(p.path("id").asText()).persona(persona(p.path("user").asText()))
                    .tipo(TipoPago.desdeClave(p.path("tipo").asText()))
                    .tramite(tramite(p.path("tupa").asText(null))).concepto(p.path("concepto").asText())
                    .estado(EstadoPago.valueOf(p.path("estado").asText().toUpperCase()))
                    .creado(momento(p.path("creado").asText(null))).vence(fecha(p.path("vence").asText(null)))
                    .pagado(momento(p.path("pagado").asText(null)))
                    .metodo(MetodoPago.desdeClave(p.path("metodo").asText(null)))
                    .operacion(p.path("op").asText(null)).boleta(p.path("boleta").asText(null))
                    .sede(sede(p.path("sede").asText("capilla"))).items(items).build());
        }

        List<JsonNode> otrosExp = lista(d.path("others").path("sols"));
        List<JsonNode> propiosExp = lista(d.path("sols"));
        for (int i = otrosExp.size() - 1; i >= 0; i--) {
            guardarExpediente(otrosExp.get(i), null, true);
        }
        for (int i = propiosExp.size() - 1; i >= 0; i--) {
            guardarExpediente(propiosExp.get(i), yo, false);
        }

        List<JsonNode> otrasRes = lista(d.path("others").path("reservas"));
        List<JsonNode> propiasRes = lista(d.path("reservas"));
        for (int i = otrasRes.size() - 1; i >= 0; i--) {
            guardarReserva(otrasRes.get(i), null);
        }
        for (int i = propiasRes.size() - 1; i >= 0; i--) {
            guardarReserva(propiasRes.get(i), yo);
        }

        List<JsonNode> otrosHist = lista(d.path("others").path("hist"));
        List<JsonNode> propiosHist = lista(d.path("hist"));
        for (int i = otrosHist.size() - 1; i >= 0; i--) {
            guardarPrestamo(otrosHist.get(i), null, true);
        }
        for (int i = propiosHist.size() - 1; i >= 0; i--) {
            guardarPrestamo(propiosHist.get(i), yo, false);
        }

        JsonNode l = d.path("loan");
        activos.save(PrestamoActivo.builder().codigo(l.path("code").asText()).persona(yo)
                .equipo(l.path("item").asText()).retiro(l.path("retiro").asText())
                .limite(LocalDateTime.now(Fechas.reloj()).plusHours(2).plusMinutes(15).withSecond(0).withNano(0))
                .renovado(l.path("renewed").asBoolean()).incidencia(l.path("incident").asText(null))
                .lugar(l.path("place").asText()).devuelto(false).build());

        favoritos(d.path("favDocs"), TipoFavorito.DOCUMENTO);
        favoritos(d.path("myFav"), TipoFavorito.MI_DOCUMENTO);
        favoritos(d.path("favEq"), TipoFavorito.EQUIPO);
        favoritos(d.path("favSpaces"), TipoFavorito.ESPACIO);

        for (JsonNode m : d.path("myDocs")) {
            documentosPersonales.save(DocumentoPersonal.builder().codigo(m.path("id").asText())
                    .nombre(m.path("name").asText()).build());
        }

        notificaciones(d.path("notifs"), Rol.ESTUDIANTE);
        notificaciones(d.path("adminNotifs"), Rol.ADMINISTRADOR);

        JsonNode mt = d.path("matricula");
        matriculas.save(Matricula.builder().periodo(mt.path("periodo").asText())
                .estado("confirmada".equals(mt.path("estado").asText()) ? EstadoMatricula.CONFIRMADA : EstadoMatricula.PENDIENTE)
                .pago(pagos.findByCodigo(mt.path("pago").asText()).orElseThrow()).build());

        List<JsonNode> listaUsos = lista(d.path("usosCarne"));
        for (int i = listaUsos.size() - 1; i >= 0; i--) {
            JsonNode u = listaUsos.get(i);
            usos.save(UsoCarne.builder().lugar(u.path("lugar").asText())
                    .fecha(Fechas.hoy().minusDays(u.path("diasAtras").asInt()).atTime(LocalTime.parse(u.path("hora").asText())))
                    .concedido(u.path("ok").asBoolean()).build());
        }

        JsonNode ctx = d.path("ctx");
        JsonNode pref = d.path("preferenciasAviso");
        String escuela = ctx.path("fac").asText("all");
        Ajustes a = ajustes.findById(1L).orElseGet(() -> Ajustes.builder().id(1L).maximizada(false).build());
        if (!conservar || a.getIdioma() == null) {
            a.setIdioma(d.path("idioma").asText("es"));
        }
        a.setSede(ctx.path("sede").asText("capilla"));
        a.setEscuela(Ajustes.TODAS.equals(escuela) ? null : escuela);
        a.setAvisoWhatsapp(pref.path("whatsapp").asBoolean(true));
        a.setAvisoCorreo(pref.path("correo").asBoolean(true));
        a.setAvisoApp(pref.path("app").asBoolean(true));
        if (!conservar) {
            a.setSesion(null);
            a.setSesionDesde(null);
            a.setReducirMovimiento(false);
        }
        a.setVersion(d.path("v").asInt(5));
        ajustes.save(a);
    }

    private void guardarExpediente(JsonNode s, Persona propietario, boolean desplazar) {
        Persona persona = propietario != null ? propietario : persona(s.path("user").asText());
        Expediente e = Expediente.builder().codigo(s.path("id").asText()).persona(persona)
                .sede(s.has("sede") ? sede(s.path("sede").asText()) : persona.getSede())
                .clase(ClaseExpediente.valueOf(s.path("kind").asText("doc").toUpperCase()))
                .tramite(tramite(s.path("tupa").asText(null)))
                .pago(s.hasNonNull("pago") ? pagos.findByCodigo(s.path("pago").asText()).orElse(null) : null)
                .grupo(GrupoExpediente.desdeClave(s.path("group").asText()))
                .icono(s.path("icon").asText("doc")).rotuloIcono(s.path("iconLabel").asText(""))
                .tono(s.path("tone").asText("neutral")).estado(s.path("status").asText())
                .titulo(s.path("title").asText()).descripcion(s.path("desc").asText(null))
                .paso(s.path("step").asInt(1)).observacion(s.path("obs").asText(null))
                .destacado(s.path("flag").asBoolean(false)).nota(s.path("note").asText(null))
                .fecha(momento(s.path("fecha").asText(null))).build();
        for (JsonNode m : s.path("meta")) {
            String valor = m.get(1).asText();
            e.getDatos().add(new DatoExpediente(m.get(0).asText(), desplazar ? desplazarTexto(valor) : valor));
        }
        s.path("tags").forEach(t -> e.getEtiquetas().add(t.asText()));
        for (JsonNode a : s.path("acts")) {
            String accion = a.get(1).asText();
            String texto = "route-sol".equals(accion) ? "Seguimiento en vivo" : a.get(2).asText();
            e.getAcciones().add(new AccionExpediente(a.get(0).asText(), accion, texto));
        }
        JsonNode sol = s.path("solicitante");
        if (!sol.isMissingNode()) {
            e.setSolicitante(DatosSolicitante.builder().tipo(sol.path("tipo").asText()).nombre(sol.path("nombre").asText())
                    .dni(sol.path("dni").asText()).codigo(sol.path("codigo").asText())
                    .escuela(sol.path("fac").asText(null)).correo(sol.path("correo").asText()).build());
        }
        expedientes.save(e);
    }

    private void guardarReserva(JsonNode r, Persona propietario) {
        Espacio espacio = espacios.findFirstByNombre(r.path("space").asText()).orElseThrow();
        reservas.save(Reserva.builder().codigo(r.path("id").asText())
                .persona(propietario != null ? propietario : persona(r.path("user").asText()))
                .espacio(espacio).fecha(fecha(r.path("date").asText())).horario(r.path("hora").asText())
                .estado(EstadoReserva.desdeEtiqueta(r.path("status").asText())).motivo(r.path("motivo").asText())
                .asistentes(r.path("asist").asInt()).nota(r.path("nota").asText(null)).build());
    }

    private void guardarPrestamo(JsonNode h, Persona propietario, boolean desplazar) {
        String detalle = desplazar ? desplazarTexto(h.path("sub").asText()) : h.path("sub").asText();
        String sede = detalle.contains("Ayabacas") ? "ayabacas" : detalle.contains("Salida Cuzco") ? "salida-cuzco" : "capilla";
        Equipo equipo = equipos.findAll().stream().filter(x -> h.path("name").asText().startsWith(x.getNombre()))
                .findFirst().orElse(null);
        prestamos.save(Prestamo.builder().codigo(h.path("code").asText()).clave(h.path("id").asText())
                .persona(propietario != null ? propietario : persona(h.path("user").asText()))
                .equipo(equipo).nombre(h.path("name").asText()).detalle(detalle)
                .estado(EstadoPrestamo.desdeEtiqueta(h.path("status").asText())).sede(sede(sede)).build());
    }

    private void favoritos(JsonNode codigos, TipoFavorito tipo) {
        codigos.forEach(c -> favoritos.save(Favorito.builder().tipo(tipo).codigo(c.asText()).build()));
    }

    private void notificaciones(JsonNode lista, Rol rol) {
        List<JsonNode> l = lista(lista);
        LocalDateTime base = Fechas.ahora().minusHours(l.size());
        for (int i = l.size() - 1; i >= 0; i--) {
            JsonNode n = l.get(i);
            notificaciones.save(Notificacion.builder().destinatario(rol).texto(n.path("text").asText())
                    .ruta(n.path("to").asText()).leida(n.path("read").asBoolean())
                    .creada(base.plusHours(l.size() - i)).build());
        }
    }

    private List<JsonNode> lista(JsonNode nodo) {
        List<JsonNode> l = new ArrayList<>();
        nodo.forEach(l::add);
        return l;
    }

    private Persona persona(String codigo) {
        return personas.findByCodigo(codigo).orElseThrow(() -> new IllegalStateException("Persona " + codigo));
    }

    private Sede sede(String codigo) {
        return sedes.findByCodigo(codigo).orElseThrow();
    }

    private Tramite tramite(String codigo) {
        return codigo == null || codigo.isBlank() ? null : tramites.findByCodigo(codigo).orElse(null);
    }

    private LocalDate fecha(String texto) {
        LocalDate f = Fechas.leerFecha(texto);
        return f == null ? null : f.plusDays(desfaseDias);
    }

    private LocalDateTime momento(String texto) {
        LocalDateTime m = Fechas.leerMomento(texto);
        return m == null ? null : m.plusDays(desfaseDias);
    }

    private String desplazarTexto(String texto) {
        if (texto == null || desfaseDias == 0) {
            return texto;
        }
        Matcher m = FECHA_DMY.matcher(texto);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            LocalDate f = LocalDate.of(Integer.parseInt(m.group(3)), Integer.parseInt(m.group(2)),
                    Integer.parseInt(m.group(1))).plusDays(desfaseDias);
            m.appendReplacement(sb, Fechas.dmy(f));
        }
        m.appendTail(sb);
        return sb.toString();
    }
}
