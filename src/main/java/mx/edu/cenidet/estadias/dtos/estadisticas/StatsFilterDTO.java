package mx.edu.cenidet.estadias.dtos.estadisticas;

import lombok.*;
import jakarta.validation.constraints.*;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StatsFilterDTO {

    @NotNull(message = "La fecha de inicio es obligatoria")
    private LocalDateTime inicio;

    @NotNull(message = "La fecha de fin es obligatoria")
    private LocalDateTime fin;

    @AssertTrue(message = "La fecha de inicio debe ser anterior a la fecha de fin")
    @JsonIgnore
    public boolean isRangoValido(){
        return inicio != null && fin != null && inicio.isBefore(fin);
    }

}
