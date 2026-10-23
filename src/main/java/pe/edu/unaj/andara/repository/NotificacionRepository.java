package pe.edu.unaj.andara.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.unaj.andara.model.Notificacion;
import pe.edu.unaj.andara.model.Rol;

import java.util.List;

@Repository
public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {

    List<Notificacion> findByDestinatarioOrderByIdDesc(Rol destinatario);

    long countByDestinatarioAndLeidaFalse(Rol destinatario);
}
