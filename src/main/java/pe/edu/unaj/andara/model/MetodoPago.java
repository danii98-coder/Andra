package pe.edu.unaj.andara.model;

public enum MetodoPago {
    YAPE("Yape", "YP", false),
    PLIN("Plin", "PL", false),
    VISA("Visa", "TC", true),
    MASTERCARD("Mastercard", "TC", true),
    AMEX("American Express", "TC", true),
    DINERS("Diners Club", "TC", true);

    private final String etiqueta;
    private final String prefijo;
    private final boolean tarjeta;

    MetodoPago(String etiqueta, String prefijo, boolean tarjeta) {
        this.etiqueta = etiqueta;
        this.prefijo = prefijo;
        this.tarjeta = tarjeta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public String getPrefijo() {
        return prefijo;
    }

    public boolean isTarjeta() {
        return tarjeta;
    }

    public String getGrupo() {
        return tarjeta ? "Tarjeta" : etiqueta;
    }

    public static MetodoPago desdeClave(String clave) {
        return clave == null ? null : valueOf(clave.toUpperCase());
    }
}
