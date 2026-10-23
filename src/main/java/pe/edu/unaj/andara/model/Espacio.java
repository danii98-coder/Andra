package pe.edu.unaj.andara.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "espacio", uniqueConstraints = @UniqueConstraint(name = "uk_espacio_codigo", columnNames = "codigo"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class Espacio {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 30)
    private String codigo;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private TipoEspacio tipo;

    @Column(nullable = false)
    private int capacidad;

    @Column(nullable = false)
    private boolean fueraDeServicio;

    @Column(length = 80)
    private String edificio;

    @ManyToOne(optional = false)
    @JoinColumn(name = "sede_id", nullable = false)
    private Sede sede;

    @ManyToOne
    @JoinColumn(name = "escuela_id")
    private EscuelaProfesional escuela;

    public String getPabellon() {
        return edificio == null ? "" : edificio.split(",")[0].trim();
    }
}
