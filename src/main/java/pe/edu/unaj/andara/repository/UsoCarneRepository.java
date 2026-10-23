package pe.edu.unaj.andara.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.unaj.andara.model.UsoCarne;

import java.util.List;

@Repository
public interface UsoCarneRepository extends JpaRepository<UsoCarne, Long> {

    List<UsoCarne> findAllByOrderByFechaDesc();
}
