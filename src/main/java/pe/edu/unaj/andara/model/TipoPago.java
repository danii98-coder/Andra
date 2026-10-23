package pe.edu.unaj.andara.model;

public enum TipoPago {
    MATRICULA("Matrícula", "fth-award"),
    DOCUMENTO("Documento", "fth-file-text"),
    CARNE("Carné", "fth-credit-card"),
    TRAMITE("Trámite", "fth-file");

    private final String etiqueta;
    private final String icono;

    TipoPago(String etiqueta, String icono) {
        this.etiqueta = etiqueta;
        this.icono = icono;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public String getIcono() {
        return icono;
    }

    public static TipoPago desdeClave(String clave) {
        return valueOf(clave.toUpperCase());
    }
}
