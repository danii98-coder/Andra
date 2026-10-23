package pe.edu.unaj.andara;

import org.springframework.test.context.DynamicPropertyRegistry;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public final class BaseTemporal {

    private BaseTemporal() {
    }

    public static void configurar(DynamicPropertyRegistry registro, String nombre) throws IOException {
        File db = Files.createTempFile(nombre, ".db").toFile();
        db.deleteOnExit();
        registro.add("spring.datasource.url", () -> "jdbc:sqlite:" + db.getAbsolutePath());
        registro.add("app.seeder.enabled", () -> "true");
    }
}
