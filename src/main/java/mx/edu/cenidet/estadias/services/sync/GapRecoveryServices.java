package mx.edu.cenidet.estadias.services.sync;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.edu.cenidet.estadias.dtos.client.AmbientWeatherReadingDTO;
import mx.edu.cenidet.estadias.dtos.client.ThingSpeakFeedDTO;
import mx.edu.cenidet.estadias.event.ConexionRecuperadaEvent;
import mx.edu.cenidet.estadias.mappers.AmbientWeatherMapper;
import mx.edu.cenidet.estadias.mappers.ThingSpeakMapper;
import mx.edu.cenidet.estadias.repositorios.lectura.LecturaRepository;
import mx.edu.cenidet.estadias.repositorios.lecturaElectrica.LecturaElectricaRepository;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

// Módulo 3 — Recuperación de brechas temporales.
// Se activa en dos escenarios:
//   1. Al arrancar la aplicación (@ApplicationReadyEvent)
//   2. Al recuperar conectividad (ConexionRecuperadaEvent de SyncHealthService)
// Busca el último timestamp en la BD y pide a las APIs externas
// todos los registros del intervalo perdido, insertándolos de forma
// secuencial e idempotente (sin duplicar).
@Service
@RequiredArgsConstructor
@Slf4j
public class GapRecoveryService {

    // Umbral: si la brecha es ≤ 2 min, no hace falta recuperar
    private static final long MINUTOS_UMBRAL = 2;

    private final LecturaRepository lecturaRepository;
    private final LecturaElectricaRepository electricaRepository;
    private final AmbientWeatherClient       ambientWeatherClient;
    private final ThingSpeakClient           thingSpeakClient;
    private final AmbientWeatherMapper ambientWeatherMapper;
    private final ThingSpeakMapper thingSpeakMapper;

    // ── Disparadores de evento ────────────────────────────────

    @EventListener(ApplicationReadyEvent.class)
    public void alIniciar() {
        log.info("[GAP-RECOVERY] Auditoría inicial de brechas al arrancar...");
        recuperarBrechasClimaticas();
        recuperarBrechasElectricas();
    }

    @EventListener(ConexionRecuperadaEvent.class)
    public void alRecuperarConexion(ConexionRecuperadaEvent evento) {
        log.info("[GAP-RECOVERY] Disparado por ConexionRecuperadaEvent: {}", evento.getFuente());
        switch (evento.getFuente()) {
            case SyncHealthService.FUENTE_AMBIENT    -> recuperarBrechasClimaticas();
            case SyncHealthService.FUENTE_THINGSPEAK -> recuperarBrechasElectricas();
        }
    }

    // ── Recuperación climática (Ambient Weather) ──────────────
    @Transactional
    public void recuperarBrechasClimaticas() {
        Optional<LocalDateTime> ultima = lecturaRepository.findMaxFechaLectura();
        LocalDateTime desde = ultima.orElse(LocalDateTime.now().minusDays(1));
        LocalDateTime hasta = LocalDateTime.now();

        if (Duration.between(desde, hasta).toMinutes() <= MINUTOS_UMBRAL) {
            log.debug("[GAP-RECOVERY] Sin brecha climática significativa.");
            return;
        }

        log.info("[GAP-RECOVERY] Brecha climática: {} → {}", desde, hasta);
        try {
            List<AmbientWeatherReadingDTO> historico =
                    ambientWeatherClient.obtenerHistoricoPorRango(desde, hasta);

            long guardados = historico.stream()
                    .sorted(Comparator.comparing(AmbientWeatherReadingDTO::getDate))
                    .map(ambientWeatherMapper::toEntity)
                    .filter(l -> !lecturaRepository.existsByFechaLectura(l.getFechaLectura()))
                    .map(lecturaRepository::save)
                    .count();

            log.info("[GAP-RECOVERY] Climático: {} lecturas recuperadas.", guardados);
        } catch (Exception ex) {
            log.error("[GAP-RECOVERY] Error recuperando brechas climáticas: {}", ex.getMessage());
        }
    }

    // ── Recuperación eléctrica (ThingSpeak) ──────────────────
    @Transactional
    public void recuperarBrechasElectricas() {
        Optional<LocalDateTime> ultima = electricaRepository.findMaxFechaLectura();
        LocalDateTime desde = ultima.orElse(LocalDateTime.now().minusDays(1));
        LocalDateTime hasta = LocalDateTime.now();

        if (Duration.between(desde, hasta).toMinutes() <= MINUTOS_UMBRAL) {
            log.debug("[GAP-RECOVERY] Sin brecha eléctrica significativa.");
            return;
        }

        log.info("[GAP-RECOVERY] Brecha eléctrica: {} → {}", desde, hasta);
        try {
            List<ThingSpeakFeedDTO.Feed> feeds =
                    thingSpeakClient.obtenerFeedsPorRango(desde, hasta);

            long guardados = feeds.stream()
                    .sorted(Comparator.comparing(ThingSpeakFeedDTO.Feed::getCreatedAt))
                    .map(thingSpeakMapper::toEntity)
                    .filter(l -> !electricaRepository.existsByFechaLectura(l.getFechaLectura()))
                    .map(electricaRepository::save)
                    .count();

            log.info("[GAP-RECOVERY] Eléctrico: {} lecturas recuperadas.", guardados);
        } catch (Exception ex) {
            log.error("[GAP-RECOVERY] Error recuperando brechas eléctricas: {}", ex.getMessage());
        }
    }
}
