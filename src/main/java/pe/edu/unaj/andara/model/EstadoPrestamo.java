package pe.edu.unaj.andara.model;

import java.util.Arrays;

public enum EstadoPrestamo {
    EN_REVISION("En revisión"),
    APROBADO("Aprobado"),
    DEVUELTO("Devuelto"),
    RECHAZADO("Rechazado"),
    CANCELADO("Cancelado");

    private final String etiqueta;

    EstadoPrestamo(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public boolean isEnCurso() {
        return this == EN_REVISION || this == APROBADO;
    }

    public static EstadoPrestamo desdeEtiqueta(String texto) {
        return Arrays.stream(values()).filter(e -> e.etiqueta.equals(texto)).findFirst().orElse(EN_REVISION);
    }
}
