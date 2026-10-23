package pe.edu.unaj.andara.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "sede", uniqueConstraints = @UniqueConstraint(name = "uk_sede_codigo", columnNames = "codigo"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class Sede {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String codigo;

    @Column(nullable = false, length = 40)
    private String nombre;

    @Column(nullable = false, length = 60)
    private String nombreCompleto;

    @Column(length = 200)
    private String descripcion;

    @Column(length = 120)
    private String oficina;

    @Column(length = 120)
    private String horario;
}
