package pe.edu.unaj.andara.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pago", uniqueConstraints = @UniqueConstraint(name = "uk_pago_codigo", columnNames = "codigo"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class Pago {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String codigo;

    @ManyToOne(optional = false)
    @JoinColumn(name = "persona_id", nullable = false)
    private Persona persona;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private TipoPago tipo;

    @ManyToOne
    @JoinColumn(name = "tramite_id")
    private Tramite tramite;

    @Column(nullable = false, length = 160)
    private String concepto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private EstadoPago estado;

    @Column(nullable = false)
    private LocalDateTime creado;

    private LocalDate vence;

    private LocalDateTime pagado;

    @Enumerated(EnumType.STRING)
    @Column(length = 12)
    private MetodoPago metodo;

    @Column(length = 20)
    private String operacion;

    @Column(length = 20)
    private String boleta;

    @ManyToOne(optional = false)
    @JoinColumn(name = "sede_id", nullable = false)
    private Sede sede;

    @Embedded
    private DatosSolicitante solicitante;

    @Column(length = 60)
    private String motivo;

    @Column(columnDefinition = "BLOB")
    private byte[] foto;

    private LocalDate recojoFecha;

    @Column(length = 160)
    private String recojoTexto;

    @Builder.Default
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "pago_item", joinColumns = @JoinColumn(name = "pago_id"))
    @OrderColumn(name = "orden")
    private List<ItemPago> items = new ArrayList<>();

    public BigDecimal getTotal() {
        return items.stream().map(ItemPago::getImporte).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public boolean isPendiente() {
        return estado == EstadoPago.PENDIENTE;
    }

    public boolean isPagado() {
        return estado == EstadoPago.PAGADO;
    }
}
