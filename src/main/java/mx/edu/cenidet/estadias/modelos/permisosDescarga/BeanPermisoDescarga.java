package mx.edu.cenidet.estadias.modelos.permisosDescarga;

import jakarta.persistence.*;
import lombok.*;
import mx.edu.cenidet.estadias.modelos.solicitudDescarga.BeanSolicitudDescarga;
import mx.edu.cenidet.estadias.modelos.usuario.BeanUsuario;

@Entity
@Table(name = "permisosDescarga")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "idPermisosDescarga")

public class BeanPermisoDescarga {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idPermisosDescarga;

    @Enumerated(EnumType.STRING)
    @Column(name = "permisoDescarga", length = 20, nullable = false)
    private EstadoPermiso permisoDescarga;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Usuario_idUsuario", nullable = false)
    private BeanUsuario usuario;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "solicitudDescarga_idSolicitudDescarga", nullable = false, unique = true)
    private BeanSolicitudDescarga solicitudDescarga;

}
