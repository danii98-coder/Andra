package pe.edu.unaj.andara.seeder;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SembradorDemo {

    public record Resumen(int catalogo, boolean estadoNuevo) {
    }

    private final CatalogoSeeder catalogoSeeder;
    private final EstadoDemoSeeder estadoSeeder;

    public Resumen sembrar() {
        int catalogo = catalogoSeeder.sembrar();
        boolean nuevo = estadoSeeder.sembrar();
        return new Resumen(catalogo, nuevo);
    }

    public void restablecer() {
        catalogoSeeder.sembrar();
        estadoSeeder.restablecer();
    }
}
