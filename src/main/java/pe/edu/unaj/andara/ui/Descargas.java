package pe.edu.unaj.andara.ui;

import javafx.stage.FileChooser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.function.Supplier;

@Component
@RequiredArgsConstructor
public class Descargas {

    private final Notificador notificador;

    public void guardarPdf(String nombreSugerido, Supplier<byte[]> contenido) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Guardar como");
        fc.setInitialFileName(nombreSugerido.replaceAll("[\\\\/:*?\"<>|]", "-") + ".pdf");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        File destino = fc.showSaveDialog(notificador.ventana());
        if (destino == null) {
            return;
        }
        try {
            Files.write(destino.toPath(), contenido.get());
            notificador.exito("Archivo guardado: " + destino.getName());
        } catch (IOException | RuntimeException e) {
            notificador.error("No se pudo guardar el archivo. Intenta en otra carpeta.");
        }
    }
}
