package pe.edu.unaj.andara.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "consulta_asesor", uniqueConstraints = @UniqueConstraint(name = "uk_consulta_expediente", columnNames = "expediente_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class ConsultaAsesor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "expediente_id", nullable = false)
    private Expediente expediente;

    @Column(nullable = false)
    private int usadas;

    @Column(nullable = false)
    private int cupo;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private EstadoAmpliacion ampliacion = EstadoAmpliacion.NINGUNA;

    @Builder.Default
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "consulta_asesor_mensaje", joinColumns = @JoinColumn(name = "consulta_id"))
    @OrderColumn(name = "orden")
    private List<MensajeAsesor> mensajes = new ArrayList<>();

    public int restantes() {
        return Math.max(0, cupo - usadas);
    }
}
