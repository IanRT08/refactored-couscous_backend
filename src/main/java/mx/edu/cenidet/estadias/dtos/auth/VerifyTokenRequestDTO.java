package mx.edu.cenidet.estadias.dtos.auth;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VerifyTokenRequestDTO {

    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "El formato del correo no es válido")
    private String correo;

    @NotNull(message = "El código de verificación es obligatorio")
    private Integer codigo;

}
