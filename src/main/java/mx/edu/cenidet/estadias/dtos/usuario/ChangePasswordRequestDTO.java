package mx.edu.cenidet.estadias.dtos.usuario;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChangePasswordRequestDTO {

    @NotBlank(message = "La contraseña actual es obligatoria")
    private String contraseniaActual;

    @NotBlank(message = "La nueva contraseña es obligatoria")
    @Size(min = 8, max = 255)
    @Pattern(regexp  = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&_\\-])[A-Za-z\\d@$!%*?&_\\-]{8,}$",
            message = "La contraseña debe tener al menos 1 mayúscula, 1 minúscula, 1 número y 1 carácter especial")
    private String nuevaContrasenia;

    @NotBlank(message = "La confirmacion de contraseña es obligatoria")
    private String confirmarContrasenia;

    @AssertTrue(message = "Las contraseñas no coinciden")
    @JsonIgnore
    public boolean isContraseniaConfirmada(){
        return nuevaContrasenia != null && nuevaContrasenia.equals(confirmarContrasenia);
    }

}
