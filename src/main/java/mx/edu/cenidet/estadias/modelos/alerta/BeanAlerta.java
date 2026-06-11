package mx.edu.cenidet.estadias.modelos.alerta;

import jakarta.persistence.*;
import lombok.*;
import mx.edu.cenidet.estadias.modelos.usuario.BeanUsuario;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "Alertas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "idAlerta")

public class BeanAlerta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idAlerta;

    @Column(length = 255, nullable = false)
    private String tipo;

    @Column(length = 255, nullable = false)
    private String mensaje;

    @CreationTimestamp
    @Column(name = "fechaCreacion", updatable = false)
    private LocalDateTime fechaCreacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Usuario_idUsuario")
    private BeanUsuario usuario;

}
