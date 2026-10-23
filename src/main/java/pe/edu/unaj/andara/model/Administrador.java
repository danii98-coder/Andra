package pe.edu.unaj.andara.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "administrador", uniqueConstraints = @UniqueConstraint(name = "uk_admin_correo", columnNames = "correo"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class Administrador {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, length = 80)
    private String correo;

    @Column(length = 8)
    private String dni;

    @Column(length = 120)
    private String cargo;

    public String getPrimerNombre() {
        return nombre.split(" ")[0];
    }
}
