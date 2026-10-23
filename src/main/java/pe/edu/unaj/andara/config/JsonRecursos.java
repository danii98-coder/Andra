package pe.edu.unaj.andara.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Objects;

@Component
public class JsonRecursos {

    private final ObjectMapper lector = new ObjectMapper();
    private JsonNode catalogo;
    private JsonNode demo;
    private JsonNode traducciones;

    public synchronized JsonNode catalogo() {
        if (catalogo == null) {
            catalogo = leer("/datos/catalogo.json");
        }
        return catalogo;
    }

    public synchronized JsonNode demo() {
        if (demo == null) {
            demo = leer("/datos/datos-demo.json");
        }
        return demo;
    }

    public synchronized JsonNode traducciones() {
        if (traducciones == null) {
            traducciones = leer("/i18n/traducciones.json");
        }
        return traducciones;
    }

    private JsonNode leer(String ruta) {
        try (InputStream in = Objects.requireNonNull(getClass().getResourceAsStream(ruta), ruta)) {
            return lector.readTree(in);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer " + ruta, e);
        }
    }
}
