package mx.edu.cenidet.estadias.dtos.telemetria;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Builder
public class ClimateReadingDTO {

    private Long   idLectura;
    private LocalDateTime fechaLectura;
    private Float  temperatura;
    private Float  viento;
    private Float  humedad;
    private Float  radiacion;
    private Float  presion;
    private Float  rainRate;
    private Float  dailyRain;
    private Float  rainTotal;

}
