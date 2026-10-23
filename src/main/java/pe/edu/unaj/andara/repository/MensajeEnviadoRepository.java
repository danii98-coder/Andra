package pe.edu.unaj.andara.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.unaj.andara.model.Expediente;
import pe.edu.unaj.andara.model.MensajeEnviado;

import java.util.List;

@Repository
public interface MensajeEnviadoRepository extends JpaRepository<MensajeEnviado, Long> {

    List<MensajeEnviado> findByExpedienteOrderByIdDesc(Expediente expediente);
}
