package mx.edu.cenidet.estadias.dtos.alertas;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Builder
public class AlertDTO {

    private Long idAlerta;
    private String tipo;
    private String mensaje;
    private LocalDateTime fechaCreacion;
    private boolean esGeneral;

}
