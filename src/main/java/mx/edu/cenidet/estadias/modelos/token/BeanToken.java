package mx.edu.cenidet.estadias.modelos.token;

import jakarta.persistence.*;
import lombok.*;
import mx.edu.cenidet.estadias.modelos.usuario.BeanUsuario;
import java.time.LocalDateTime;

@Entity
@Table(name = "Token")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "idToken")

public class BeanToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idToken;

    @Column(nullable = false)
    private Integer codigo;

    @Enumerated(EnumType.STRING)
    private TipoToken tipo;

    @Column(name = "fechaExpiracion", nullable = false)
    private LocalDateTime fechaExpiracion;

    @Convert(converter = SiNoBooleanConventer.class)
    @Column(length = 20, nullable = false)
    @Builder.Default
    private Boolean usado = Boolean.FALSE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Usuario_idUsuario", nullable = false)
    private BeanUsuario usuario;

    @Transient
    public boolean esValido() {
        return Boolean.FALSE.equals(usado) && LocalDateTime.now().isBefore(fechaExpiracion);
    }

}
