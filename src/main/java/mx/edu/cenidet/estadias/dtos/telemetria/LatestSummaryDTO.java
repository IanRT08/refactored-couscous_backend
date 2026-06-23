package mx.edu.cenidet.estadias.dtos.telemetria;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Builder
public class LatestSummaryDTO {

    private ClimateReadingDTO ultimaLecturaClimatica;
    private ElectricReadingDTO ultimaLecturaFotovoltaica;
    private ElectricReadingDTO ultimaLecturaEolica;

    //True significa que tiene los datos sincronizados y hay conexión a internet
    //False significa que opera con los ultimos datos guardados (modo offline)
    private boolean datosEnTiempoReal;

    //Momento en el que se ensamblo este resumen
    private LocalDateTime timestampConsulta;

}
