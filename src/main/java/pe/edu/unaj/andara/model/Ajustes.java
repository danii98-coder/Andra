package pe.edu.unaj.andara.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "ajustes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class Ajustes {
    public static final String TODAS = "all";

    @Id
    private Long id;

    @Column(nullable = false, length = 2)
    private String idioma;

    @Column(nullable = false, length = 20)
    private String sede;

    @Column(length = 10)
    private String escuela;

    @Column(nullable = false)
    private boolean avisoWhatsapp;

    @Column(nullable = false)
    private boolean avisoCorreo;

    @Column(nullable = false)
    private boolean avisoApp;

    @Enumerated(EnumType.STRING)
    @Column(length = 14)
    private Rol sesion;

    private LocalDateTime sesionDesde;

    @Column(nullable = false)
    private boolean reducirMovimiento;

    private Double ventanaX;

    private Double ventanaY;

    private Double ventanaAncho;

    private Double ventanaAlto;

    @Column(nullable = false)
    private boolean maximizada;

    @Column(nullable = false)
    private int version;
}
