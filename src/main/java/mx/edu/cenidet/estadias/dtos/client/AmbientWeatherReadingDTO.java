package mx.edu.cenidet.estadias.dtos.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AmbientWeatherReadingDTO {

    //Para guardar la fecha exacta de la lectura
    @JsonProperty("date")
    private String date;

    //Convierte °F en °C
    @JsonProperty("tempf")
    private Float tempf;

    //Convierte la velocidad del viento en m/s
    @JsonProperty("windspeedmph")
    private Float windspeedmph;

    //Humedad relativa
    @JsonProperty("humidity")
    private Integer humidity;

    //Irradiencia solar
    @JsonProperty("solarradiation")
    private Float solarradiation;

    //Convierte la presión en inHg a hPa
    @JsonProperty("baromrelin")
    private Float baromrelin;

    //Tasa de lluvia en in/h a mm/h
    @JsonProperty("rainratein")
    private Float rainratein;

    //Lluvia diaria acumulada en in a mm
    @JsonProperty("dailyrainin")
    private Float dailyrainin;

    //Lluvia total acumulada en in a mm
    @JsonProperty("totalrainin")
    private Float totalrainin;

}
