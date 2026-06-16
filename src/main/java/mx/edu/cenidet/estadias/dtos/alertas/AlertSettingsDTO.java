package mx.edu.cenidet.estadias.dtos.alertas;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AlertSettingsDTO {

    @NotBlank(message = "La preferencia de alertas es obligatoria")
    @Pattern(regexp  = "TODAS|SISTEMA|NINGUNA", message = "La preferencia debe ser TODAS, SISTEMA o NINGUNA")
    private String preferencia;

}
