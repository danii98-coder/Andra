package pe.edu.unaj.andara.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "matricula")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class Matricula {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 10)
    private String periodo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private EstadoMatricula estado;

    @ManyToOne(optional = false)
    @JoinColumn(name = "pago_id", nullable = false)
    private Pago pago;

    public boolean isConfirmada() {
        return estado == EstadoMatricula.CONFIRMADA;
    }
}
