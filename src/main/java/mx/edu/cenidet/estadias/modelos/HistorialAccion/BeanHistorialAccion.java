package mx.edu.cenidet.estadias.modelos.HistorialAccion;

import jakarta.persistence.*;
import lombok.*;
import mx.edu.cenidet.estadias.modelos.usuario.BeanUsuario;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "historialAcciones")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "idAccion")
public class BeanHistorialAccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idAccion;

    @Column(name = "tipoAccion", length = 60, nullable = false)
    private String tipoAccion;

    @Column(length = 255)
    private String descripcion;

    @CreationTimestamp
    @Column(name = "fechaAccion", updatable = false)
    private LocalDateTime fechaAccion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Usuario_idUsuario", nullable = false)
    private BeanUsuario usuario;

}
