package pe.edu.unaj.andara.model;

import java.util.Arrays;

public enum EstadoReserva {
    APROBADO("Aprobado"),
    PENDIENTE("Pendiente"),
    RECHAZADO("Rechazado"),
    VENCIO("Venció"),
    CANCELADO("Cancelado");

    private final String etiqueta;

    EstadoReserva(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public boolean isActiva() {
        return this == APROBADO || this == PENDIENTE;
    }

    public static EstadoReserva desdeEtiqueta(String texto) {
        return Arrays.stream(values()).filter(e -> e.etiqueta.equals(texto)).findFirst().orElse(PENDIENTE);
    }
}
