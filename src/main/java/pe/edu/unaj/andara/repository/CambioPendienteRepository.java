package pe.edu.unaj.andara.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.unaj.andara.model.CambioPendiente;

import java.util.List;

@Repository
public interface CambioPendienteRepository extends JpaRepository<CambioPendiente, Long> {

    List<CambioPendiente> findAllByOrderByIdAsc();
}
