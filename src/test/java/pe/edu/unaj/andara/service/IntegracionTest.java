package pe.edu.unaj.andara.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import pe.edu.unaj.andara.BaseTemporal;
import pe.edu.unaj.andara.exception.NegocioException;
import pe.edu.unaj.andara.model.*;
import pe.edu.unaj.andara.repository.*;
import pe.edu.unaj.andara.util.Fechas;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class IntegracionTest {

    @DynamicPropertySource
    static void base(DynamicPropertyRegistry registro) throws Exception {
        BaseTemporal.configurar(registro, "andara-integracion");
    }

    @Autowired private DemoService demo;
    @Autowired private PagoService pagos;
    @Autowired private CatalogoService catalogo;
    @Autowired private SesionService sesion;
    @Autowired private ExpedienteService expedientes;
    @Autowired private AdminService admin;
    @Autowired private NotificacionService notificaciones;
    @Autowired private ConexionService conexion;
    @Autowired private ReservaService reservas;
    @Autowired private OcupacionService ocupacion;
    @Autowired private CarneService carne;
    @Autowired private SeguimientoService seguimiento;
    @Autowired private AnaliticaService analitica;
    @Autowired private ContextoService contexto;
    @Autowired private FavoritoService favoritos;
    @Autowired private ReservaRepository reservaRepository;
    @Autowired private MensajeEnviadoRepository mensajes;
    @Autowired private AjustesService ajustes;
    @Autowired private ConsultaAsesorService consultas;

    @BeforeEach
    void preparar() {
        demo.restablecer();
        conexion.simularSinConexion(false);
        conexion.redProperty().set(true);
    }

    @AfterEach
    void limpiar() {
        conexion.simularSinConexion(false);
    }

    private DatosSolicitante yo() {
        Persona p = sesion.estudiante();
        return DatosSolicitante.builder().tipo("Estudiante").nombre(p.getNombre()).dni(p.getDni()).codigo(p.getCodigo())
                .escuela("iss").correo(p.getCorreo()).build();
    }

    @Test
    void consultaConAsesorLimitadaYAmpliable() {
        Expediente e = expedientes.mios().stream().filter(ConsultaAsesorService::disponible).findFirst().orElseThrow();
        ConsultaAsesor c = consultas.abrir(e.getCodigo());
        assertThat(c.getMensajes().get(0).getTexto()).contains(e.getCodigo()).contains(e.getTitulo());
        assertThat(c.restantes()).isEqualTo(3);
        assertThatThrownBy(() -> consultas.solicitarAmpliacion(e.getCodigo())).isInstanceOf(NegocioException.class);
        for (int i = 0; i < 3; i++) {
            consultas.preguntar(e.getCodigo(), "¿Cuándo estará listo?");
        }
        assertThat(consultas.abrir(e.getCodigo()).restantes()).isZero();
        assertThatThrownBy(() -> consultas.preguntar(e.getCodigo(), "Otra")).isInstanceOf(NegocioException.class);
        assertThat(consultas.solicitarAmpliacion(e.getCodigo()).getAmpliacion()).isEqualTo(EstadoAmpliacion.PENDIENTE);
        ConsultaAsesor ampliada = consultas.evaluarAmpliacion(e.getCodigo());
        assertThat(ampliada.getAmpliacion()).isEqualTo(EstadoAmpliacion.APROBADA);
        assertThat(ampliada.restantes()).isEqualTo(3);
        assertThatThrownBy(() -> consultas.solicitarAmpliacion(e.getCodigo())).isInstanceOf(NegocioException.class);
        Expediente listo = expedientes.mios().stream().filter(x -> !ConsultaAsesorService.disponible(x)).findFirst().orElseThrow();
        assertThatThrownBy(() -> consultas.abrir(listo.getCodigo())).isInstanceOf(NegocioException.class);
    }

    @Test
    void ingresoYPermisosPorRol() {
        assertThatThrownBy(() -> sesion.ingresar(Rol.ESTUDIANTE, "2024207009", "mal")).isInstanceOf(NegocioException.class);
        assertThat(sesion.ingresar(Rol.ESTUDIANTE, "72458913", "andara2026")).isEqualTo(Rol.ESTUDIANTE);
        assertThat(SesionService.puedeAcceder(sesion.rol(), "admin")).isFalse();
        sesion.cerrar();
        assertThat(sesion.ingresar(Rol.ADMINISTRADOR, "RMAMANI@unaj.edu.pe", "admin2026")).isEqualTo(Rol.ADMINISTRADOR);
        assertThat(sesion.esAdministrador()).isTrue();
        sesion.cerrar();
        assertThat(sesion.conectado()).isFalse();
    }

    @Test
    void efectosDelPagoEnOrdenExpedienteYMatricula() {
        Tramite t = catalogo.tramite("t-cert").orElseThrow();
        Pago p = pagos.crearOrden(t, yo(), 1, "Trabajo", "capilla", null);
        Expediente e = expedientes.dePago(p).orElseThrow();
        assertThat(e.getEstado()).isEqualTo("Pago pendiente en Andara Pay");
        long antes = notificaciones.noLeidas(Rol.ADMINISTRADOR);
        Pago pagado = pagos.completar(p.getCodigo(), MetodoPago.YAPE);
        assertThat(pagado.isPagado()).isTrue();
        assertThat(pagado.getBoleta()).startsWith("B001-");
        assertThat(pagado.getOperacion()).startsWith("YP");
        assertThat(pagado.getRecojoFecha()).isNotNull();
        Expediente actualizado = expedientes.obtener(e.getCodigo());
        assertThat(actualizado.getEstado()).isEqualTo("Pagado · En emisión");
        assertThat(actualizado.getAcciones()).extracting(AccionExpediente::getAccion).contains("gen-sol", "rcpt-sol", "route-sol");
        assertThat(notificaciones.noLeidas(Rol.ADMINISTRADOR)).isEqualTo(antes + 1);
        Matricula m = pagos.matricula();
        assertThat(m.isConfirmada()).isFalse();
        pagos.completar(m.getPago().getCodigo(), MetodoPago.VISA);
        assertThat(pagos.matricula().isConfirmada()).isTrue();
        assertThat(carne.vigente()).isTrue();
        assertThat(notificaciones.listar(Rol.ESTUDIANTE).get(0).getTexto()).contains("quedó confirmada");
    }

    @Test
    void desistirAnulaLaOrdenPendiente() {
        Pago p = pagos.crearOrden(catalogo.tramite("t-cest").orElseThrow(), yo(), 1, "Trámite personal", "capilla", null);
        Expediente e = expedientes.dePago(p).orElseThrow();
        expedientes.desistir(e.getCodigo());
        assertThat(pagos.obtener(p.getCodigo()).getEstado()).isEqualTo(EstadoPago.ANULADO);
    }

    @Test
    void validacionDelSolicitante() {
        Tramite egresado = catalogo.tramite("t-cegr").orElseThrow();
        assertThat(pagos.validarSolicitante(egresado, yo(), false)).containsEntry("publico", "Este trámite es para: Egresado.");
        Tramite carneT = catalogo.tramite("t-carne").orElseThrow();
        assertThat(pagos.validarSolicitante(carneT, yo(), false)).containsEntry("foto", "Sube tu foto para el carné.");
        DatosSolicitante malo = yo().toBuilder().dni("123").correo("x").build();
        assertThat(pagos.validarSolicitante(carneT, malo, true)).containsKeys("dni", "correo");
    }

    @Test
    void accionesDelAdministradorNotificanYEnvianMensajes() {
        Pago p = pagos.crearOrden(catalogo.tramite("t-cert").orElseThrow(), yo(), 1, "Trabajo", "capilla", null);
        Expediente e = expedientes.dePago(p).orElseThrow();
        assertThatThrownBy(() -> admin.emitir(e.getCodigo())).hasMessage("Esperando pago");
        pagos.completar(p.getCodigo(), MetodoPago.PLIN);
        int mensajesAntes = mensajes.findByExpedienteOrderByIdDesc(expedientes.obtener(e.getCodigo())).size();
        assertThat(mensajesAntes).isEqualTo(2);
        long antes = notificaciones.noLeidas(Rol.ESTUDIANTE);
        admin.emitir(e.getCodigo());
        assertThat(expedientes.obtener(e.getCodigo()).getGrupo()).isEqualTo(GrupoExpediente.LISTO);
        assertThat(notificaciones.noLeidas(Rol.ESTUDIANTE)).isEqualTo(antes + 1);
        seguimiento.cambiarPreferencia("wa", false);
        admin.entregar(e.getCodigo());
        List<MensajeEnviado> lista = mensajes.findByExpedienteOrderByIdDesc(expedientes.obtener(e.getCodigo()));
        assertThat(lista).hasSize(5);
        assertThat(lista.get(0).getCanal()).isEqualTo(CanalAviso.CORREO);
        assertThat(SeguimientoService.calcular(expedientes.obtener(e.getCodigo())).estimado()).isEqualTo("Entregado");
        Expediente otro = expedientes.mios().stream().filter(x -> x.getGrupo() == GrupoExpediente.TRAMITE && x.getPago() == null).findFirst().orElseThrow();
        assertThatThrownBy(() -> admin.observar(otro.getCodigo(), "corto")).hasMessage("Escribe al menos 15 caracteres.");
        admin.observar(otro.getCodigo(), "Falta adjuntar la copia del DNI legible.");
        assertThat(expedientes.obtener(otro.getCodigo()).getGrupo()).isEqualTo(GrupoExpediente.OBSERVADO);
        assertThat(notificaciones.listar(Rol.ESTUDIANTE).get(0).getTexto()).contains("fue observado");
        Reserva r = reservas.mias().stream().filter(x -> x.getEstado() == EstadoReserva.PENDIENTE).findFirst().orElseThrow();
        admin.rechazarReserva(r.getId(), "El auditorio está en mantenimiento.");
        assertThat(reservaRepository.findById(r.getId()).orElseThrow().getEstado()).isEqualTo(EstadoReserva.RECHAZADO);
        Pago pagado = pagos.mios().stream().filter(Pago::isPagado).findFirst().orElseThrow();
        admin.reembolsar(pagado.getCodigo());
        assertThat(pagos.obtener(pagado.getCodigo()).getEstado()).isEqualTo(EstadoPago.REEMBOLSADO);
    }

    @Test
    void sinConexionLaColaCreceLosPagosSeBloqueanYSeVacia() {
        conexion.simularSinConexion(true);
        assertThat(conexion.sinConexion()).isTrue();
        favoritos.alternar(TipoFavorito.EQUIPO, "e1");
        expedientes.registrar("Traslado interno", "EP Ingeniería de Software y Sistemas", "capilla", "Solicito traslado interno por motivos personales.", null);
        assertThat(conexion.contarPendientes()).isEqualTo(2);
        String m = pagos.matricula().getPago().getCodigo();
        assertThatThrownBy(() -> pagos.completar(m, MetodoPago.YAPE)).hasMessage("Necesitas conexión para pagar");
        conexion.simularSinConexion(false);
        assertThat(conexion.sincronizar()).isEqualTo(2);
        assertThat(conexion.contarPendientes()).isZero();
        favoritos.alternar(TipoFavorito.EQUIPO, "e2");
        assertThat(conexion.contarPendientes()).isZero();
    }

    @Test
    void persistenciaDeIdaYVuelta() {
        Espacio redes = catalogo.espacio("lab-redes").orElseThrow();
        LocalDate manana = Fechas.hoy().plusDays(1);
        Reserva r = reservas.reservar(redes, manana, "08:00 - 10:00", 20, "Práctica calificada de Redes");
        Reserva leida = reservaRepository.findById(r.getId()).orElseThrow();
        assertThat(leida.getEspacio().getCodigo()).isEqualTo("lab-redes");
        assertThat(leida.getFecha()).isEqualTo(manana);
        assertThat(leida.getEstado()).isEqualTo(EstadoReserva.PENDIENTE);
        assertThatThrownBy(() -> reservas.reservar(redes, manana, "08:00 - 10:00", 5, "Otra clase del curso")).hasMessage("Ese horario ya está reservado. Elige otro.");
        ajustes.cambiar(a -> a.setIdioma("qu"));
        assertThat(ajustes.obtener().getIdioma()).isEqualTo("qu");
        assertThat(contexto.cambiarEscuela("ier")).isEqualTo("EP Ingeniería en Energías Renovables · Sede Ayabacas.");
        assertThat(contexto.codigoSede()).isEqualTo("ayabacas");
        assertThat(contexto.cambiarSede("capilla")).isEqualTo("Mostrando información de la Sede Capilla · todas las escuelas.");
        assertThat(contexto.escuela()).isNull();
        demo.restablecer();
        assertThat(reservaRepository.findById(r.getId())).isEmpty();
        assertThat(ajustes.obtener().getIdioma()).isEqualTo("qu");
    }

    @Test
    void mapaLibrePorTerminarYOcupado() {
        Espacio cnc = catalogo.espacio("taller-cnc").orElseThrow();
        assertThat(ocupacion.estado(cnc, 0, 0)).isEqualTo(OcupacionService.EstadoOcupacion.OCUPADO);
        Espacio e = catalogo.espacios().stream().filter(x -> !x.isFueraDeServicio()).filter(x -> ReservaService.disponibleSimulado(x.getNombre(), 3, "08:00")
                && ReservaService.disponibleSimulado(x.getNombre(), 3, "10:00")).findFirst().orElseThrow();
        assertThat(ocupacion.estado(e, 0, 3)).isEqualTo(OcupacionService.EstadoOcupacion.LIBRE);
        reservas.reservar(e, Fechas.hoy().plusDays(3), "10:00 - 12:00", 1, "Prueba del mapa en vivo");
        assertThat(ocupacion.estado(e, 0, 3)).isEqualTo(OcupacionService.EstadoOcupacion.POR_TERMINAR);
        assertThat(ocupacion.estado(e, 1, 3)).isEqualTo(OcupacionService.EstadoOcupacion.OCUPADO);
    }

    @Test
    void carneAceptaFranjaActualYVecinasYRechazaVencidoOAlterado() {
        long f = CarneService.franja(Fechas.milis());
        String token = CarneService.token("2024207009", f);
        assertThat(carne.verificar(token, f).ok()).isTrue();
        assertThat(carne.verificar(token, f + 1).ok()).isTrue();
        assertThat(carne.verificar(token, f - 1).ok()).isTrue();
        assertThat(carne.verificar(token, f + 2).mensaje()).isEqualTo("Código vencido. Pide al estudiante que muestre el código actual.");
        String alterado = "ANDARA-ID|2024207009|" + f + "|ZZZZZZ";
        assertThat(carne.verificar(alterado, f).mensaje()).isEqualTo("Firma inválida: el código fue alterado.");
        assertThat(carne.verificar("72458913", f).mensaje()).isEqualTo("Estudiante encontrado (validación manual por código)");
        assertThat(carne.verificar("000", f).ok()).isFalse();
        assertThat(carne.simularLectura().isConcedido()).isFalse();
    }

    @Test
    void analisisPorPeriodo() {
        contexto.cambiarSede(Ajustes.TODAS);
        AnaliticaService.Analisis sem = analitica.analizar("sem");
        AnaliticaService.Analisis siete = analitica.analizar("7");
        assertThat(sem.tramitesYOrdenes()).isGreaterThanOrEqualTo(siete.tramitesYOrdenes());
        assertThat(sem.recaudacion()).isGreaterThanOrEqualTo(siete.recaudacion());
        assertThat(sem.top()).hasSizeLessThanOrEqualTo(6);
        for (int i = 1; i < sem.top().size(); i++) {
            assertThat(sem.top().get(i - 1).valor()).isGreaterThanOrEqualTo(sem.top().get(i).valor());
        }
        assertThat(sem.hallazgos()).hasSize(4);
        assertThat(sem.calor()[sem.pico()[0]][sem.pico()[1]]).isEqualTo(sem.calorMaximo());
        assertThat(sem.subutilizados()).hasSize(4);
        assertThat(sem.demandados()).hasSize(3);
        assertThat(sem.porSede()).hasSize(3);
    }
}
