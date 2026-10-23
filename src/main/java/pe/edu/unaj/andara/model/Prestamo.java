package pe.edu.unaj.andara.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "prestamo", uniqueConstraints = @UniqueConstraint(name = "uk_prestamo_codigo", columnNames = "codigo"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class Prestamo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String codigo;

    @Column(nullable = false, length = 20)
    private String clave;

    @ManyToOne(optional = false)
    @JoinColumn(name = "persona_id", nullable = false)
    private Persona persona;

    @ManyToOne
    @JoinColumn(name = "equipo_id")
    private Equipo equipo;

    @Column(nullable = false, length = 160)
    private String nombre;

    @Column(nullable = false, length = 240)
    private String detalle;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private EstadoPrestamo estado;

    @ManyToOne(optional = false)
    @JoinColumn(name = "sede_id", nullable = false)
    private Sede sede;
}
