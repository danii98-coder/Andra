package pe.edu.unaj.andara.service;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class EstadoFiltro {

    private final Set<String> activos = new LinkedHashSet<>();
    private final Map<String, String> avanzados = new LinkedHashMap<>();
    private String texto = "";

    public Set<String> getActivos() {
        return activos;
    }

    public Map<String, String> getAvanzados() {
        return avanzados;
    }

    public String getTexto() {
        return texto;
    }

    public void setTexto(String texto) {
        this.texto = texto == null ? "" : texto.trim();
    }

    public void alternar(String clave) {
        if (!activos.remove(clave)) {
            activos.add(clave);
        }
    }

    public String avanzado(String clave) {
        String v = avanzados.get(clave);
        return v == null || v.isBlank() ? null : v;
    }

    public int cantidad() {
        return activos.size() + (texto.isBlank() ? 0 : 1)
                + (int) avanzados.values().stream().filter(v -> v != null && !v.isBlank()).count();
    }

    public void limpiar() {
        activos.clear();
        avanzados.clear();
        texto = "";
    }
}
