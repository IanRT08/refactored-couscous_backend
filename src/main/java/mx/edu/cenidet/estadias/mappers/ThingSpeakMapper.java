package mx.edu.cenidet.estadias.mappers;

import mx.edu.cenidet.estadias.dtos.client.ThingSpeakFeedDTO;
import mx.edu.cenidet.estadias.modelos.lecturaElectrica.BeanLecturaElectrica;
import mx.edu.cenidet.estadias.modelos.lecturaElectrica.FuenteElectrica;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class ThingSpeakMapper {

    private static final ZoneId ZONA_CENIDET = ZoneId.of("America/Mexico_City");

    //Cada canal de ThingSpeak usa sus field1..field4 para variables distintas.
    //energia no la reporta ningún canal: se calcula aparte (EnergiaCalculator)
    //una vez que se conoce la lectura anterior de la misma fuente.
    public BeanLecturaElectrica toEntity(ThingSpeakFeedDTO.Feed feed, FuenteElectrica fuente) {
        BeanLecturaElectrica.BeanLecturaElectricaBuilder lectura = BeanLecturaElectrica.builder()
                .fechaLectura(parseFecha(feed.getCreatedAt()))
                .fuente(fuente);

        if (fuente == FuenteElectrica.FOTOVOLTAICO) {
            lectura.voc(parseFloat(feed.getField1()))        // V (circuito abierto)
                    .voltaje(parseFloat(feed.getField2()))    // V (bajo carga)
                    .corriente(parseFloat(feed.getField3())) // A (bajo carga)
                    .potencia(parseFloat(feed.getField4())); // W
        } else {
            lectura.voltaje(parseFloat(feed.getField1()))     // V
                    .corriente(parseFloat(feed.getField2())) // A
                    .potencia(parseFloat(feed.getField3())); // W
        }

        return lectura.build();
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
