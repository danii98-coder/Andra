package pe.edu.unaj.andara.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.unaj.andara.model.Tramite;

import java.util.List;
import java.util.Optional;

@Repository
public interface TramiteRepository extends JpaRepository<Tramite, Long> {

    Optional<Tramite> findByCodigo(String codigo);

    Optional<Tramite> findFirstByNombre(String nombre);

    List<Tramite> findAllByOrderByIdAsc();
}
