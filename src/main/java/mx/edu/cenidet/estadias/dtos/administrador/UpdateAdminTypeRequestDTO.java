package mx.edu.cenidet.estadias.dtos.administrador;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import mx.edu.cenidet.estadias.modelos.administrador.TipoAdministrador;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateAdminTypeRequestDTO {

    @NotNull
    private TipoAdministrador nuevoTipo;

}
