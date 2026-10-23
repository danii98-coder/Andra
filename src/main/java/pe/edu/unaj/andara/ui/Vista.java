package pe.edu.unaj.andara.ui;

public interface Vista {
    void refrescar();

    default void salir() {
    }

    default void enfocarBusqueda() {
    }
}
