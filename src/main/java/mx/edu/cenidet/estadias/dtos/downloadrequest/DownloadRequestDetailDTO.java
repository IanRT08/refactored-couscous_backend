package mx.edu.cenidet.estadias.dtos.downloadrequest;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Builder
public class DownloadRequestDetailDTO {

    private Long idSolicitudDescarga;
    private String estado;
    private String motivo;
    private LocalDateTime fechaSolicitud;

    //Datos del que hizo la solicitud
    private Long   idUsuario;
    private String nombreUsuario;
    private String correo;
    private String nombreCompleto;

    //Estado actual de la solicitud
    private String estadoPermiso;

}
