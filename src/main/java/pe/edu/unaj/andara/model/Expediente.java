package pe.edu.unaj.andara.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "expediente", uniqueConstraints = @UniqueConstraint(name = "uk_expediente_codigo", columnNames = "codigo"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class Expediente {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 30)
    private String codigo;

    @ManyToOne(optional = false)
    @JoinColumn(name = "persona_id", nullable = false)
    private Persona persona;

    @ManyToOne(optional = false)
    @JoinColumn(name = "sede_id", nullable = false)
    private Sede sede;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 6)
    private ClaseExpediente clase;

    @ManyToOne
    @JoinColumn(name = "tramite_id")
    private Tramite tramite;

    @ManyToOne
    @JoinColumn(name = "pago_id")
    private Pago pago;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private GrupoExpediente grupo;

    @Column(nullable = false, length = 20)
    private String icono;

    @Column(nullable = false, length = 20)
    private String rotuloIcono;

    @Column(nullable = false, length = 10)
    private String tono;

    @Column(nullable = false, length = 100)
    private String estado;

    @Column(nullable = false, length = 200)
    private String titulo;

    @Column(length = 400)
    private String descripcion;

    @Column(nullable = false)
    private int paso;

    @Column(length = 500)
    private String observacion;

    @Column(nullable = false)
    private boolean destacado;

    @Column(length = 200)
    private String nota;

    @Embedded
    private DatosSolicitante solicitante;

    private LocalDateTime fecha;

    @Builder.Default
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "expediente_dato", joinColumns = @JoinColumn(name = "expediente_id"))
    @OrderColumn(name = "orden")
    private List<DatoExpediente> datos = new ArrayList<>();

    @Builder.Default
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "expediente_etiqueta", joinColumns = @JoinColumn(name = "expediente_id"))
    @OrderColumn(name = "orden")
    @Column(name = "etiqueta", length = 120)
    private List<String> etiquetas = new ArrayList<>();

    @Builder.Default
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "expediente_accion", joinColumns = @JoinColumn(name = "expediente_id"))
    @OrderColumn(name = "orden")
    private List<AccionExpediente> acciones = new ArrayList<>();

    public boolean isEntregado() {
        return estado != null && estado.contains("Entregado");
    }

    public boolean isConPagoPendiente() {
        return pago != null && pago.isPendiente();
    }

    public void ponerAcciones(String... ternas) {
        acciones.clear();
        for (int i = 0; i + 2 < ternas.length; i += 3) {
            acciones.add(new AccionExpediente(ternas[i], ternas[i + 1], ternas[i + 2]));
        }
    }

    public void ponerDato(int indice, String etiqueta, String valor) {
        while (datos.size() <= indice) {
            datos.add(new DatoExpediente("", ""));
        }
        datos.set(indice, new DatoExpediente(etiqueta, valor));
    }
}
