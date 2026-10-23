package pe.edu.unaj.andara.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pe.edu.unaj.andara.model.Espacio;
import pe.edu.unaj.andara.util.DiasHabiles;
import pe.edu.unaj.andara.util.Fechas;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
public class OcupacionService {

    public enum EstadoOcupacion {
        LIBRE("Libre", "free"),
        POR_TERMINAR("Libre hasta", "soon"),
        OCUPADO("Ocupado", "busy");

        private final String etiqueta;
        private final String clase;

        EstadoOcupacion(String etiqueta, String clase) {
            this.etiqueta = etiqueta;
            this.clase = clase;
        }

        public String getEtiqueta() {
            return etiqueta;
        }

        public String getClase() {
            return clase;
        }
    }

    public record FranjaBnps(int indice, int dia, boolean enVivo) {
        public String inicio() {
            return CatalogoService.FRANJAS.get(indice);
        }

        public String fin() {
            return String.format("%02d:00", 8 + indice * 2 + 2);
        }
    }

    private final ReservaService reservas;

    public static FranjaBnps franjaPara(LocalDateTime momento) {
        double h = momento.getHour() + momento.getMinute() / 60.0;
        if (h >= 8 && h < 18 && DiasHabiles.esHabil(momento.toLocalDate())) {
            return new FranjaBnps((int) Math.floor((h - 8) / 2), 0, true);
        }
        return new FranjaBnps(0, h >= 18 ? 1 : 0, false);
    }

    public FranjaBnps franjaActual() {
        return franjaPara(LocalDateTime.now(Fechas.reloj()));
    }

    public EstadoOcupacion estado(Espacio espacio, int indice, int dia) {
        if (ocupado(espacio, indice, dia)) {
            return EstadoOcupacion.OCUPADO;
        }
        if (indice < CatalogoService.FRANJAS.size() - 1 && ocupado(espacio, indice + 1, dia)) {
            return EstadoOcupacion.POR_TERMINAR;
        }
        return EstadoOcupacion.LIBRE;
    }

    private boolean ocupado(Espacio espacio, int indice, int dia) {
        String franja = CatalogoService.FRANJAS.get(indice);
        return espacio.isFueraDeServicio() || !ReservaService.disponibleSimulado(espacio.getNombre(), dia, franja)
                || reservas.ocupadoPorReserva(espacio, Fechas.hoy().plusDays(dia), franja);
    }

    public Map<String, List<Espacio>> porPabellon(List<Espacio> espacios) {
        Map<String, List<Espacio>> grupos = new LinkedHashMap<>();
        for (Espacio e : espacios) {
            grupos.computeIfAbsent(e.getPabellon(), k -> new ArrayList<>()).add(e);
        }
        return grupos;
    }

    public long libres(List<Espacio> espacios, FranjaBnps franja) {
        return espacios.stream().filter(e -> estado(e, franja.indice(), franja.dia()) == EstadoOcupacion.LIBRE).count();
    }

    public static String nombreCorto(String nombre) {
        return nombre.replaceFirst("^(Laboratorio|Auditorio|Taller|Sala|Aula) (de |del )?", "");
    }
}
