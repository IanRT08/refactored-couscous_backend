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
    private Double modaTemperatura;

    //Viento
    private Double promedioViento;
    private Double maxViento;
    private Double minViento;
    private Double modaViento;

    //Humedad
    private Double promedioHumedad;
    private Double maxHumedad;
    private Double minHumedad;
    private Double modaHumedad;

    //Radiacion solar
    private Double promedioRadiacion;
    private Double maxRadiacion;
    private Double minRadiacion;
    private Double modaRadiacion;

    //Presion
    private Double promedioPresion;
    private Double maxPresion;
    private Double minPresion;
    private Double modaPresion;

}
