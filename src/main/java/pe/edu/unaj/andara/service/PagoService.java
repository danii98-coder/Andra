package pe.edu.unaj.andara.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.unaj.andara.exception.NegocioException;
import pe.edu.unaj.andara.model.*;
import pe.edu.unaj.andara.repository.ExpedienteRepository;
import pe.edu.unaj.andara.repository.MatriculaRepository;
import pe.edu.unaj.andara.repository.PagoRepository;
import pe.edu.unaj.andara.util.DiasHabiles;
import pe.edu.unaj.andara.util.Fechas;
import pe.edu.unaj.andara.util.Montos;
import pe.edu.unaj.andara.util.TarjetaJemm;
import pe.edu.unaj.andara.util.Validacion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.function.Predicate;

@Service
@RequiredArgsConstructor
public class PagoService {

    public record Recojo(LocalDate fecha, String texto) {
    }

    public static final List<String[]> PRESETS = List.of(new String[]{"mat", "Matrícula"},
            new String[]{"doc", "Documentos y carné"}, new String[]{"week", "Vence esta semana"},
            new String[]{"gt50", "Más de S/ 50"});
    public static final List<String> USOS = List.of("Trámite personal", "Prácticas preprofesionales", "Beca o subvención",
            "Trabajo", "Convalidación o traslado", "Otro");

    private static final Random AZAR_JEMM = new Random();

    private final PagoRepository pagos;
    private final ExpedienteRepository expedientes;
    private final MatriculaRepository matriculas;
    private final SesionService sesion;
    private final CatalogoService catalogo;
    private final NotificacionService notificaciones;
    private final SeguimientoService seguimiento;
    private final ConexionService conexion;

    public static Recojo recojo(Tramite t, LocalDateTime pagado) {
        LocalDate dia = pagado.toLocalDate();
        int hora = pagado.getHour();
        if (t == null || "inmediato".equals(t.getEntrega())) {
            return new Recojo(null, "Habilitado al instante. No requiere recojo.");
        }
        if ("mismo".equals(t.getEntrega())) {
            boolean mismoDia = hora < 15 && DiasHabiles.esHabil(dia);
            LocalDate f = mismoDia ? dia : DiasHabiles.sumar(dia, 1);
            return new Recojo(f, mismoDia ? "Hoy, " + Fechas.largo(f) + ", desde las 16:00" : Fechas.largo(f) + ", desde las 09:00");
        }
        int n = "carne".equals(t.getEntrega()) ? 10 : Integer.parseInt(t.getEntrega());
        LocalDate f = DiasHabiles.sumar(dia, n);
        return new Recojo(f, Fechas.largo(f) + " (" + n + " días hábiles)");
    }

