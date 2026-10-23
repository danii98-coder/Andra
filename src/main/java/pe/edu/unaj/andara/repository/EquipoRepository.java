package pe.edu.unaj.andara.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.unaj.andara.model.Equipo;

import java.util.List;
import java.util.Optional;

@Repository
public interface EquipoRepository extends JpaRepository<Equipo, Long> {

    Optional<Equipo> findByCodigo(String codigo);

    List<Equipo> findAllByOrderByIdAsc();
}
