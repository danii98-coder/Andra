package pe.edu.unaj.andara.model;

import java.util.Arrays;

public enum GrupoExpediente {
    TRAMITE("tramite", "En trámite", "warning"),
    LISTO("listo", "Emitidos / Listos", "success"),
    OBSERVADO("observado", "Observados", "error"),
    ANULADO("anulado", "Anulados", "neutral");

    private final String clave;
    private final String etiqueta;
    private final String tono;

    GrupoExpediente(String clave, String etiqueta, String tono) {
        this.clave = clave;
        this.etiqueta = etiqueta;
        this.tono = tono;
    }

    public String getClave() {
        return clave;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public String getTono() {
        return tono;
    }

    public static GrupoExpediente desdeClave(String clave) {
        return Arrays.stream(values()).filter(g -> g.clave.equals(clave)).findFirst().orElse(TRAMITE);
    }
}
