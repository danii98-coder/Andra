package pe.edu.unaj.andara.model;

public enum EstadoPago {
    PENDIENTE("Por pagar"),
    PAGADO("Pagado"),
    ANULADO("Anulado"),
    REEMBOLSADO("Reembolsado");

    private final String etiqueta;

    EstadoPago(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
