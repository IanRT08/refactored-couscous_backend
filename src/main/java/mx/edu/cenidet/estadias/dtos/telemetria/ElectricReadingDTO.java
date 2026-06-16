package mx.edu.cenidet.estadias.dtos.telemetria;

import lombok.*;

import java.time.LocalDateTime;

public class ElectricReadingDTO {

    private Long idLecturaElectrica;
    private LocalDateTime fechaLectura;
    private Float corriente;
    private Float voltaje;
    private Float potencia;
    private Float energia;

}
