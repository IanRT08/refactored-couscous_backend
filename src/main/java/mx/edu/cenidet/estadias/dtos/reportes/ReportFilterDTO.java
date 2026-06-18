package mx.edu.cenidet.estadias.dtos.reportes;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ReportFilterDTO {

    @NotNull(message = "La fecha de inicio es obligatoria")
    private LocalDateTime inicio;

    @NotNull(message = "La fecha de fin es obligatoria")
    private LocalDateTime fin;

    @NotNull(message = "El tipo de reporte es obligatorio")
    private TipoReporte tipo;

    @NotNull(message = "El formato es obligatorio")
    private FormatoReporte formato;

    @AssertTrue(message = "La fecha de inicio debe ser anterior a la fecha fin")
    @JsonIgnore
    public boolean isRangoValido() {
        return inicio != null && fin != null && inicio.isBefore(fin);
    }

}
