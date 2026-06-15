package mx.edu.cenidet.estadias.dtos.usuario;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Builder
public class UserProfileDTO {

    private Long idUsuario;
    private String nombreUsuario;
    private String correo;
    private String nombreCompleto;
    private String estado;
    private String fotoPerfil;
    private LocalDateTime fechaRegistro;

    //Determina si muestra la seccion de reportes al usuario
    private boolean tienePermisoDescarga;

    //null si no es administrador
    private String tipoAdministrador;

}
