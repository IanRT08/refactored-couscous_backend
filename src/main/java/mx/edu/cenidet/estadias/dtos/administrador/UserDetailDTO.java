package mx.edu.cenidet.estadias.dtos.administrador;

import lombok.*;

import java.time.LocalDateTime;


@Getter
@Builder
public class UserDetailDTO {

    private Long idUsuario;
    private String nombreUsuario;
    private String correo;
    private String nombreCompleto;
    private String estado;
    private String fotoPerfil;
    private LocalDateTime fechaRegistro;
    private boolean esAdministrador;
    private String tipoAdministrador;
    private boolean tienePermisoDescarga;

}
