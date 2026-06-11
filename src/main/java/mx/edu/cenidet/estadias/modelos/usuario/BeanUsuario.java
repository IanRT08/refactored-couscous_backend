package mx.edu.cenidet.estadias.modelos.usuario;

import jakarta.persistence.*;
import lombok.*;
import mx.edu.cenidet.estadias.modelos.HistorialAccion.BeanHistorialAccion;
import mx.edu.cenidet.estadias.modelos.administrador.BeanAdministrador;
import mx.edu.cenidet.estadias.modelos.alerta.BeanAlerta;
import mx.edu.cenidet.estadias.modelos.permisosDescarga.BeanPermisoDescarga;
import mx.edu.cenidet.estadias.modelos.solicitudDescarga.BeanSolicitudDescarga;
import mx.edu.cenidet.estadias.modelos.token.BeanToken;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "Usuario")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@EqualsAndHashCode(of = "idUsuario")

public class BeanUsuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idUsuario;

    @Column(unique = true, length = 60, nullable = false)
    private String nombreUsuario;

    @Column(unique = true, length = 60, nullable = false)
    private String correo;

    @Column(length = 90)
    private String nombreCompleto;

    @Column(length = 255, nullable = false)
    private String contrasenia;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private EstadoUsuario estado; //ACTIVO, INACTIVO, BLOQUEADO

    @Lob
    @Column(columnDefinition = "MEDIUMBLOB")
    private byte[] fotoPerfil;

    @CreationTimestamp
    private LocalDateTime fechaRegistro;

    @OneToOne(mappedBy = "usuario", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private BeanAdministrador administrador;

    @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<BeanSolicitudDescarga> solicitudesDescarga =  new ArrayList<>();

    @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<BeanPermisoDescarga> permisosDescarga = new ArrayList<>();

    @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<BeanHistorialAccion> historialAcciones = new ArrayList<>();

    @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<BeanToken> tokens = new ArrayList<>();

    @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<BeanAlerta> alertas = new ArrayList<>();

}
