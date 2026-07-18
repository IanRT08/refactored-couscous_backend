package mx.edu.cenidet.estadias.config;

import lombok.extern.slf4j.Slf4j;
import mx.edu.cenidet.estadias.dtos.client.ThingSpeakFeedDTO;
import mx.edu.cenidet.estadias.excepciones.ExternalApiException;
import mx.edu.cenidet.estadias.modelos.lecturaElectrica.FuenteElectrica;
import mx.edu.cenidet.estadias.services.sync.SyncHealthService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class ThingSpeakClient {

    //ThingSpeak espera las fechas en UTC con este formato
    private static final DateTimeFormatter TS_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final ZoneId ZONA_CENIDET = ZoneId.of("America/Mexico_City");

    private record CanalConfig(String channelId, String readApiKey) {
    }

    private final RestClient restClient;
    private final Map<FuenteElectrica, CanalConfig> canales = new EnumMap<>(FuenteElectrica.class);

    public ThingSpeakClient(
            @Qualifier("thingSpeakRestClient") RestClient restClient,
            @Value("${awshef.thingspeak.fotovoltaico.channel-id}") String channelIdFotovoltaico,
            @Value("${awshef.thingspeak.fotovoltaico.read-api-key}") String readApiKeyFotovoltaico,
            @Value("${awshef.thingspeak.eolico.channel-id}") String channelIdEolico,
            @Value("${awshef.thingspeak.eolico.read-api-key}") String readApiKeyEolico) {
        this.restClient = restClient;
        canales.put(FuenteElectrica.FOTOVOLTAICO, new CanalConfig(channelIdFotovoltaico, readApiKeyFotovoltaico));
        canales.put(FuenteElectrica.EOLICO, new CanalConfig(channelIdEolico, readApiKeyEolico));
    }

    //Último feed disponible de un canal
    public ThingSpeakFeedDTO.Feed obtenerUltimoFeed(FuenteElectrica fuente) {
        CanalConfig canal = canales.get(fuente);
        try {
            ThingSpeakFeedDTO respuesta = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/channels/{id}/feeds.json")
                            .queryParam("api_key", canal.readApiKey())
                            .queryParam("results", 1)
                            .build(canal.channelId()))
                    .retrieve()
                    .body(ThingSpeakFeedDTO.class);

            if (respuesta == null
                    || respuesta.getFeeds() == null
                    || respuesta.getFeeds().isEmpty()) {
                throw new ExternalApiException(SyncHealthService.FUENTE_THINGSPEAK,
                        "ThingSpeak no devolvió feeds para el canal " + fuente + " (" + canal.channelId() + ")");
            }

            ThingSpeakFeedDTO.Feed feed = respuesta.getFeeds()
                    .get(respuesta.getFeeds().size() - 1);

            log.debug("[TS-CLIENT] Feed obtenido ({}): {}", fuente, feed.getCreatedAt());
            return feed;

        } catch (RestClientException ex) {
            throw new ExternalApiException(SyncHealthService.FUENTE_THINGSPEAK,
                    "Error HTTP al consultar ThingSpeak (" + fuente + "): " + ex.getMessage(), ex);
        }
    }

    //Sondeo rápido: devuelve true si existe al menos un feed desde el origen de los
    //tiempos hasta "hasta". Usa results=1 para minimizar datos transferidos.
    public boolean existenFeedsHasta(FuenteElectrica fuente, LocalDateTime hasta) {
        CanalConfig canal = canales.get(fuente);
        try {
            //Punto de inicio arbitrariamente antiguo (ThingSpeak filtra por lo disponible)
            String startUTC = LocalDateTime.of(2000, 1, 1, 0, 0, 0)
                    .atZone(ZONA_CENIDET)
                    .withZoneSameInstant(ZoneId.of("UTC"))
                    .format(TS_DATE_FORMAT);
            String endUTC = hasta
                    .atZone(ZONA_CENIDET)
                    .withZoneSameInstant(ZoneId.of("UTC"))
                    .format(TS_DATE_FORMAT);
            ThingSpeakFeedDTO respuesta = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/channels/{id}/feeds.json")
                            .queryParam("api_key", canal.readApiKey())
                            .queryParam("start", startUTC)
                            .queryParam("end", endUTC)
                            .queryParam("results", 1)
                            .build(canal.channelId()))
                    .retrieve()
                    .body(ThingSpeakFeedDTO.class);
            return respuesta != null
                    && respuesta.getFeeds() != null
                    && !respuesta.getFeeds().isEmpty();
        } catch (RestClientException ex) {
            log.warn("[TS-CLIENT] Sondeo falló ({}) en {}: {}", fuente, hasta, ex.getMessage());
            return false;
        }
    }

    //Feeds por rango de fechas de un canal
    public List<ThingSpeakFeedDTO.Feed> obtenerFeedsPorRango(
            FuenteElectrica fuente, LocalDateTime desde, LocalDateTime hasta) {
        CanalConfig canal = canales.get(fuente);
        try {
            String startUTC = desde
                    .atZone(ZONA_CENIDET)
                    .withZoneSameInstant(ZoneId.of("UTC"))
                    .format(TS_DATE_FORMAT);
            String endUTC = hasta
                    .atZone(ZONA_CENIDET)
                    .withZoneSameInstant(ZoneId.of("UTC"))
                    .format(TS_DATE_FORMAT);
            ThingSpeakFeedDTO respuesta = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/channels/{id}/feeds.json")
                            .queryParam("api_key", canal.readApiKey())
                            .queryParam("start", startUTC)
                            .queryParam("end", endUTC)
                            .build(canal.channelId()))
                    .retrieve()
                    .body(ThingSpeakFeedDTO.class);
            if (respuesta == null || respuesta.getFeeds() == null) {
                log.info("[TS-CLIENT] Sin feeds ({}) para rango {} → {}", fuente, desde, hasta);
                return Collections.emptyList();
            }
            log.info("[TS-CLIENT] Histórico recibido ({}): {} feeds para rango {} → {}",
                    fuente, respuesta.getFeeds().size(), desde, hasta);
            return respuesta.getFeeds();
        } catch (RestClientException ex) {
            throw new ExternalApiException(SyncHealthService.FUENTE_THINGSPEAK,
                    "Error HTTP al consultar histórico de ThingSpeak (" + fuente + "): " + ex.getMessage(), ex);
        }
    }

}