    public static BigDecimal total(Collection<Pago> lista) {
        return lista.stream().map(Pago::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public static BigDecimal totalOrden(List<ItemPago> items) {
        return items.stream().map(ItemPago::getImporte).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Transactional(readOnly = true)
    public List<Pago> mios() {
        return pagos.findByPersonaOrderByCreadoDesc(sesion.estudiante());
    }

    @Transactional(readOnly = true)
    public List<Pago> todos() {
        return pagos.findAllByOrderByCreadoDesc();
    }

    @Transactional(readOnly = true)
    public Pago obtener(String codigo) {
        return pagos.findByCodigo(codigo).orElseThrow(() -> new NegocioException("La orden no existe."));
    }

    @Transactional(readOnly = true)
    public List<Pago> pendientes() {
        return mios().stream().filter(Pago::isPendiente).toList();
    }

    @Transactional(readOnly = true)
    public Optional<Pago> masProximo() {
        return pendientes().stream().filter(p -> p.getVence() != null).min(Comparator.comparing(Pago::getVence));
    }

    @Transactional(readOnly = true)
    public BigDecimal pagadoEnAnio(int anio) {
        return total(mios().stream().filter(p -> p.isPagado() && p.getPagado() != null && p.getPagado().getYear() == anio).toList());
    }

    @Transactional(readOnly = true)
    public Matricula matricula() {
        return matriculas.findFirstByOrderByIdDesc().orElseThrow(() -> new NegocioException("No hay matrícula."));
    }

    public Map<String, Predicate<Pago>> predicados() {
        LocalDate semana = Fechas.hoy().plusDays(7);
        return Map.of("mat", p -> p.getTipo() == TipoPago.MATRICULA,
                "doc", p -> p.getTipo() == TipoPago.DOCUMENTO || p.getTipo() == TipoPago.CARNE,
                "week", p -> p.isPendiente() && p.getVence() != null && !p.getVence().isAfter(semana),
                "gt50", p -> p.getTotal().compareTo(BigDecimal.valueOf(50)) > 0);
    }

    public static String estadoVisible(Pago p) {
        if (p.getEstado() == EstadoPago.PENDIENTE && p.getVence() != null && p.getVence().isBefore(Fechas.hoy())) {
            return "Vencido";
        }
        return p.getEstado().getEtiqueta();
    }

    public static String tonoEstado(String estado) {
        return switch (estado) {
            case "Pagado" -> "success";
            case "Vencido" -> "error";
            case "Por pagar" -> "warning";
            default -> "neutral";
        };
    }

    public Map<String, String> validarSolicitante(Tramite t, DatosSolicitante s, boolean tieneFoto) {
        Map<String, String> e = new LinkedHashMap<>();
        if (blanco(s.getNombre())) {
            e.put("nombre", "Escribe tus nombres y apellidos.");
        }
        if (blanco(s.getDni())) {
            e.put("dni", "Escribe tu DNI.");
        } else if (!Validacion.dni(s.getDni())) {
            e.put("dni", "El DNI debe tener 8 dígitos.");
        }
        boolean pideCodigo = List.of("Estudiante", "Egresado").contains(s.getTipo());
        if (pideCodigo && blanco(s.getCodigo())) {
            e.put("codigo", "Escribe tu código universitario.");
        }
        if (List.of("Estudiante", "Egresado", "Docente").contains(s.getTipo()) && blanco(s.getEscuela())) {
            e.put("escuela", "Elige tu escuela.");
        }
        if (blanco(s.getCorreo())) {
            e.put("correo", "Escribe tu correo.");
        } else if (!Validacion.correo(s.getCorreo())) {
            e.put("correo", "Escribe un correo válido.");
        }
        if (e.isEmpty() && !t.admite(s.getTipo())) {
            e.put("publico", "Este trámite es para: " + String.join(", ", t.getListaPublicos()) + ".");
        }
        if (e.isEmpty() && t.isCarne() && !tieneFoto) {
            e.put("foto", "Sube tu foto para el carné.");
        }
        return e;
    }

    public static Map<String, String> validarYape(String celular, String codigo) {
        Map<String, String> e = new LinkedHashMap<>();
        if (blanco(celular)) {
            e.put("celular", "Escribe tu celular.");
        } else if (!Validacion.celular(celular)) {
            e.put("celular", "Celular de 9 dígitos que empiece con 9.");
        }
        if (blanco(codigo)) {
            e.put("codigo", "Escribe el código de 6 dígitos.");
        } else if (!Validacion.codigoAprobacion(codigo)) {
            e.put("codigo", "El código tiene 6 dígitos.");
        }
        return e;
    }

    public static Map<String, String> validarPlin(String celular) {
        Map<String, String> e = new LinkedHashMap<>();
        if (blanco(celular)) {
            e.put("celular", "Escribe tu celular.");
        } else if (!Validacion.celular(celular)) {
            e.put("celular", "Celular de 9 dígitos que empiece con 9.");
        }
        return e;
    }

    public static Map<String, String> validarTarjeta(String numero, String titular, String vencimiento, String cvv,
                                                     String correo, YearMonth mesActual) {
        Map<String, String> e = new LinkedHashMap<>();
        if (blanco(numero)) {
            e.put("numero", "Escribe el número de tu tarjeta.");
        }
        if (blanco(titular)) {
            e.put("titular", "Escribe el nombre como aparece en la tarjeta.");
        }
        if (blanco(vencimiento)) {
            e.put("vencimiento", "Escribe el vencimiento.");
        }
        if (blanco(cvv)) {
            e.put("cvv", "Escribe el CVV.");
        }
        if (blanco(correo)) {
            e.put("correo", "Escribe un correo.");
        }
        if (!e.isEmpty()) {
            return e;
        }
        if (TarjetaJemm.marca(numero) == null) {
            e.put("numero", "Usa una tarjeta Visa, Mastercard, American Express o Diners Club.");
        } else if (!TarjetaJemm.luhn(numero)) {
            e.put("numero", "El número de tarjeta no es válido. Revísalo.");
        } else if (!TarjetaJemm.vencimientoValido(vencimiento, mesActual)) {
            e.put("vencimiento", "La fecha de vencimiento no es válida.");
        } else if (!Validacion.cvv(cvv)) {
            e.put("cvv", "El CVV tiene 3 o 4 dígitos.");
        }
        return e;
    }

    private static boolean blanco(String s) {
        return s == null || s.isBlank();
    }

    public static List<ItemPago> itemsPara(Tramite t, int cantidad) {
        int q = t.isPorCantidad() ? Math.max(1, Math.min(20, cantidad)) : 1;
        String concepto = t.getNombre() + (t.isPorCantidad() && q > 1 ? " (" + q + " cursos)" : "");
        return new ArrayList<>(List.of(new ItemPago(concepto, q, t.getMonto())));
    }

    @Transactional(readOnly = true)
    public String siguienteCodigoOrden() {
        int n = 5600 + (int) pagos.count();
        String c;
        do {
            c = "PAG-2026-00" + n++;
        } while (pagos.findByCodigo(c).isPresent());
        return c;
    }

    @Transactional
    public Pago crearOrden(Tramite t, DatosSolicitante sol, int cantidad, String motivo, String sedeCodigo, byte[] foto) {
        Sede sede = catalogo.sede(sedeCodigo).orElseThrow();
        List<ItemPago> items = itemsPara(t, cantidad);
        String ordenJemm = siguienteCodigoOrden();
        TipoPago tipo = t.isCarne() ? TipoPago.CARNE : t.generaDocumento() ? TipoPago.DOCUMENTO : TipoPago.TRAMITE;
        Pago p = pagos.save(Pago.builder().codigo(ordenJemm).persona(sesion.estudiante()).tipo(tipo).tramite(t)
                .concepto(t.getNombre()).estado(EstadoPago.PENDIENTE).creado(Fechas.ahora()).vence(Fechas.hoy().plusDays(3))
                .sede(sede).solicitante(sol).motivo(motivo).foto(foto).items(items).build());
        String codigoExp = siguienteCodigoExpediente("SOL-2026-0", 85400);
        Expediente e = Expediente.builder().codigo(codigoExp).persona(sesion.estudiante()).sede(sede).clase(ClaseExpediente.DOC)
                .tramite(t).pago(p).grupo(GrupoExpediente.TRAMITE).icono(t.isCarne() ? "idcard" : "doc").rotuloIcono("PAGO")
                .tono("warning").estado("Pago pendiente en Andara Pay").titulo(t.getNombre())
                .descripcion("Documento a nombre de " + sol.getNombre() + " (" + sol.getTipo() + "). Uso: " + motivo + ".")
                .paso(1).solicitante(sol).fecha(Fechas.ahora()).build();
        e.getDatos().add(new DatoExpediente("Fecha de registro", "Hoy, " + Fechas.dmy(Fechas.hoy())));
        e.getDatos().add(new DatoExpediente("Derecho / Tasa", Montos.soles(p.getTotal()) + " (Pendiente)"));
        e.getDatos().add(new DatoExpediente("Recojo", "Mesa de Partes · " + sede.getNombreCompleto()));
        e.ponerAcciones("primary", "pay-sol", "Pagar en Andara Pay", "secondary", "route-sol", "Seguimiento en vivo",
                "danger", "desist-sol", "Anular trámite");
        expedientes.save(e);
        notificaciones.notificarAdministrador("Nueva orden " + ordenJemm + ": " + t.getNombre() + " (" + sol.getNombre() + ").", "a-tramites");
        conexion.encolar("Solicitar " + t.getNombre().toLowerCase());
        return p;
    }

    @Transactional(readOnly = true)
    public String siguienteCodigoExpediente(String prefijo, int base) {
        int n = base + (int) expedientes.count();
        String c;
        do {
            c = prefijo + n++;
        } while (expedientes.existsByCodigo(c));
        return c;
    }

    public static String nuevaOperacion(MetodoPago m) {
        return m.getPrefijo() + (10000000 + AZAR_JEMM.nextInt(89999999));
    }

    @Transactional
    public Pago completar(String codigo, MetodoPago metodo) {
        if (conexion.sinConexion()) {
            throw new NegocioException("Necesitas conexión para pagar");
        }
        Pago p = obtener(codigo);
        if (p.isPagado()) {
            return p;
        }
        if (!p.isPendiente()) {
            throw new NegocioException("Esta orden ya no se puede pagar.");
        }
        Tramite t = p.getTramite();
        p.setEstado(EstadoPago.PAGADO);
        p.setMetodo(metodo);
        p.setPagado(Fechas.ahora());
        p.setOperacion(nuevaOperacion(metodo));
        p.setBoleta("B001-00" + (4000 + pagos.countByBoletaIsNotNull()));
        Recojo r = recojo(t, p.getPagado());
        p.setRecojoFecha(r.fecha());
        p.setRecojoTexto(r.texto());
        pagos.save(p);
        String monto = Montos.soles(p.getTotal());
        expedientes.findFirstByPago(p).ifPresent(sol -> {
            boolean listo = t != null && "mismo".equals(t.getEntrega());
            boolean inmediato = t != null && "inmediato".equals(t.getEntrega());
            sol.setTono("success");
            sol.setRotuloIcono("PAGADO");
            sol.ponerDato(1, "Derecho / Tasa", monto + " (Pagado con " + metodo.getEtiqueta() + ")");
            sol.ponerDato(2, "Recojo", r.fecha() != null ? Fechas.dmy(r.fecha()) + " · " + p.getSede().getNombre() : "No requiere");
            sol.setGrupo(listo || inmediato ? GrupoExpediente.LISTO : GrupoExpediente.TRAMITE);
            sol.setEstado(inmediato ? "Pagado · Habilitado" : listo ? "Pagado · Listo para recoger" : "Pagado · En emisión");
            sol.setPaso(listo || inmediato ? 4 : 2);
            sol.getEtiquetas().clear();
            sol.getEtiquetas().add("Comprobante " + p.getBoleta());
            sol.getEtiquetas().add("Operación " + p.getOperacion());
            if (t != null && t.generaDocumento()) {
                sol.ponerAcciones("primary", "gen-sol", "Ver documento generado", "secondary", "rcpt-sol", "Ver comprobante",
                        "tertiary", "route-sol", "Seguimiento en vivo");
            } else {
                sol.ponerAcciones("primary", "rcpt-sol", "Ver comprobante", "secondary", "route-sol", "Seguimiento en vivo");
            }
            expedientes.save(sol);
            seguimiento.registrar(sol, "Andara UNAJ: recibimos tu pago de " + monto + " por " + p.getConcepto() + ". "
                    + (r.fecha() != null ? "Recojo: " + r.texto() + "." : ""));
        });
        Matricula m = matricula();
        if (p.getTipo() == TipoPago.MATRICULA && m.getPago().getId().equals(p.getId())) {
            m.setEstado(EstadoMatricula.CONFIRMADA);
            matriculas.save(m);
            notificaciones.notificarEstudiante("Tu matrícula " + m.getPeriodo() + " quedó confirmada. Pago " + p.getBoleta() + ".", "pay");
        } else {
            notificaciones.notificarEstudiante("Pago confirmado: " + p.getConcepto() + " (" + monto + ")."
                    + (r.fecha() != null ? " Recojo: " + Fechas.dmy(r.fecha()) + "." : ""), "pay");
        }
        notificaciones.notificarAdministrador("Pago confirmado por " + metodo.getEtiqueta() + ": " + p.getConcepto() + " · " + monto + ".", "a-pagos");
        return p;
    }

    @Transactional
    public void anular(Pago p) {
        if (p != null && p.isPendiente()) {
            Pago actual = obtener(p.getCodigo());
            actual.setEstado(EstadoPago.ANULADO);
            pagos.save(actual);
        }
    }
}
