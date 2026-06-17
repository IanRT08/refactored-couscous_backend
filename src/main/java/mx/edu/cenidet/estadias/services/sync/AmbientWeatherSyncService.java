package mx.edu.cenidet.estadias.services.sync;

package mx.edu.cenidet.estadias.service.sync;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.edu.cenidet.estadias.dtos.client.AmbientWeatherReadingDTO;
import mx.edu.cenidet.estadias.modelos.lectura.BeanLectura;
import mx.edu.cenidet.estadias.repositorios.lectura.LecturaRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Módulo 3.1 — Sincronización periódica con Ambient Weather API.
// Rate limit de la API: 1 req/s por apiKey.
// El intervalo de fixedDelay (60 s por defecto) garantiza que
// NUNCA se supere ese límite — y da datos con granularidad de 1 minuto.
// fixedDelay (no fixedRate) asegura que el siguiente ciclo empieza
// DESPUÉS de que el anterior termine, evitando solapamientos.
@Service
@RequiredArgsConstructor
@Slf4j
public class AmbientWeatherSyncService {

    private final AmbientWeatherClient  ambientWeatherClient;
    private final LecturaRepository lecturaRepository;
    private final AmbientWeatherMapper  ambientWeatherMapper;
    private final SyncHealthService     syncHealthService;

    // ── Ciclo de sincronización ───────────────────────────────
    // Intervalo configurable; el mínimo práctico es 60 000 ms (1 req/min).
    // application.yml: sync.ambientweather.interval-ms: 60000
    @Scheduled(fixedDelayString = "${sync.ambientweather.interval-ms:60000}")
    @Transactional
    public void sincronizar() {
        try {
            // 1. Obtener última lectura de la API
            AmbientWeatherReadingDTO dto = ambientWeatherClient.obtenerUltimaLectura();

            // 2. Mapear a entidad (con conversión de unidades)
            BeanLectura lectura = ambientWeatherMapper.toEntity(dto);

            // 3. Idempotencia: no insertar si ya existe ese timestamp
            // (Módulo 3.2 RN: "No se deben insertar lecturas duplicadas")
            if (lecturaRepository.existsByFechaLectura(lectura.getFechaLectura())) {
                log.debug("[SYNC-AW] Lectura {} ya existe. Omitiendo.",
                        lectura.getFechaLectura());
                syncHealthService.marcarExito(SyncHealthService.FUENTE_AMBIENT);
                return;
            }

            // 4. Persistir
            lecturaRepository.save(lectura);
            log.info("[SYNC-AW] Lectura climática guardada: {}", lectura.getFechaLectura());
            syncHealthService.marcarExito(SyncHealthService.FUENTE_AMBIENT);

        } catch (Exception ex) {
            syncHealthService.registrarFallo(SyncHealthService.FUENTE_AMBIENT, ex);
        }
    }
}
