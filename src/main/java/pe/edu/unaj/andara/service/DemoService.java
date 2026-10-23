package pe.edu.unaj.andara.service;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import pe.edu.unaj.andara.seeder.SembradorDemo;

@Service
@RequiredArgsConstructor
public class DemoService {

    private final SembradorDemo sembrador;
    private final ApplicationEventPublisher eventos;

    public void restablecer() {
        sembrador.restablecer();
        eventos.publishEvent(new CambioDatos("demo"));
    }
}
