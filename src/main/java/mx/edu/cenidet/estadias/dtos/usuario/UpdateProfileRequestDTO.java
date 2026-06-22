package mx.edu.cenidet.estadias.dtos.usuario;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileRequestDTO {

    @Size(max = 60, message = "El nombre no puede superar los 60 caracteres")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ]+(?: [a-zA-ZáéíóúÁÉÍÓÚñÑ]+)*$",
            message = "El nombre solo debe contener letras y un solo espacio entre palabras")
    private String nombreCompleto;

    @Size(min = 3, max = 60, message = "El nombre de usuario debe tener entre 3 y 60 caracteres")
    @Pattern(regexp = "^[a-zA-Z0-9._-]+$",
            message = "Solo se permiten letras, números, puntos, guiones y guiones bajos")
    private String nombreUsuario;

    private String fotoPerfil;

}
