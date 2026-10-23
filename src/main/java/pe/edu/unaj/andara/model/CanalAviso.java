package pe.edu.unaj.andara.model;

public enum CanalAviso {
    WHATSAPP("WhatsApp"),
    CORREO("Correo");

    private final String etiqueta;

    CanalAviso(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
