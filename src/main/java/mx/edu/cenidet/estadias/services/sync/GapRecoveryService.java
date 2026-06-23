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
import mx.edu.cenidet.estadias.modelos.lecturaElectrica.FuenteElectrica;
import mx.edu.cenidet.estadias.repositorios.lectura.LecturaRepository;
import mx.edu.cenidet.estadias.repositorios.lecturaElectrica.LecturaElectricaRepository;
import mx.edu.cenidet.estadias.util.EnergiaCalculator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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

    //Si nunca hay un registro previo (primer arranque), cuántas horas hacia
    //atrás se intenta respaldar. Configurable para no saturar la app ni
    //agotar las llamadas del plan gratuito de las APIs en la primera corrida.
    @Value("${gap-recovery.horas-inicial:24}")
    private long horasInicial;

    //Pausa entre páginas/llamadas paginadas a la misma API externa, para
    //respetar los límites de frecuencia de los planes gratuitos.
    @Value("${gap-recovery.delay-ms-entre-paginas:1500}")
    private long delayMsEntrePaginas;

    private final LecturaRepository lecturaRepository;
    private final LecturaElectricaRepository electricaRepository;
    private final AmbientWeatherClient ambientWeatherClient;
    private final ThingSpeakClient thingSpeakClient;
    private final AmbientWeatherMapper ambientWeatherMapper;
    private final ThingSpeakMapper thingSpeakMapper;

    private void pausar() {
        try {
            Thread.sleep(delayMsEntrePaginas);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    //Disparadores de evento

    @EventListener(ApplicationReadyEvent.class)
    public void alIniciar() {
        log.info("[GAP-RECOVERY] Auditoría inicial de brechas al arrancar...");
        recuperarBrechasClimaticas();
        pausar(); // espaciar la recuperación climática de la eléctrica
        recuperarBrechasElectricasTodasLasFuentes();
    }

    @EventListener(ConexionRecuperadaEvent.class)
    public void alRecuperarConexion(ConexionRecuperadaEvent evento) {
        log.info("[GAP-RECOVERY] Disparado por ConexionRecuperadaEvent: {}", evento.getFuente());
        switch (evento.getFuente()) {
            case SyncHealthService.FUENTE_AMBIENT    -> recuperarBrechasClimaticas();
            case SyncHealthService.FUENTE_THINGSPEAK -> recuperarBrechasElectricasTodasLasFuentes();
        }
    }

    private void recuperarBrechasElectricasTodasLasFuentes() {
        boolean primera = true;
        for (FuenteElectrica fuente : FuenteElectrica.values()) {
            if (!primera) {
                pausar(); // espaciar la recuperación de un canal y el otro
            }
            recuperarBrechasElectricas(fuente);
            primera = false;
        }
    }

    // ── Recuperación climática (Ambient Weather) ──────────────
    @Transactional
    public void recuperarBrechasClimaticas() {
        Optional<LocalDateTime> ultima = lecturaRepository.findMaxFechaLectura();
        LocalDateTime desde = ultima.orElse(LocalDateTime.now().minusHours(horasInicial));
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
                if (pagina > 0) {
                    pausar(); // no martillar la API entre páginas sucesivas
                }

                List<AmbientWeatherReadingDTO> historico =
                        ambientWeatherClient.obtenerHistoricoPorRango(desde, cursor);

                if (historico.isEmpty()) {
                    break;
                }

                List<BeanLectura> lecturas = historico.stream()
                        .map(ambientWeatherMapper::toEntity)
                        .sorted(Comparator.comparing(BeanLectura::getFechaLectura))
                        .toList();

                //Una sola consulta para saber qué fechas de esta página ya
                //existen, en vez de un existsBy... por cada registro.
                Set<LocalDateTime> existentes = new HashSet<>(lecturaRepository.findFechasByFechaLecturaBetween(
                        lecturas.get(0).getFechaLectura(),
                        lecturas.get(lecturas.size() - 1).getFechaLectura()));
                List<BeanLectura> nuevas = lecturas.stream()
                        .filter(l -> !existentes.contains(l.getFechaLectura()))
                        .toList();
                lecturaRepository.saveAll(nuevas);
                totalGuardados += nuevas.size();

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

    // ── Recuperación eléctrica (ThingSpeak, una fuente a la vez) ──
    @Transactional
    public void recuperarBrechasElectricas(FuenteElectrica fuente) {
        Optional<BeanLecturaElectrica> ultimaLectura =
                electricaRepository.findTopByFuenteOrderByFechaLecturaDesc(fuente);
        LocalDateTime desde = ultimaLectura.map(BeanLecturaElectrica::getFechaLectura)
                .orElse(LocalDateTime.now().minusHours(horasInicial));
        LocalDateTime hasta = LocalDateTime.now();

        if (Duration.between(desde, hasta).toMinutes() <= MINUTOS_UMBRAL) {
            log.debug("[GAP-RECOVERY] Sin brecha eléctrica significativa ({}).", fuente);
            return;
        }

        log.info("[GAP-RECOVERY] Brecha eléctrica ({}): {} → {}", fuente, desde, hasta);
        try {
            long totalGuardados = 0;
            //ThingSpeak sí soporta start/end, así que normalmente esto se resuelve
            //en una sola página. Si la respuesta luce truncada (tope por defecto
            //de la API), se continúa desde el último dato recibido hacia "hasta".
            LocalDateTime cursorDesde = desde;
            int pagina = 0;
            //Se recuerda la lectura previa (de la misma fuente) para integrar la
            //energía incremental a medida que se recorre la brecha en orden.
            BeanLecturaElectrica anterior = ultimaLectura.orElse(null);

            while (cursorDesde.isBefore(hasta) && pagina < MAX_PAGINAS_THINGSPEAK) {
                if (pagina > 0) {
                    pausar(); // no martillar la API entre páginas sucesivas
                }

                List<ThingSpeakFeedDTO.Feed> feeds =
                        thingSpeakClient.obtenerFeedsPorRango(fuente, cursorDesde, hasta);

                if (feeds.isEmpty()) {
                    break;
                }

                List<BeanLecturaElectrica> lecturas = feeds.stream()
                        .map(feed -> thingSpeakMapper.toEntity(feed, fuente))
                        .sorted(Comparator.comparing(BeanLecturaElectrica::getFechaLectura))
                        .toList();

                //Una sola consulta para saber qué fechas de esta página ya
                //existen, en vez de un existsBy... por cada registro.
                Set<LocalDateTime> existentes = new HashSet<>(electricaRepository.findFechasByFuenteAndFechaLecturaBetween(
                        fuente,
                        lecturas.get(0).getFechaLectura(),
                        lecturas.get(lecturas.size() - 1).getFechaLectura()));

                List<BeanLecturaElectrica> nuevas = new ArrayList<>();
                for (BeanLecturaElectrica lectura : lecturas) {
                    if (existentes.contains(lectura.getFechaLectura())) {
                        anterior = lectura;
                        continue;
                    }
                    lectura.setEnergia(EnergiaCalculator.calcularIncremento(anterior, lectura));
                    nuevas.add(lectura);
                    anterior = lectura;
                }
                electricaRepository.saveAll(nuevas);
                totalGuardados += nuevas.size();
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

            log.info("[GAP-RECOVERY] Eléctrico ({}): {} lecturas recuperadas en {} página(s).",
                    fuente, totalGuardados, pagina);
        } catch (Exception ex) {
            log.error("[GAP-RECOVERY] Error recuperando brechas eléctricas ({}): {}", fuente, ex.getMessage());
        }
    }
}
