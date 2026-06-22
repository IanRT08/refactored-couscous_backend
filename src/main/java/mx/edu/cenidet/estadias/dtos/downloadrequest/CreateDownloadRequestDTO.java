package mx.edu.cenidet.estadias.dtos.downloadrequest;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateDownloadRequestDTO {

    //Obligatorio solo si el usuario no lo registró en su perfil (validado en el Service).
    @Size(max = 90, message = "El nombre completo no debe superar los 90 caracteres")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ]+(?: [a-zA-ZáéíóúÁÉÍÓÚñÑ]+)*$",
            message = "El nombre solo debe contener letras y un solo espacio entre palabras")
    private String nombreCompleto;

    @NotBlank(message = "El motivo de la solicitud es obligatorio")
    @Size(max = 255, message = "El motivo no puede superar los 255 caracteres")
    private String motivo;

}
