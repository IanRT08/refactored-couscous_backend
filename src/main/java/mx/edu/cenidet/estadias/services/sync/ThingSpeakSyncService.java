package mx.edu.cenidet.estadias.services.sync;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import mx.edu.cenidet.estadias.config.ThingSpeakClient;
import mx.edu.cenidet.estadias.dtos.client.ThingSpeakFeedDTO;
import mx.edu.cenidet.estadias.mappers.ThingSpeakMapper;
import mx.edu.cenidet.estadias.modelos.lecturaElectrica.BeanLecturaElectrica;
import mx.edu.cenidet.estadias.modelos.lecturaElectrica.FuenteElectrica;
import mx.edu.cenidet.estadias.repositorios.lecturaElectrica.LecturaElectricaRepository;
import mx.edu.cenidet.estadias.util.EnergiaCalculator;
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

    @Scheduled(fixedDelayString = "${sync.thingspeak.interval-ms:30000}")
    public void sincronizar() {
        for (FuenteElectrica fuente : FuenteElectrica.values()) {
            sincronizarFuente(fuente);
        }
    }

    @Transactional
    public void sincronizarFuente(FuenteElectrica fuente) {
        try {
            //Obtener el último feed del canal
            ThingSpeakFeedDTO.Feed feed = thingSpeakClient.obtenerUltimoFeed(fuente);

            //Mapear a entidad
            BeanLecturaElectrica lectura = thingSpeakMapper.toEntity(feed, fuente);

            //Idempotencia (por fuente)
            if (electricaRepository.existsByFechaLecturaAndFuente(lectura.getFechaLectura(), fuente)) {
                log.debug("[SYNC-TS] Lectura {} ({}) ya existe. Omitiendo.",
                        lectura.getFechaLectura(), fuente);
                syncHealthService.marcarExito(SyncHealthService.FUENTE_THINGSPEAK);
                return;
            }

            //Energía = potencia integrada desde la lectura anterior de la misma fuente
            electricaRepository.findTopByFuenteOrderByFechaLecturaDesc(fuente)
                    .ifPresent(anterior -> lectura.setEnergia(EnergiaCalculator.calcularIncremento(anterior, lectura)));

            //Persistir
            electricaRepository.save(lectura);
            log.info("[SYNC-TS] Lectura eléctrica guardada ({}): {}", fuente, lectura.getFechaLectura());
            syncHealthService.marcarExito(SyncHealthService.FUENTE_THINGSPEAK);

        } catch (Exception ex) {
            syncHealthService.registrarFallo(SyncHealthService.FUENTE_THINGSPEAK, ex);
        }
    }
}