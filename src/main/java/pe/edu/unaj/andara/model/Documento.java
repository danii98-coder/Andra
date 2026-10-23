package pe.edu.unaj.andara.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "documento", uniqueConstraints = @UniqueConstraint(name = "uk_documento_codigo", columnNames = "codigo"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class Documento {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String codigo;

    @Column(nullable = false, length = 200)
    private String nombre;

    @Column(length = 400)
    private String descripcion;

    @Column(nullable = false, length = 60)
    private String tipo;

    @Column(nullable = false, length = 120)
    private String dependencia;

    @Column(nullable = false, length = 40)
    private String publico;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(nullable = false)
    private boolean frecuente;

    @Column(nullable = false)
    private int paginas;

    @ManyToOne
    @JoinColumn(name = "escuela_id")
    private EscuelaProfesional escuela;
}
