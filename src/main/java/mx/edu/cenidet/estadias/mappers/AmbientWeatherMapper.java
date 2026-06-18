package mx.edu.cenidet.estadias.mappers;

import mx.edu.cenidet.estadias.dtos.client.AmbientWeatherReadingDTO;
import mx.edu.cenidet.estadias.modelos.lectura.BeanLectura;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

// Convierte AmbientWeatherReadingDTO (unidades imperiales/API) → Lectura (SI).
// Conversiones aplicadas:
//   Temperatura: °F → °C    t_C = (t_F - 32) × 5/9
//   Viento:      mph → m/s  v = mph × 0.44704
//   Presión:     inHg → hPa p = inHg × 33.8639
//   Lluvia:      in → mm    r = in × 25.4
@Component
public class AmbientWeatherMapper {

    private static final ZoneId ZONA_CENIDET = ZoneId.of("America/Mexico_City");

    public BeanLectura toEntity(AmbientWeatherReadingDTO dto) {
        return BeanLectura.builder()
                .fechaLectura(parseFecha(dto.getDate()))
                .temperatura(fahrenheitToCelsius(dto.getTempf()))
                .viento(mphToMs(dto.getWindspeedmph()))
                .humedad(dto.getHumidity() != null ? dto.getHumidity().floatValue() : null)
                .radiacion(dto.getSolarradiation())
                .presion(inHgToHpa(dto.getBaromrelin()))
                .rainRate(inToMm(dto.getRainratein()))
                .dailyRain(inToMm(dto.getDailyrainin()))
                .rainTotal(inToMm(dto.getTotalrainin()))
                .build();
    }

    private LocalDateTime parseFecha(String dateStr) {
        return ZonedDateTime.parse(dateStr, DateTimeFormatter.ISO_DATE_TIME)
                .withZoneSameInstant(ZONA_CENIDET)
                .toLocalDateTime();
    }

    private Float fahrenheitToCelsius(Float f) {
        return f != null ? (f - 32f) * 5f / 9f : null;
    }

    private Float mphToMs(Float mph) {
        return mph != null ? mph * 0.44704f : null;
    }

    private Float inHgToHpa(Float inHg) {
        return inHg != null ? inHg * 33.8639f : null;
    }

    private Float inToMm(Float in) {
        return in != null ? in * 25.4f : null;
    }
}

