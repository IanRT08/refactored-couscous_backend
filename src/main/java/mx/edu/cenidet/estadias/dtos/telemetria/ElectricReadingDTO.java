package mx.edu.cenidet.estadias.dtos.telemetria;

import lombok.*;
import mx.edu.cenidet.estadias.modelos.lecturaElectrica.FuenteElectrica;

import java.time.LocalDateTime;

@Getter
@Builder
public class ElectricReadingDTO {

    private Long idLecturaElectrica;
    private LocalDateTime fechaLectura;
    private FuenteElectrica fuente;
    private Float voltaje;
    private Float corriente;
    private Float potencia;
    //Solo aplica para fuente = FOTOVOLTAICO; null en EOLICO
    private Float voc;
    private Float energia;

}
