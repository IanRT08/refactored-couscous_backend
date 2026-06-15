package mx.edu.cenidet.estadias.dtos.usuario;

import lombok.*;
import jakarta.validation.constraints.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DesactiveAccountRequestDTO {

    @NotNull(message = "El código de confirmación es obligatorio")
    private Integer codigoToken;

    @NotBlank(message = "La contraseña es obligatoria")
    private String contrasenia;

}
