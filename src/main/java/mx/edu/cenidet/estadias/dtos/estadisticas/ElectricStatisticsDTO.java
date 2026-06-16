package mx.edu.cenidet.estadias.dtos.estadisticas;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Builder
public class ElectricStatisticsDTO {

    private LocalDateTime inicio;
    private LocalDateTime fin;

    //Corriente
    private Double promedioCorriente;
    private Double maxCorriente;
    private Double minCorriente;

    //Voltaje
    private Double promedioVoltaje;
    private Double maxVoltaje;
    private Double minVoltaje;

    //Potencia
    private Double promedioPotencia;
    private Double maxPotencia;
    private Double minPotencia;

    //Energia
    private Double promedioEnergia;
    private Double maxEnergia;
    private Double minEnergia;

}
