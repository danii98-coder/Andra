package pe.edu.unaj.andara.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

@Entity
@Table(name = "tramite", uniqueConstraints = @UniqueConstraint(name = "uk_tramite_codigo", columnNames = "codigo"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class Tramite {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String codigo;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(nullable = false, length = 40)
    private String categoria;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal monto;

    @Column(nullable = false, length = 12)
    private String entrega;

    @Column(nullable = false, length = 160)
    private String publicos;

    @Column(length = 20)
    private String plantilla;

    @Column(length = 240)
    private String descripcion;

    @Column(length = 30)
    private String unidad;

    @Column(nullable = false)
    private boolean porCantidad;

    public List<String> getListaPublicos() {
        return Arrays.asList(publicos.split("\\|"));
    }

    public boolean admite(String tipoSolicitante) {
        return getListaPublicos().contains(tipoSolicitante);
    }

    public boolean isCarne() {
        return "Carné".equals(categoria);
    }

    public boolean generaDocumento() {
        return plantilla != null && !plantilla.isBlank();
    }

    public String getTextoEntrega() {
        return switch (entrega) {
            case "mismo" -> "Recojo el mismo día";
            case "inmediato" -> "Habilitación inmediata";
            case "carne" -> "10 días hábiles";
            default -> entrega + " días hábiles";
        };
    }
}
