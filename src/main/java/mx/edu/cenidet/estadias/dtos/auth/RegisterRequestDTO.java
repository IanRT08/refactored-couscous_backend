package mx.edu.cenidet.estadias.dtos.auth;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequestDTO {

    @NotBlank(message = "El nombre de usuario es obligatorio")
    @Size(min = 3, max = 60, message = "El nombre de usuario debe tener entre 3 y 60 caracteres")
    @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "Solo se permiten letras, números, puntos, guiones y guiones bajos")
    private String nombreUsuario;

    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "El fotmato del correo no es válido")
    @Size(max = 60, message = "El correo no puede superar los 60 caracteres")
    private String correo;

    @Size(max = 90, message = "El nombre completo no puede superar los 90 caracteres")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ]+(?: [a-zA-ZáéíóúÁÉÍÓÚñÑ]+)*$",
            message = "El nombre solo debe contener letras y un solo espacio entre palabras")
    private String nombreCompleto;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 8, max = 255)
    @Pattern(regexp  = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&_\\-])[A-Za-z\\d@$!%*?&_\\-]{8,}$",
            message = "La contraseña debe tener al menos 1 mayúscula, 1 minúscula, 1 número y 1 carácter especial (@$!%*?&_-)")
    private String contrasenia;

    @NotBlank(message = "La confirmación de contraseña es obligatoria")
    private String confirmarContrasenia;

    @AssertTrue(message = "Las contraseñas no coinciden")
    @JsonIgnore
    public boolean isContraseniaConfirmada() {
        return contrasenia != null && contrasenia.equals(confirmarContrasenia);
    }

}
