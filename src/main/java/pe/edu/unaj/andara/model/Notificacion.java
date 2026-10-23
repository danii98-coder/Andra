package pe.edu.unaj.andara.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "notificacion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class Notificacion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 14)
    private Rol destinatario;

    @Column(nullable = false, length = 300)
    private String texto;

    @Column(nullable = false, length = 20)
    private String ruta;

    @Column(nullable = false)
    private boolean leida;

    @Column(nullable = false)
    private LocalDateTime creada;
}
