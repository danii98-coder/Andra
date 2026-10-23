package pe.edu.unaj.andara.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.unaj.andara.model.Persona;
import pe.edu.unaj.andara.model.Prestamo;

import java.util.List;
import java.util.Optional;

@Repository
public interface PrestamoRepository extends JpaRepository<Prestamo, Long> {

    Optional<Prestamo> findByClave(String clave);

    List<Prestamo> findAllByOrderByIdDesc();

    List<Prestamo> findByPersonaOrderByIdDesc(Persona persona);

    boolean existsByCodigo(String codigo);
}
