package mx.edu.cenidet.estadias.dtos.administrador;

import lombok.*;

@Getter
@Builder
public class AdminSummaryDTO {

    private Long idAdministrador;
    private Long idUsuario;
    private String nombreUsuario;
    private String correo;
    private String nombreCompleto;
    private String tipoAdministrador;
    private String estadoUsuario;

}
