package pe.edu.unaj.andara.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.unaj.andara.model.*;
import pe.edu.unaj.andara.repository.MensajeEnviadoRepository;
import pe.edu.unaj.andara.util.Fechas;
import pe.edu.unaj.andara.util.Hash;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SeguimientoService {

    public record Etapa(String icono, String titulo) {
    }

    public record SeguimientoCjsv(List<Etapa> etapas, int actual, List<LocalDateTime> tiempos, String estimado,
                                  int ventanilla, boolean observado) {
    }

    public record Preferencias(boolean whatsapp, boolean correo, boolean app) {
    }

    private final MensajeEnviadoRepository mensajes;
    private final AjustesService ajustes;

    public static int ventanilla(String codigo) {
        return 1 + (int) (Hash.numero(codigo) % 6);
    }

    public static LocalDate fechaDe(Expediente s) {
        if (s.getFecha() != null) {
            return s.getFecha().toLocalDate();
        }
        return s.getDatos().isEmpty() ? null : Fechas.desdeTexto(s.getDatos().get(0).getValor());
    }

    public static SeguimientoCjsv calcular(Expediente s) {
        Pago p = s.getPago();
        LocalDate dia = fechaDe(s);
        LocalDateTime base = s.getFecha() != null ? s.getFecha() : (dia != null ? dia : Fechas.hoy()).atTime(9, 0);
        int ven = ventanilla(s.getCodigo());
        boolean entregado = s.isEntregado();
        GrupoExpediente g = s.getGrupo();
        List<Etapa> etapas = new ArrayList<>();
        int actual;
        if (s.getClase().esReservaOPrestamo()) {
            boolean lab = s.getClase() == ClaseExpediente.LAB;
            etapas.add(new Etapa("fth-inbox", "Solicitado"));
            etapas.add(new Etapa("fth-check", "Aprobado"));
            etapas.add(new Etapa(lab ? "fth-key" : "fth-box", lab ? "Listo para usar" : "Listo para retirar"));
            etapas.add(new Etapa("fth-flag", "Finalizado"));
            actual = g == GrupoExpediente.ANULADO ? 0 : g == GrupoExpediente.LISTO ? 2 : 1;
        } else if (p != null) {
            etapas.add(new Etapa("fth-inbox", "Recibido"));
            etapas.add(new Etapa("fth-credit-card", "Pago confirmado"));
            etapas.add(new Etapa("fth-pen-tool", "En firma"));
            etapas.add(new Etapa("fth-printer", "Impreso"));
            etapas.add(new Etapa("fth-map-pin", "Listo en ventanilla " + ven));
            etapas.add(new Etapa("fth-flag", "Entregado"));
            actual = !p.isPagado() ? 1 : entregado ? 6 : g == GrupoExpediente.LISTO ? 4 : 2;
        } else {
            etapas.add(new Etapa("fth-inbox", "Recibido"));
            etapas.add(new Etapa("fth-eye", "En revisión"));
            etapas.add(new Etapa("fth-pen-tool", "En firma"));
            etapas.add(new Etapa("fth-map-pin", "Listo en ventanilla " + ven));
            etapas.add(new Etapa("fth-flag", "Entregado"));
            actual = entregado ? 5 : g == GrupoExpediente.LISTO ? 3 : Math.min(2, Math.max(1, s.getPaso()));
        }
        List<LocalDateTime> tiempos = new ArrayList<>();
        for (int i = 0; i < etapas.size(); i++) {
            if (i == 0) {
                tiempos.add(base);
            } else if (i == 1 && p != null && p.getPagado() != null) {
                tiempos.add(p.getPagado());
            } else {
                LocalDateTime desde = p != null && p.getPagado() != null ? p.getPagado() : base;
                tiempos.add(desde.plusMinutes(i * 45L));
            }
        }
        String estimado;
        if (g == GrupoExpediente.OBSERVADO) {
            estimado = "Detenido: tienes una observación por subsanar";
        } else if (g == GrupoExpediente.ANULADO) {
            estimado = "Trámite anulado";
        } else if (actual >= etapas.size()) {
            estimado = "Entregado";
        } else if (p != null && !p.isPagado()) {
            estimado = "Esperando tu pago en Andara Pay";
        } else if (p != null && p.getRecojoFecha() != null) {
            estimado = "Listo para recoger: " + p.getRecojoTexto();
        } else if (g == GrupoExpediente.LISTO) {
            estimado = "Listo · Ventanilla " + ven + ", Mesa de Partes · Sede " + s.getSede().getNombre();
        } else {
            estimado = "Estimado: 2 a 3 días hábiles";
        }
        return new SeguimientoCjsv(etapas, actual, tiempos, estimado, ven, g == GrupoExpediente.OBSERVADO);
    }

    public Preferencias preferencias() {
        Ajustes a = ajustes.obtener();
        return new Preferencias(a.isAvisoWhatsapp(), a.isAvisoCorreo(), a.isAvisoApp());
    }

    public void cambiarPreferencia(String canal, boolean valor) {
        ajustes.cambiar(a -> {
            switch (canal) {
                case "wa" -> a.setAvisoWhatsapp(valor);
                case "mail" -> a.setAvisoCorreo(valor);
                default -> a.setAvisoApp(valor);
            }
        });
    }

    @Transactional
    public int registrar(Expediente expediente, String texto) {
        if (!expediente.getPersona().isPrincipal()) {
            return 0;
        }
        Preferencias pref = preferencias();
        LocalDateTime ahora = Fechas.ahora();
        int n = 0;
        if (pref.whatsapp()) {
            mensajes.save(MensajeEnviado.builder().expediente(expediente).canal(CanalAviso.WHATSAPP).texto(texto.trim()).enviado(ahora).build());
            n++;
        }
        if (pref.correo()) {
            mensajes.save(MensajeEnviado.builder().expediente(expediente).canal(CanalAviso.CORREO).texto(texto.trim()).enviado(ahora).build());
            n++;
        }
        return n;
    }

    @Transactional(readOnly = true)
    public List<MensajeEnviado> mensajes(Expediente expediente) {
        return mensajes.findByExpedienteOrderByIdDesc(expediente).stream().limit(4).toList();
    }
}
