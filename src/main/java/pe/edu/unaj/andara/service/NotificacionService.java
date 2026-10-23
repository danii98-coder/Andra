package pe.edu.unaj.andara.service;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.unaj.andara.model.Notificacion;
import pe.edu.unaj.andara.model.Rol;
import pe.edu.unaj.andara.repository.NotificacionRepository;
import pe.edu.unaj.andara.util.Fechas;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificacionService {

    private final NotificacionRepository repositorio;
    private final ApplicationEventPublisher eventos;

    @Transactional(readOnly = true)
    public List<Notificacion> listar(Rol rol) {
        return repositorio.findByDestinatarioOrderByIdDesc(rol);
    }

    @Transactional(readOnly = true)
    public long noLeidas(Rol rol) {
        return rol == null ? 0 : repositorio.countByDestinatarioAndLeidaFalse(rol);
    }

    @Transactional
    public void marcarTodas(Rol rol) {
        List<Notificacion> l = repositorio.findByDestinatarioOrderByIdDesc(rol);
        l.forEach(n -> n.setLeida(true));
        repositorio.saveAll(l);
        eventos.publishEvent(new CambioDatos("notificaciones"));
    }

    @Transactional
    public String abrir(Long id) {
        Notificacion n = repositorio.findById(id).orElseThrow();
        n.setLeida(true);
        repositorio.save(n);
        eventos.publishEvent(new CambioDatos("notificaciones"));
        return n.getRuta();
    }

    @Transactional
    public void notificarEstudiante(String texto, String ruta) {
        guardar(Rol.ESTUDIANTE, texto, ruta);
    }

    @Transactional
    public void notificarAdministrador(String texto, String ruta) {
        guardar(Rol.ADMINISTRADOR, texto, ruta);
    }

    @Transactional
    public void quitarPorTexto(String fragmento) {
        repositorio.findByDestinatarioOrderByIdDesc(Rol.ESTUDIANTE).stream()
                .filter(n -> n.getTexto().contains(fragmento)).forEach(repositorio::delete);
        eventos.publishEvent(new CambioDatos("notificaciones"));
    }

    private void guardar(Rol rol, String texto, String ruta) {
        repositorio.save(Notificacion.builder().destinatario(rol).texto(texto).ruta(ruta).leida(false)
                .creada(Fechas.ahora()).build());
        eventos.publishEvent(new CambioDatos("notificaciones"));
    }
}
