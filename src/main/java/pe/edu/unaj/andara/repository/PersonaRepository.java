package pe.edu.unaj.andara.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.unaj.andara.model.Persona;

import java.util.List;
import java.util.Optional;

@Repository
public interface PersonaRepository extends JpaRepository<Persona, Long> {

    Optional<Persona> findByCodigo(String codigo);

    Optional<Persona> findFirstByDni(String dni);

    Optional<Persona> findFirstByPrincipalTrue();

    List<Persona> findAllByOrderByIdAsc();
}
