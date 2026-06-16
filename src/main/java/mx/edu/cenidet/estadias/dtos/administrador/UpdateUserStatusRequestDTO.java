package mx.edu.cenidet.estadias.dtos.administrador;

import lombok.*;
import jakarta.validation.constraints.*;
import mx.edu.cenidet.estadias.modelos.usuario.EstadoUsuario;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserStatusRequestDTO {

    @NotNull(message = "El nuevo estado es obligatorio")
    private EstadoUsuario nuevoEstado;

    //Opcional que queda el historial de acciones
    private String motivo;

}
