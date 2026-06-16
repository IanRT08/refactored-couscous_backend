package mx.edu.cenidet.estadias.dtos.estadisticas;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Builder
public class ClimateStatisticsDTO {

    //Rango consultado
    private LocalDateTime inicio;
    private LocalDateTime fin;

    //Temperatura
    private Double promedioTemperatura;
    private Double maxTemperatura;
    private Double minTemperatura;

    //Viento
    private Double promedioViento;
    private Double maxViento;
    private Double minViento;

    //Humedad
    private Double promedioHumedad;
    private Double maxHumedad;
    private Double minHumedad;

    //Radiacion solar
    private Double promedioRadiacion;
    private Double maxRadiacion;
    private Double minRadiacion;

    //Presion
    private Double promedioPresion;
    private Double maxPresion;
    private Double minPresion;

}
