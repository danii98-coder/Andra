package pe.edu.unaj.andara.model;

import java.util.Arrays;

public enum TipoEspacio {
    AULA("Aula", "Aulas"),
    AUDITORIO("Auditorio", "Auditorios"),
    LABORATORIO("Laboratorio", "Laboratorios"),
    SALA("Sala", "Salas"),
    TALLER("Taller", "Talleres");

    private final String etiqueta;
    private final String plural;

    TipoEspacio(String etiqueta, String plural) {
        this.etiqueta = etiqueta;
        this.plural = plural;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public String getPlural() {
        return plural;
    }

    public static TipoEspacio desdeEtiqueta(String texto) {
        return Arrays.stream(values()).filter(t -> t.etiqueta.equalsIgnoreCase(texto)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException(texto));
    }
}
