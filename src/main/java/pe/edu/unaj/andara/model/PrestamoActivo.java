package pe.edu.unaj.andara.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "prestamo_activo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class PrestamoActivo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String codigo;

    @ManyToOne(optional = false)
    @JoinColumn(name = "persona_id", nullable = false)
    private Persona persona;

    @Column(nullable = false, length = 120)
    private String equipo;

    @Column(nullable = false, length = 60)
    private String retiro;

    @Column(nullable = false)
    private LocalDateTime limite;

    @Column(nullable = false)
    private boolean renovado;

    @Column(length = 60)
    private String incidencia;

    @Column(nullable = false, length = 160)
    private String lugar;

    @Column(nullable = false)
    private boolean devuelto;
}
