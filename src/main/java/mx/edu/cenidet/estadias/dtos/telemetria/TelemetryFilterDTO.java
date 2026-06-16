package mx.edu.cenidet.estadias.dtos.telemetria;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TelemetryFilterDTO {

    @NotNull(message = "La fecha de inicio es obligatoria")
    private LocalDateTime inicio;

    @NotNull(message = "La fecha de fin es obligatoria")
    private LocalDateTime fin;

    private int page = 0;
    private int size = 500;

    @AssertTrue(message = "La fecha de inicio debe ser anterior a la fecha de fin")
    @JsonIgnore
    public boolean isRangoValido(){
        return inicio != null && fin != null && inicio.isBefore(fin);
    }

}
