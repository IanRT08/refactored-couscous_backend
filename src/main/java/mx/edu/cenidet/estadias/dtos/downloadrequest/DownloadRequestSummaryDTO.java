package mx.edu.cenidet.estadias.dtos.downloadrequest;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Builder
public class DownloadRequestSummaryDTO {

    private Long idSolicitudDescarga;
    private String motivo;
    private String estado;
    private LocalDateTime fechaSolicitud;

    private String estadoPermiso;

}
