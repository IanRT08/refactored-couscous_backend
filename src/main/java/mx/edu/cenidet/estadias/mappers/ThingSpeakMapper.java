package mx.edu.cenidet.estadias.mappers;

import mx.edu.cenidet.estadias.dtos.client.ThingSpeakFeedDTO;
import mx.edu.cenidet.estadias.modelos.lecturaElectrica.BeanLecturaElectrica;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class ThingSpeakMapper {

    private static final ZoneId ZONA_CENIDET = ZoneId.of("America/Mexico_City");

    public BeanLecturaElectrica toEntity(ThingSpeakFeedDTO.Feed feed) {
        return BeanLecturaElectrica.builder()
                .fechaLectura(parseFecha(feed.getCreatedAt()))
                .corriente(parseFloat(feed.getField1()))  // A
                .voltaje(parseFloat(feed.getField2()))    // V
                .potencia(parseFloat(feed.getField3()))   // W
                .energia(parseFloat(feed.getField4()))    // kWh
                .build();
    }

    private LocalDateTime parseFecha(String dateStr) {
        return ZonedDateTime.parse(dateStr, DateTimeFormatter.ISO_DATE_TIME)
                .withZoneSameInstant(ZONA_CENIDET)
                .toLocalDateTime();
    }

    private Float parseFloat(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Float.parseFloat(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
