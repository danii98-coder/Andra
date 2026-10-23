package pe.edu.unaj.andara.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "favorito", uniqueConstraints = @UniqueConstraint(name = "uk_favorito", columnNames = {"tipo", "codigo"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class Favorito {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 14)
    private TipoFavorito tipo;

    @Column(nullable = false, length = 30)
    private String codigo;
}
