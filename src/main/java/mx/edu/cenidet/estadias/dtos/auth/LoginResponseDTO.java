package mx.edu.cenidet.estadias.dtos.auth;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Builder
public class LoginResponseDTO {

    //JWT pata el header Authotization
    private String token;
    private String tipo;

    //Datos del usuario autenticado para el AuthContext
    private Long idUsuario;
    private String nombreUsuario;
    private String nombreCompleto;
    private String correo;

    //Determina la redirección despues del login
    private String rol;

    //Cierre de sesion automatico
    private LocalDateTime fechaExpiracionToken;

    // Obliga al admin a cambiar su contraseña temporal en el primer login
    private boolean debeRestablecerPassword;

}
