package mx.edu.cenidet.estadias.modelos.solicitudDescarga;

import jakarta.persistence.*;
import lombok.*;
import mx.edu.cenidet.estadias.modelos.permisosDescarga.BeanPermisoDescarga;
import mx.edu.cenidet.estadias.modelos.usuario.BeanUsuario;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;


@Entity
@Table(name = "solicitudDescarga")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "idSolicitudDescarga")

public class BeanSolicitudDescarga {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idSolicitudDescarga;

    @Column(length = 255, nullable = false)
    private String motivo;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private EstadoSolicitud estado;

    @CreationTimestamp
    @Column(name = "fechaSolicitud", updatable = false)
    private LocalDateTime fechaSolicitud;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Usuario_idUsuario", nullable = false)
    private BeanUsuario usuario;

    @OneToOne(mappedBy = "solicitudDescarga", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private BeanPermisoDescarga permisoDescarga;

}
