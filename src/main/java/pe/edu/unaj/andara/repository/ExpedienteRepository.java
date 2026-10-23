package pe.edu.unaj.andara.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.unaj.andara.model.Expediente;
import pe.edu.unaj.andara.model.Pago;
import pe.edu.unaj.andara.model.Persona;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExpedienteRepository extends JpaRepository<Expediente, Long> {

    Optional<Expediente> findByCodigo(String codigo);

    Optional<Expediente> findFirstByPago(Pago pago);

    List<Expediente> findAllByOrderByIdDesc();

    List<Expediente> findByPersonaOrderByIdDesc(Persona persona);

    boolean existsByCodigo(String codigo);
}
