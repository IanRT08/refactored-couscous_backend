package mx.edu.cenidet.estadias.dtos.estadisticas;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClimateResumenDTO {
    private ClimateStatisticsDTO hoy;
    private ClimateStatisticsDTO ayer;
    private ClimateStatisticsDTO semana;
    private ClimateStatisticsDTO mes;
    private ClimateStatisticsDTO anio;
}
