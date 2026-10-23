package pe.edu.unaj.andara.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.unaj.andara.model.Ajustes;
import pe.edu.unaj.andara.repository.AjustesRepository;

import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
public class AjustesService {

    private final AjustesRepository repositorio;

    @Transactional(readOnly = true)
    public Ajustes obtener() {
        return repositorio.findById(1L).orElseGet(() -> Ajustes.builder().id(1L).idioma("es").sede("capilla")
                .avisoWhatsapp(true).avisoCorreo(true).avisoApp(true).version(5).build());
    }

    @Transactional
    public Ajustes cambiar(Consumer<Ajustes> cambio) {
        Ajustes a = obtener();
        cambio.accept(a);
        return repositorio.save(a);
    }
}
