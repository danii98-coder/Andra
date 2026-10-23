package pe.edu.unaj.andara.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "persona", uniqueConstraints = @UniqueConstraint(name = "uk_persona_codigo", columnNames = "codigo"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class Persona {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String codigo;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(length = 8)
    private String dni;

    @Column(nullable = false, length = 30)
    private String tipo;

    @ManyToOne
    @JoinColumn(name = "escuela_id")
    private EscuelaProfesional escuela;

    @ManyToOne(optional = false)
    @JoinColumn(name = "sede_id", nullable = false)
    private Sede sede;

    @Column(length = 80)
    private String correo;

    @Column(length = 15)
    private String celular;

    @Column(nullable = false)
    private boolean principal;

    public String getPrimerNombre() {
        return nombre.split(" ")[0];
    }

    public String getIniciales() {
        StringBuilder sb = new StringBuilder();
        for (String p : nombre.split(" ")) {
            if (!p.isEmpty() && Character.isUpperCase(p.charAt(0)) && sb.length() < 2) {
                sb.append(p.charAt(0));
            }
        }
        return sb.toString();
    }
}
