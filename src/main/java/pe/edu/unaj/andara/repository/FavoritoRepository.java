package pe.edu.unaj.andara.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.unaj.andara.model.Favorito;
import pe.edu.unaj.andara.model.TipoFavorito;

import java.util.List;
import java.util.Optional;

@Repository
public interface FavoritoRepository extends JpaRepository<Favorito, Long> {

    List<Favorito> findByTipoOrderByIdAsc(TipoFavorito tipo);

    Optional<Favorito> findByTipoAndCodigo(TipoFavorito tipo, String codigo);

    boolean existsByTipoAndCodigo(TipoFavorito tipo, String codigo);
}
