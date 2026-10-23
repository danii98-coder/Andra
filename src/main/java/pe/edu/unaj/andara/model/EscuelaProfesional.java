package pe.edu.unaj.andara.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "escuela_profesional", uniqueConstraints = @UniqueConstraint(name = "uk_escuela_codigo", columnNames = "codigo"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class EscuelaProfesional {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 10)
    private String codigo;

    @Column(nullable = false, length = 80)
    private String nombre;

    @ManyToOne(optional = false)
    @JoinColumn(name = "sede_id", nullable = false)
    private Sede sede;
}
