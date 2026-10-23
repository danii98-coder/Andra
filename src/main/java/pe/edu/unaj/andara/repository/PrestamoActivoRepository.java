package pe.edu.unaj.andara.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.unaj.andara.model.PrestamoActivo;

import java.util.Optional;

@Repository
public interface PrestamoActivoRepository extends JpaRepository<PrestamoActivo, Long> {

    Optional<PrestamoActivo> findFirstByOrderByIdDesc();
}
