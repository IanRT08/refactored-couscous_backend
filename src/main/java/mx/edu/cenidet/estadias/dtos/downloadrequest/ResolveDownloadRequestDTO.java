package mx.edu.cenidet.estadias.dtos.downloadrequest;

import lombok.*;
import jakarta.validation.constraints.*;
import mx.edu.cenidet.estadias.modelos.solicitudDescarga.EstadoSolicitud;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ResolveDownloadRequestDTO {

    @NotNull(message = "El ID de la solicitud es obligatorio")
    private Long idSolicitudDescarga;

    @NotNull(message = "La desicion es obligatoria")
    private EstadoSolicitud desicion;

    //Comentario opcional para el historial de acciones
    private String comentario;

}
