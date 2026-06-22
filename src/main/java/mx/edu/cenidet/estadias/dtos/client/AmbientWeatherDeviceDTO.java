package mx.edu.cenidet.estadias.dtos.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AmbientWeatherDeviceDTO {

    @JsonProperty("macAddress")
    private String macAddress;

    //El objeto "lastData" contiene todas las variables climáticas.
    //Se mapea directamente al DTO que ya existe y que el Mapper convierte a Lectura.
    @JsonProperty("lastData")
    private AmbientWeatherReadingDTO lastData;

}
