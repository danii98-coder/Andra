package pe.edu.unaj.andara.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.unaj.andara.model.Espacio;

import java.util.List;
import java.util.Optional;

@Repository
public interface EspacioRepository extends JpaRepository<Espacio, Long> {

    Optional<Espacio> findByCodigo(String codigo);

    Optional<Espacio> findFirstByNombre(String nombre);

    List<Espacio> findAllByOrderByIdAsc();
}
