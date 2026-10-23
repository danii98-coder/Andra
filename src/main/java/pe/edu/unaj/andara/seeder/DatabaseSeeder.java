package pe.edu.unaj.andara.seeder;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "app.seeder.enabled", havingValue = "true")
public class DatabaseSeeder implements ApplicationRunner {

    private final SembradorDemo sembrador;

    @Override
    public void run(ApplicationArguments args) {
        SembradorDemo.Resumen r = sembrador.sembrar();
        log.info("Seeder: {} registros de catálogo nuevos; estado de demostración {}.", r.catalogo(),
                r.estadoNuevo() ? "creado" : "existente");
    }
}
