package pe.edu.unaj.andara.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "equipo", uniqueConstraints = @UniqueConstraint(name = "uk_equipo_codigo", columnNames = "codigo"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class Equipo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 10)
    private String codigo;

    @ManyToOne(optional = false)
    @JoinColumn(name = "categoria_id", nullable = false)
    private CategoriaEquipo categoria;

    @Column(nullable = false, length = 40)
    private String rotulo;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(length = 240)
    private String especificaciones;

    @Column(length = 120)
    private String ubicacion;

    @Column(length = 80)
    private String puntoEntrega;

    @ManyToOne(optional = false)
    @JoinColumn(name = "sede_id", nullable = false)
    private Sede sede;

    @ManyToOne
    @JoinColumn(name = "escuela_id")
    private EscuelaProfesional escuela;

    @Column(nullable = false)
    private int unidadesIniciales;

    @Column(nullable = false)
    private int stock;

    @Column(nullable = false, length = 10)
    private String unidad;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ModoEquipo modo;

    @Column(length = 10)
    private String proximoTurno;

    public boolean isDisponibleAhora() {
        return stock > 0 && modo != ModoEquipo.TURNO;
    }

    public boolean isPorTurno() {
        return modo == ModoEquipo.TURNO;
    }
}
