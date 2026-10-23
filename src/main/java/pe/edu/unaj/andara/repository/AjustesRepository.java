package pe.edu.unaj.andara.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.unaj.andara.model.Ajustes;

@Repository
public interface AjustesRepository extends JpaRepository<Ajustes, Long> {
}
