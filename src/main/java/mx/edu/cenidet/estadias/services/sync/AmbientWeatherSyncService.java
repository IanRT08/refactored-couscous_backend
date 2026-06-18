package mx.edu.cenidet.estadias.services.sync;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.edu.cenidet.estadias.dtos.client.AmbientWeatherReadingDTO;
import mx.edu.cenidet.estadias.mappers.AmbientWeatherMapper;
import mx.edu.cenidet.estadias.modelos.lectura.BeanLectura;
import mx.edu.cenidet.estadias.repositorios.lectura.LecturaRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AmbientWeatherSyncService {

    private final AmbientWeatherClient  ambientWeatherClient;
    private final LecturaRepository lecturaRepository;
    private final AmbientWeatherMapper ambientWeatherMapper;
    private final SyncHealthService     syncHealthService;

    // ── Ciclo de sincronización ───────────────────────────────
    // Intervalo configurable; el mínimo práctico es 60 000 ms (1 req/min).
    // application.yml: sync.ambientweather.interval-ms: 60000
    @Scheduled(fixedDelayString = "${sync.ambientweather.interval-ms:60000}")
    @Transactional
    public void sincronizar() {
        try {
            //Obtener última lectura de la API
            AmbientWeatherReadingDTO dto = ambientWeatherClient.obtenerUltimaLectura();

            //Mapear a entidad (con conversión de unidades)
            BeanLectura lectura = ambientWeatherMapper.toEntity(dto);

            //Idempotencia: no insertar si ya existe ese timestamp
            if (lecturaRepository.existsByFechaLectura(lectura.getFechaLectura())) {
                log.debug("[SYNC-AW] Lectura {} ya existe. Omitiendo.",
                        lectura.getFechaLectura());
                syncHealthService.marcarExito(SyncHealthService.FUENTE_AMBIENT);
                return;
            }

            //Persistir
            lecturaRepository.save(lectura);
            log.info("[SYNC-AW] Lectura climática guardada: {}", lectura.getFechaLectura());
            syncHealthService.marcarExito(SyncHealthService.FUENTE_AMBIENT);

        } catch (Exception ex) {
            syncHealthService.registrarFallo(SyncHealthService.FUENTE_AMBIENT, ex);
        }
    }
}
