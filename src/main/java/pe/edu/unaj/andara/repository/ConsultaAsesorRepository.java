package pe.edu.unaj.andara.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.unaj.andara.model.ConsultaAsesor;
import pe.edu.unaj.andara.model.Expediente;

import java.util.Optional;

@Repository
public interface ConsultaAsesorRepository extends JpaRepository<ConsultaAsesor, Long> {

    Optional<ConsultaAsesor> findByExpediente(Expediente expediente);
}
