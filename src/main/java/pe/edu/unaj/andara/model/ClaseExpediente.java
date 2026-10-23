package pe.edu.unaj.andara.model;

public enum ClaseExpediente {
    DOC,
    OBS,
    LAB,
    EQ;

    public boolean esReservaOPrestamo() {
        return this == LAB || this == EQ;
    }
}
