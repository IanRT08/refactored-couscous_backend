package mx.edu.cenidet.estadias.dtos.comunes;

import lombok.*;


import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
public class ApiResponseDTO<T> {

    private boolean exito;
    private String mensaje;
    private T datos;
    private LocalDateTime timestamp;

   //Respuesta exitosa con datos
    public static <T> ApiResponseDTO<T> ok(String mensaje, T datos) {
        return new ApiResponseDTO<>(true, mensaje, datos, LocalDateTime.now());
    }

    //Respuesta exitosa sin datos
    public static <T> ApiResponseDTO<T> ok(String mensaje) {
        return new ApiResponseDTO<>(true, mensaje, null, LocalDateTime.now());
    }

    //Estandariza errores
    public static <T> ApiResponseDTO<T> error(String mensaje) {
        return new ApiResponseDTO<>(false, mensaje, null, LocalDateTime.now());
    }

}
