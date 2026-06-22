package mx.edu.cenidet.estadias.dtos.administrador;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import mx.edu.cenidet.estadias.modelos.permisosDescarga.EstadoPermiso;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateDownloadPermissionRequestDTO {

    @NotNull(message = "El nuevo permiso de descarga es obligatorio")
    private EstadoPermiso nuevoPermiso;

    //Opcional, queda en el historial de acciones
    private String motivo;

}
