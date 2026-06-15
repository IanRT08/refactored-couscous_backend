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

    private String fotoPerfil;

}
