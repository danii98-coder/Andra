package pe.edu.unaj.andara.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.unaj.andara.model.CategoriaEquipo;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoriaEquipoRepository extends JpaRepository<CategoriaEquipo, Long> {

    Optional<CategoriaEquipo> findByCodigo(String codigo);

    List<CategoriaEquipo> findAllByOrderByIdAsc();
}
