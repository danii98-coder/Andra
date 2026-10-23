package pe.edu.unaj.andara.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.unaj.andara.model.Pago;
import pe.edu.unaj.andara.model.Persona;
import pe.edu.unaj.andara.model.UsoCarne;
import pe.edu.unaj.andara.repository.UsoCarneRepository;
import pe.edu.unaj.andara.util.Fechas;
import pe.edu.unaj.andara.util.Hash;

import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class CarneService {

    public static final long PASO_MS = 15000;
    public static final List<String> LUGARES = List.of("Biblioteca Central · Sede Capilla",
            "Préstamo de equipos · Pabellón Central", "Laboratorio Episs A-450", "Auditorio Magno");

    private static final String SEMILLA_CJSV = "andara";
    private static final Pattern TOKEN = Pattern.compile("^ANDARA-ID\\|(\\w+)\\|(\\d+)\\|(\\w+)$");
    private static final Random AZAR = new Random();

    public record Verificacion(boolean ok, String mensaje, Persona persona) {
    }

    private final SesionService sesion;
    private final PagoService pagos;
    private final CatalogoService catalogo;
    private final UsoCarneRepository usos;

    public static long franja(long milis) {
        return Math.floorDiv(milis, PASO_MS);
    }

    public static String firma(String codigo, long franja) {
        byte[] h = Hash.sha256(codigo + "|" + franja + "|" + SEMILLA_CJSV);
        long v = 0;
        for (int i = 0; i < 8; i++) {
            v = (v << 8) | (h[i] & 0xFF);
        }
        String b36 = Long.toUnsignedString(v, 36).toUpperCase();
        while (b36.length() < 6) {
            b36 = "0" + b36;
        }
        return b36.substring(0, 6);
    }

    public static String token(String codigo, long franja) {
        return "ANDARA-ID|" + codigo + "|" + franja + "|" + firma(codigo, franja);
    }

    public String tokenActual() {
        return token(sesion.estudiante().getCodigo(), franja(Fechas.milis()));
    }

    public static long segundosRestantes(long milis) {
        long resta = PASO_MS - Math.floorMod(milis, PASO_MS);
        return (long) Math.ceil(resta / 1000.0);
    }

    public static double fraccionRestante(long milis) {
        return (PASO_MS - Math.floorMod(milis, PASO_MS)) / (double) PASO_MS;
    }

    public Verificacion verificar(String entrada) {
        return verificar(entrada, franja(Fechas.milis()));
    }

    @Transactional(readOnly = true)
    public Verificacion verificar(String entrada, long franjaActual) {
        String v = entrada == null ? "" : entrada.trim();
        Matcher m = TOKEN.matcher(v);
        if (m.matches()) {
            long sl = Long.parseLong(m.group(2));
            if (Math.abs(franjaActual - sl) > 1) {
                return new Verificacion(false, "Código vencido. Pide al estudiante que muestre el código actual.", null);
            }
            if (!firma(m.group(1), sl).equals(m.group(3))) {
                return new Verificacion(false, "Firma inválida: el código fue alterado.", null);
            }
            return new Verificacion(true, "Carné válido", catalogo.persona(m.group(1)).orElse(null));
        }
        Optional<Persona> p = catalogo.personaPorCodigoODni(v);
        return p.map(x -> new Verificacion(true, "Estudiante encontrado (validación manual por código)", x))
                .orElseGet(() -> new Verificacion(false, "No encontramos ese código o DNI.", null));
    }

    public boolean vigente() {
        return pagos.matricula().isConfirmada();
    }

    @Transactional
    public UsoCarne simularLectura() {
        String lugar = LUGARES.get(AZAR.nextInt(LUGARES.size()));
        boolean ok = verificar(tokenActual()).ok() && vigente();
        return usos.save(UsoCarne.builder().lugar(lugar).fecha(Fechas.ahora()).concedido(ok).build());
    }

    @Transactional(readOnly = true)
    public List<UsoCarne> ultimos() {
        return usos.findAllByOrderByFechaDesc().stream().limit(6).toList();
    }

    @Transactional(readOnly = true)
    public Optional<byte[]> foto() {
        return pagos.mios().stream().map(Pago::getFoto).filter(f -> f != null && f.length > 0).findFirst();
    }
}
