package mx.edu.cenidet.estadias.services.sync;

import lombok.extern.slf4j.Slf4j;
import mx.edu.cenidet.estadias.config.AmbientWeatherClient;
import mx.edu.cenidet.estadias.config.ThingSpeakClient;
import mx.edu.cenidet.estadias.dtos.client.AmbientWeatherReadingDTO;
import mx.edu.cenidet.estadias.dtos.client.ThingSpeakFeedDTO;
import mx.edu.cenidet.estadias.dtos.sync.BackfillEstadoDTO;
import mx.edu.cenidet.estadias.dtos.sync.FechasDisponiblesDTO;
import mx.edu.cenidet.estadias.mappers.AmbientWeatherMapper;
import mx.edu.cenidet.estadias.mappers.ThingSpeakMapper;
import mx.edu.cenidet.estadias.modelos.lectura.BeanLectura;
import mx.edu.cenidet.estadias.modelos.lecturaElectrica.BeanLecturaElectrica;
import mx.edu.cenidet.estadias.modelos.lecturaElectrica.FuenteElectrica;
import mx.edu.cenidet.estadias.repositorios.lectura.LecturaRepository;
import mx.edu.cenidet.estadias.repositorios.lecturaElectrica.LecturaElectricaRepository;
import mx.edu.cenidet.estadias.util.EnergiaCalculator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

//Respaldo histórico MANUAL (no automático): trae a la base de datos local
//datos que ya existen en Ambient Weather / ThingSpeak desde hace meses, pero
//que el GapRecoveryService normal nunca toca (ese está acotado a
//desconexiones cortas, no a importar todo el historial previo).
//
//Cada página se guarda en su PROPIA transacción (vía TransactionTemplate) en
//vez de envolver todo el respaldo en una sola transacción gigante: así una
//corrida de meses no mantiene abierta una transacción larguísima ni acumula
//miles de entidades en el EntityManager.
@Service
@Slf4j
public class HistoricalBackfillService {

    //Salvaguarda anti-loop-infinito; no es un límite de negocio (a diferencia
    //de los MAX_PAGINAS_* de GapRecoveryService, aquí sí puede ser necesario
    //recorrer cientos de páginas para cubrir varios meses).
    private static final int MAX_PAGINAS_SEGURIDAD = 100_000;

    @Value("${respaldo-historico.delay-ms-entre-paginas:2000}")
    private long delayMsEntrePaginas;

    private final LecturaRepository lecturaRepository;
    private final LecturaElectricaRepository electricaRepository;
    private final AmbientWeatherClient ambientWeatherClient;
    private final ThingSpeakClient thingSpeakClient;
    private final AmbientWeatherMapper ambientWeatherMapper;
    private final ThingSpeakMapper thingSpeakMapper;
    private final TransactionTemplate transactionTemplate;

    private final AtomicBoolean climaticoEnProgreso = new AtomicBoolean(false);
    private final Map<FuenteElectrica, AtomicBoolean> electricoEnProgreso = new EnumMap<>(FuenteElectrica.class);

    public HistoricalBackfillService(
            LecturaRepository lecturaRepository,
            LecturaElectricaRepository electricaRepository,
            AmbientWeatherClient ambientWeatherClient,
            ThingSpeakClient thingSpeakClient,
            AmbientWeatherMapper ambientWeatherMapper,
            ThingSpeakMapper thingSpeakMapper,
            PlatformTransactionManager transactionManager) {
        this.lecturaRepository = lecturaRepository;
        this.electricaRepository = electricaRepository;
        this.ambientWeatherClient = ambientWeatherClient;
        this.thingSpeakClient = thingSpeakClient;
        this.ambientWeatherMapper = ambientWeatherMapper;
        this.thingSpeakMapper = thingSpeakMapper;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        for (FuenteElectrica fuente : FuenteElectrica.values()) {
            electricoEnProgreso.put(fuente, new AtomicBoolean(false));
        }
    }

    public boolean climaticoEstaEnProgreso() {
        return climaticoEnProgreso.get();
    }

    public boolean electricoEstaEnProgreso(FuenteElectrica fuente) {
        return electricoEnProgreso.get(fuente).get();
    }

