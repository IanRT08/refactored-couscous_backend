package mx.edu.cenidet.estadias.dtos.estadisticas;

import lombok.*;

import java.time.LocalDateTime;

//Vista combinada del sistema híbrido. Voltaje/corriente/Voc NO se combinan:
//son circuitos eléctricamente distintos y sumarlos no representa una magnitud
//física real. Solo potencia (promedio, por linealidad) y energía (suma total)
//son comparables/sumables entre Fotovoltaico y Eólico.
@Getter
@Builder
public class ElectricCombinedStatisticsDTO {

    private LocalDateTime inicio;
    private LocalDateTime fin;

    private Double potenciaPromedioCombinada;
    private Double energiaTotalCombinada;

    //Desglose por fuente, para que el frontend pueda mostrar la contribución de cada una
    private ElectricStatisticsDTO fotovoltaico;
    private ElectricStatisticsDTO eolico;

}
