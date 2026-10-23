package pe.edu.unaj.andara.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.unaj.andara.model.Pago;
import pe.edu.unaj.andara.model.Persona;

import java.util.List;
import java.util.Optional;

@Repository
public interface PagoRepository extends JpaRepository<Pago, Long> {

    Optional<Pago> findByCodigo(String codigo);

    List<Pago> findAllByOrderByCreadoDesc();

    List<Pago> findByPersonaOrderByCreadoDesc(Persona persona);

    long countByBoletaIsNotNull();
}
