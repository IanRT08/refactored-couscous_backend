package mx.edu.cenidet.estadias.services.sync;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import mx.edu.cenidet.estadias.config.ThingSpeakClient;
import mx.edu.cenidet.estadias.dtos.client.ThingSpeakFeedDTO;
import mx.edu.cenidet.estadias.mappers.ThingSpeakMapper;
import mx.edu.cenidet.estadias.modelos.lecturaElectrica.BeanLecturaElectrica;
import mx.edu.cenidet.estadias.repositorios.lecturaElectrica.LecturaElectricaRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ThingSpeakSyncService {

    private final ThingSpeakClient thingSpeakClient;
    private final LecturaElectricaRepository electricaRepository;
    private final ThingSpeakMapper thingSpeakMapper;
    private final SyncHealthService          syncHealthService;

    // application.yml: sync.thingspeak.interval-ms: 30000
    @Scheduled(fixedDelayString = "${sync.thingspeak.interval-ms:30000}")
    @Transactional
    public void sincronizar() {
        try {
            //Obtener el último feed del canal
            ThingSpeakFeedDTO.Feed feed = thingSpeakClient.obtenerUltimoFeed();

            //Mapear a entidad
            BeanLecturaElectrica lectura = thingSpeakMapper.toEntity(feed);

            //Idempotencia
            if (electricaRepository.existsByFechaLectura(lectura.getFechaLectura())) {
                log.debug("[SYNC-TS] Lectura {} ya existe. Omitiendo.",
                        lectura.getFechaLectura());
                syncHealthService.marcarExito(SyncHealthService.FUENTE_THINGSPEAK);
                return;
            }

            //Persistir
            electricaRepository.save(lectura);
            log.info("[SYNC-TS] Lectura eléctrica guardada: {}", lectura.getFechaLectura());
            syncHealthService.marcarExito(SyncHealthService.FUENTE_THINGSPEAK);

        } catch (Exception ex) {
            syncHealthService.registrarFallo(SyncHealthService.FUENTE_THINGSPEAK, ex);
        }
    }
}