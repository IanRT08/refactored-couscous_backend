package mx.edu.cenidet.estadias.dtos.administrador;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Builder
public class ActionHistorySummaryDTO {

    private Long idAccion;
    private String tipoAccion;
    private String descripcion;
    private LocalDateTime fechaAccion;
    private Long idUsuario;
    private String nombreUsuario;

}
