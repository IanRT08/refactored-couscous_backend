package mx.edu.cenidet.estadias.modelos.administrador;


import jakarta.persistence.*;
import lombok.*;
import mx.edu.cenidet.estadias.modelos.usuario.BeanUsuario;

@Entity
@Table(name = "Administrador")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@EqualsAndHashCode(of = "idAdministrador")

public class BeanAdministrador {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idAdministrador;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipoAdministrador", length = 20, nullable = false)
    private TipoAdministrador tipoAdministrador;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Usuario_idUsuario", nullable = false, unique = true)
    private BeanUsuario usuario;

}
