package mx.edu.cenidet.estadias.dtos.downloadrequest;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateDownloadRequestDTO {

    @NotBlank(message = "El nombre completo es obligatorio")
    @Size(max = 90, message = "El nombre completo no debe superar los 90 caracteres")
    private String nombreCompleto;

    @NotBlank(message = "El motivo de la solicitud es obligatorio")
    @Size(max = 255, message = "El motivo no puede superar los 255 caracteres")
    private String motivo;

}
