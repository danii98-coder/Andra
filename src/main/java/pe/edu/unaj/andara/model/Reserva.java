package pe.edu.unaj.andara.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "reserva", uniqueConstraints = @UniqueConstraint(name = "uk_reserva_codigo", columnNames = "codigo"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class Reserva {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String codigo;

    @ManyToOne(optional = false)
    @JoinColumn(name = "persona_id", nullable = false)
    private Persona persona;

    @ManyToOne(optional = false)
    @JoinColumn(name = "espacio_id", nullable = false)
    private Espacio espacio;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(nullable = false, length = 15)
    private String horario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private EstadoReserva estado;

    @Column(nullable = false, length = 300)
    private String motivo;

    @Column(nullable = false)
    private int asistentes;

    @Column(length = 300)
    private String nota;

    public String getInicio() {
        return horario.substring(0, 5);
    }

    public String getFin() {
        return horario.substring(horario.length() - 5);
    }
}
