package mx.edu.cenidet.estadias.dtos.auth;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequestDTO {

    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "Solo se puede inciar sesion con correo y contraseña")
    private String correo;

    @NotBlank(message = "La contraseña es obligatoria")
    private String contrasenia;

}