    public BackfillEstadoDTO obtenerEstado() {
        return new BackfillEstadoDTO(
                climaticoEnProgreso.get(),
                electricoEnProgreso.get(FuenteElectrica.FOTOVOLTAICO).get(),
                electricoEnProgreso.get(FuenteElectrica.EOLICO).get(),
                lecturaRepository.findMinFechaLectura().orElse(null),
                electricaRepository.findMinFechaLectura(FuenteElectrica.FOTOVOLTAICO).orElse(null),
                electricaRepository.findMinFechaLectura(FuenteElectrica.EOLICO).orElse(null)
        );
    }

    // ── Respaldo climático (Ambient Weather) ──────────────────
    @Async
    public void respaldarClimatico(LocalDateTime desde, LocalDateTime hasta) {
        if (!climaticoEnProgreso.compareAndSet(false, true)) {
            log.warn("[RESPALDO-HISTORICO] Ya hay un respaldo climático en curso; se ignora esta solicitud.");
            return;
        }
        try {
            log.info("[RESPALDO-HISTORICO] Climático: iniciando {} → {}", desde, hasta);
            long totalGuardados = 0;
            LocalDateTime cursor = hasta;
            int pagina = 0;

            while (cursor.isAfter(desde) && pagina < MAX_PAGINAS_SEGURIDAD) {
                if (pagina > 0) {
                    pausar();
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

                int guardadasPagina = guardarPaginaClimatica(lecturas);
                totalGuardados += guardadasPagina;

                LocalDateTime masAntigua = lecturas.get(0).getFechaLectura();
                pagina++;
                log.info("[RESPALDO-HISTORICO] Climático: página {} -> {} nuevas (acumulado {})",
                        pagina, guardadasPagina, totalGuardados);

                //Sin avance o ya cubrimos el inicio del rango -> terminar
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

            log.info("[RESPALDO-HISTORICO] Climático completo: {} lecturas nuevas en {} página(s).",
                    totalGuardados, pagina);
        } catch (Exception ex) {
            log.error("[RESPALDO-HISTORICO] Error en respaldo climático: {}", ex.getMessage(), ex);
        } finally {
            climaticoEnProgreso.set(false);
        }
    }

    private int guardarPaginaClimatica(List<BeanLectura> lecturas) {
        return transactionTemplate.execute(status -> {
            Set<LocalDateTime> existentes = new HashSet<>(lecturaRepository.findFechasByFechaLecturaBetween(
                    lecturas.get(0).getFechaLectura(),
                    lecturas.get(lecturas.size() - 1).getFechaLectura()));
            List<BeanLectura> nuevas = lecturas.stream()
                    .filter(l -> !existentes.contains(l.getFechaLectura()))
                    .toList();
            lecturaRepository.saveAll(nuevas);
            return nuevas.size();
        });
    }

    // ── Respaldo eléctrico (ThingSpeak, una fuente a la vez) ──
    @Async
    public void respaldarElectrico(FuenteElectrica fuente, LocalDateTime desde, LocalDateTime hasta) {
        AtomicBoolean enProgreso = electricoEnProgreso.get(fuente);
        if (!enProgreso.compareAndSet(false, true)) {
            log.warn("[RESPALDO-HISTORICO] Ya hay un respaldo eléctrico ({}) en curso; se ignora esta solicitud.", fuente);
            return;
        }
        try {
            log.info("[RESPALDO-HISTORICO] Eléctrico ({}): iniciando {} → {}", fuente, desde, hasta);
            long totalGuardados = 0;
            LocalDateTime cursorDesde = desde;
            int pagina = 0;
            //Lectura inmediatamente anterior a "desde" (si ya existe algo más
            //reciente en la tabla, NO sirve como referencia para la energía
            //de estos datos antiguos: por eso se busca explícitamente la que
            //queda estrictamente antes del rango a respaldar).
            BeanLecturaElectrica anterior = electricaRepository
                    .findTopByFuenteAndFechaLecturaLessThanOrderByFechaLecturaDesc(fuente, desde)
                    .orElse(null);

            while (cursorDesde.isBefore(hasta) && pagina < MAX_PAGINAS_SEGURIDAD) {
                if (pagina > 0) {
                    pausar();
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

                PaginaElectricaResultado resultado = guardarPaginaElectrica(fuente, lecturas, anterior);
                anterior = resultado.ultimaLectura();
                totalGuardados += resultado.guardadas();
                pagina++;
                log.info("[RESPALDO-HISTORICO] Eléctrico ({}): página {} -> {} nuevas (acumulado {})",
                        fuente, pagina, resultado.guardadas(), totalGuardados);

                LocalDateTime masReciente = lecturas.get(lecturas.size() - 1).getFechaLectura();
                if (!masReciente.isAfter(cursorDesde)) {
                    break; // sin avance, evitar bucle infinito
                }
                cursorDesde = masReciente.plusSeconds(1);
            }

            log.info("[RESPALDO-HISTORICO] Eléctrico ({}) completo: {} lecturas nuevas en {} página(s).",
                    fuente, totalGuardados, pagina);
        } catch (Exception ex) {
            log.error("[RESPALDO-HISTORICO] Error en respaldo eléctrico ({}): {}", fuente, ex.getMessage(), ex);
        } finally {
            enProgreso.set(false);
        }
    }

    private record PaginaElectricaResultado(int guardadas, BeanLecturaElectrica ultimaLectura) {
    }

    private PaginaElectricaResultado guardarPaginaElectrica(
            FuenteElectrica fuente, List<BeanLecturaElectrica> lecturas, BeanLecturaElectrica anteriorInicial) {
        return transactionTemplate.execute(status -> {
            Set<LocalDateTime> existentes = new HashSet<>(electricaRepository.findFechasByFuenteAndFechaLecturaBetween(
                    fuente,
                    lecturas.get(0).getFechaLectura(),
                    lecturas.get(lecturas.size() - 1).getFechaLectura()));

            BeanLecturaElectrica anterior = anteriorInicial;
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
            return new PaginaElectricaResultado(nuevas.size(), anterior);
        });
    }

    private void pausar() {
        try {
            Thread.sleep(delayMsEntrePaginas);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    // ── Detección de fechas disponibles en las APIs externas ──────────

    //Busca la fecha más antigua disponible en cada API mediante búsqueda binaria
    //sobre los últimos 4 años (≈6 llamadas por fuente). Se puede tardar ~15-30 s.
    public FechasDisponiblesDTO detectarFechasDisponibles() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime inicioAW      = buscarFechaInicio(cursor -> ambientWeatherClient.existenLecturasHasta(cursor), now).orElse(null);
        LocalDateTime inicioFV      = buscarFechaInicio(cursor -> thingSpeakClient.existenFeedsHasta(FuenteElectrica.FOTOVOLTAICO, cursor), now).orElse(null);
        LocalDateTime inicioEolico  = buscarFechaInicio(cursor -> thingSpeakClient.existenFeedsHasta(FuenteElectrica.EOLICO, cursor), now).orElse(null);
        return new FechasDisponiblesDTO(inicioAW, inicioFV, inicioEolico);
    }

    //Búsqueda binaria sobre los últimos MAX_MESES_SONDEO meses.
    //f(cursor) = "¿existen datos ANTES de cursor?". Se espera f(now)=true y
    //f(now-MAX_MESES_SONDEO meses)=false (o se retorna el límite como aproximación).
    //Granularidad resultante: ±1 mes.
    private static final int MAX_MESES_SONDEO = 48; // 4 años

    private Optional<LocalDateTime> buscarFechaInicio(Predicate<LocalDateTime> existeAntesDe, LocalDateTime now) {
        try {
            // Si no hay datos recientes, la fuente está desconectada o vacía.
            if (!existeAntesDe.test(now)) {
                return Optional.empty();
            }
            // Si hay datos de más de 4 años, devolver el límite como aproximación.
            if (existeAntesDe.test(now.minusMonths(MAX_MESES_SONDEO))) {
                return Optional.of(now.minusMonths(MAX_MESES_SONDEO));
            }
            // Búsqueda binaria: lo = último offset con datos; hi = primer offset sin datos.
            int lo = 0;
            int hi = MAX_MESES_SONDEO;
            while (hi - lo > 1) {
                int mid = (lo + hi) / 2;
                if (existeAntesDe.test(now.minusMonths(mid))) {
                    lo = mid;
                } else {
                    hi = mid;
                }
            }
            // Los datos empiezan aproximadamente en (now - hi meses). Devolver el 1º de ese mes.
            return Optional.of(now.minusMonths(hi).withDayOfMonth(1)
                    .withHour(0).withMinute(0).withSecond(0).withNano(0));
        } catch (Exception ex) {
            log.warn("[RESPALDO-HISTORICO] Error durante sondeo de fechas: {}", ex.getMessage());
            return Optional.empty();
        }
    }
}
