package mx.edu.cenidet.estadias.services.sync;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.edu.cenidet.estadias.config.AmbientWeatherClient;
import mx.edu.cenidet.estadias.config.ThingSpeakClient;
import mx.edu.cenidet.estadias.dtos.client.AmbientWeatherReadingDTO;
import mx.edu.cenidet.estadias.dtos.client.ThingSpeakFeedDTO;
import mx.edu.cenidet.estadias.event.ConexionRecuperadaEvent;
import mx.edu.cenidet.estadias.mappers.AmbientWeatherMapper;
import mx.edu.cenidet.estadias.mappers.ThingSpeakMapper;
import mx.edu.cenidet.estadias.modelos.lectura.BeanLectura;
import mx.edu.cenidet.estadias.modelos.lecturaElectrica.BeanLecturaElectrica;
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

@Service
@RequiredArgsConstructor
@Slf4j
public class GapRecoveryService {

    //Umbral: si la brecha es ≤ 2 min, no hace falta recuperar
    private static final long MINUTOS_UMBRAL = 2;

    //Tope de páginas para Ambient Weather: cada página trae como máximo
    //AmbientWeatherClient.LIMITE_HISTORICO registros (la API solo soporta
    //endDate + limit, no un rango "desde"). 60 páginas ≈ 60 días de histórico
    //a intervalos de 5 min, más que suficiente para cualquier desconexión real.
    private static final int MAX_PAGINAS_AMBIENT = 60;

    //ThingSpeak sí soporta start/end explícitos, así que normalmente una sola
    //llamada cubre toda la brecha. Este tope es una salvaguarda por si la API
    //trunca la respuesta en una desconexión excepcionalmente larga.
    private static final int MAX_PAGINAS_THINGSPEAK = 20;
    private static final int LIMITE_SOSPECHA_TRUNCAMIENTO_TS = 8000;

    private final LecturaRepository lecturaRepository;
    private final LecturaElectricaRepository electricaRepository;
    private final AmbientWeatherClient ambientWeatherClient;
    private final ThingSpeakClient thingSpeakClient;
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
            long totalGuardados = 0;
            //La API de Ambient Weather solo acepta "endDate" + "limit" (no un
            //"desde" real), así que para brechas largas hay que paginar hacia
            //atrás: cada vuelta pide el tramo que termina justo antes del más
            //antiguo de la página anterior, hasta cubrir toda la brecha.
            LocalDateTime cursor = hasta;
            int pagina = 0;

            while (cursor.isAfter(desde) && pagina < MAX_PAGINAS_AMBIENT) {
                List<AmbientWeatherReadingDTO> historico =
                        ambientWeatherClient.obtenerHistoricoPorRango(desde, cursor);

                if (historico.isEmpty()) {
                    break;
                }

                List<BeanLectura> lecturas = historico.stream()
                        .map(ambientWeatherMapper::toEntity)
                        .sorted(Comparator.comparing(BeanLectura::getFechaLectura))
                        .toList();

                totalGuardados += lecturas.stream()
                        .filter(l -> !lecturaRepository.existsByFechaLectura(l.getFechaLectura()))
                        .map(lecturaRepository::save)
                        .count();

                LocalDateTime masAntigua = lecturas.get(0).getFechaLectura();
                pagina++;

                //Sin avance o ya cubrimos el inicio de la brecha -> terminar
                if (!masAntigua.isBefore(cursor) || !masAntigua.isAfter(desde)) {
                    break;
                }
                //La API devuelve como máximo LIMITE_HISTORICO registros; si trajo
                //menos, ya no hay más historia disponible antes de este punto
                if (historico.size() < AmbientWeatherClient.LIMITE_HISTORICO) {
                    break;
                }
                cursor = masAntigua.minusSeconds(1);
            }

            log.info("[GAP-RECOVERY] Climático: {} lecturas recuperadas en {} página(s).",
                    totalGuardados, pagina);
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
            long totalGuardados = 0;
            //ThingSpeak sí soporta start/end, así que normalmente esto se resuelve
            //en una sola página. Si la respuesta luce truncada (tope por defecto
            //de la API), se continúa desde el último dato recibido hacia "hasta".
            LocalDateTime cursorDesde = desde;
            int pagina = 0;

            while (cursorDesde.isBefore(hasta) && pagina < MAX_PAGINAS_THINGSPEAK) {
                List<ThingSpeakFeedDTO.Feed> feeds =
                        thingSpeakClient.obtenerFeedsPorRango(cursorDesde, hasta);

                if (feeds.isEmpty()) {
                    break;
                }

                List<BeanLecturaElectrica> lecturas = feeds.stream()
                        .map(thingSpeakMapper::toEntity)
                        .sorted(Comparator.comparing(BeanLecturaElectrica::getFechaLectura))
                        .toList();

                totalGuardados += lecturas.stream()
                        .filter(l -> !electricaRepository.existsByFechaLectura(l.getFechaLectura()))
                        .map(electricaRepository::save)
                        .count();
                pagina++;

                if (feeds.size() < LIMITE_SOSPECHA_TRUNCAMIENTO_TS) {
                    break; // no hay indicio de truncamiento, ya se cubrió la brecha completa
                }

                LocalDateTime masReciente = lecturas.get(lecturas.size() - 1).getFechaLectura();
                if (!masReciente.isAfter(cursorDesde)) {
                    break; // sin avance, evitar bucle infinito
                }
                cursorDesde = masReciente.plusSeconds(1);
            }

            log.info("[GAP-RECOVERY] Eléctrico: {} lecturas recuperadas en {} página(s).",
                    totalGuardados, pagina);
        } catch (Exception ex) {
            log.error("[GAP-RECOVERY] Error recuperando brechas eléctricas: {}", ex.getMessage());
        }
    }
}
