package mx.edu.cenidet.estadias.dtos.auth;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ForgotPasswordRequestDTO {

    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "El formato de correo no es valido")
    private String correo;

}
