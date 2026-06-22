package mx.edu.cenidet.estadias.dtos.administrador;

import jakarta.validation.constraints.*;
import lombok.*;
import mx.edu.cenidet.estadias.modelos.administrador.TipoAdministrador;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateAdminRequestDTO {

    @NotBlank(message = "El nombre de usuario es obligatorio")
    @Size(min = 3, max = 60)
    @Pattern(regexp  = "^[a-zA-Z0-9._-]+$", message = "Solo se permiten letras, números, puntos, guiones y guiones bajos")
    private String nombreUsuario;

    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "El formato de correo no es válido")
    @Size(max = 60)
    private String correo;

    @NotBlank(message = "El nombre completo es obligatorio")
    @Size(max = 90)
    private String nombreCompleto;

    @NotNull(message = "El tipo de administrador es obligatorio")
    private TipoAdministrador tipoAdministrador;

    //RN 2.7: el superadmin debe confirmar la acción con su propia contraseña
    @NotBlank(message = "Debes confirmar la acción con tu contraseña")
    private String contraseniaConfirmacion;

}
