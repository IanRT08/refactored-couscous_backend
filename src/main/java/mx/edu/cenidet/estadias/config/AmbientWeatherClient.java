package mx.edu.cenidet.estadias.config;

import lombok.extern.slf4j.Slf4j;
import mx.edu.cenidet.estadias.dtos.client.AmbientWeatherDeviceDTO;
import mx.edu.cenidet.estadias.dtos.client.AmbientWeatherReadingDTO;
import mx.edu.cenidet.estadias.excepciones.ExternalApiException;
import mx.edu.cenidet.estadias.services.sync.SyncHealthService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Component
@Slf4j
public class AmbientWeatherClient {

    private static final ZoneId ZONA_CENIDET = ZoneId.of("America/Mexico_City");

    //Límite máximo de registros que la API de Ambient Weather devuelve por petición
    public static final int LIMITE_HISTORICO = 288;

    private final RestClient restClient;

    @Value("${awshef.ambientweather.api-key}")
    private String apiKey;

    @Value("${awshef.ambientweather.application-key}")
    private String applicationKey;

    @Value("${awshef.ambientweather.mac-address}")
    private String macAddress;

    public AmbientWeatherClient(
            @Qualifier("ambientWeatherRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    //Última lectura disponible
    public AmbientWeatherReadingDTO obtenerUltimaLectura() {
        try {
            AmbientWeatherDeviceDTO[] dispositivos = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/devices")
                            .queryParam("applicationKey", applicationKey)
                            .queryParam("apiKey", apiKey)
                            .build())
                    .retrieve()
                    .body(AmbientWeatherDeviceDTO[].class);

            if (dispositivos == null || dispositivos.length == 0) {
                throw new ExternalApiException(SyncHealthService.FUENTE_AMBIENT,
                        "La API no devolvió dispositivos. Verifica applicationKey y apiKey.");
            }

            //La cuenta puede tener más de un dispositivo: filtrar por la MAC
            //de la estación del CENIDET en vez de asumir que es la primera.
            AmbientWeatherDeviceDTO dispositivo = Arrays.stream(dispositivos)
                    .filter(d -> macAddress.equalsIgnoreCase(d.getMacAddress()))
                    .findFirst()
                    .orElseThrow(() -> new ExternalApiException(SyncHealthService.FUENTE_AMBIENT,
                            "No se encontró ningún dispositivo con MAC " + macAddress
                                    + " en la cuenta de Ambient Weather."));

            AmbientWeatherReadingDTO lastData = dispositivo.getLastData();
            if (lastData == null || lastData.getDate() == null) {
                throw new ExternalApiException(SyncHealthService.FUENTE_AMBIENT,
                        "La estación del CENIDET no tiene datos recientes.");
            }

            log.debug("[AW-CLIENT] Lectura obtenida: {}", lastData.getDate());
            return lastData;

        } catch (RestClientException ex) {
            throw new ExternalApiException(SyncHealthService.FUENTE_AMBIENT,
                    "Error HTTP al consultar Ambient Weather: " + ex.getMessage(), ex);
        }
    }

    //Sondeo rápido: devuelve true si existe al menos un registro anterior a "hasta".
    //Usa limit=1 para minimizar datos transferidos; pensado para la búsqueda binaria
    //de la fecha más antigua disponible en la API.
    public boolean existenLecturasHasta(LocalDateTime hasta) {
        try {
            long endDateMs = hasta.atZone(ZONA_CENIDET).toInstant().toEpochMilli();
            AmbientWeatherReadingDTO[] lecturas = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/devices/{mac}")
                            .queryParam("applicationKey", applicationKey)
                            .queryParam("apiKey", apiKey)
                            .queryParam("endDate", endDateMs)
                            .queryParam("limit", 1)
                            .build(macAddress))
                    .retrieve()
                    .body(AmbientWeatherReadingDTO[].class);
            return lecturas != null && lecturas.length > 0;
        } catch (RestClientException ex) {
            log.warn("[AW-CLIENT] Sondeo falló en {}: {}", hasta, ex.getMessage());
            return false;
        }
    }

    //Histórico por rango de fechas
    public List<AmbientWeatherReadingDTO> obtenerHistoricoPorRango(
            LocalDateTime desde, LocalDateTime hasta) {
        try {
            // Convertir "hasta" de CDMX a milisegundos UTC para el parámetro endDate
            long endDateMs = hasta
                    .atZone(ZONA_CENIDET)
                    .toInstant()
                    .toEpochMilli();

            AmbientWeatherReadingDTO[] lecturas = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/devices/{mac}")
                            .queryParam("applicationKey", applicationKey)
                            .queryParam("apiKey", apiKey)
                            .queryParam("endDate", endDateMs)
                            .queryParam("limit", LIMITE_HISTORICO)
                            .build(macAddress))
                    .retrieve()
                    .body(AmbientWeatherReadingDTO[].class);

            if (lecturas == null || lecturas.length == 0) {
                log.info("[AW-CLIENT] Sin datos históricos para el rango {} → {}", desde, hasta);
                return Collections.emptyList();
            }
            //Filtrar solo las que caen dentro del rango solicitado.
            log.info("[AW-CLIENT] Histórico recibido: {} registros para rango {} → {}",
                    lecturas.length, desde, hasta);

            return Arrays.asList(lecturas);

        } catch (RestClientException ex) {
            throw new ExternalApiException(SyncHealthService.FUENTE_AMBIENT,
                    "Error HTTP al consultar histórico de Ambient Weather: " + ex.getMessage(), ex);
        }
    }
}