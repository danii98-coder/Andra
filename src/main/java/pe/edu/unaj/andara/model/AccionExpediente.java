package pe.edu.unaj.andara.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AccionExpediente {
    @Column(nullable = false, length = 12)
    private String estilo;

    @Column(nullable = false, length = 20)
    private String accion;

    @Column(nullable = false, length = 60)
    private String texto;
}
