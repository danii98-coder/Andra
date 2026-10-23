package pe.edu.unaj.andara.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.unaj.andara.model.EscuelaProfesional;

import java.util.List;
import java.util.Optional;

@Repository
public interface EscuelaProfesionalRepository extends JpaRepository<EscuelaProfesional, Long> {

    Optional<EscuelaProfesional> findByCodigo(String codigo);

    List<EscuelaProfesional> findAllByOrderByIdAsc();
}
