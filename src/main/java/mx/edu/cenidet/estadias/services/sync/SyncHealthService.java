package mx.edu.cenidet.estadias.services.sync;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.edu.cenidet.estadias.event.ConexionRecuperadaEvent;
import mx.edu.cenidet.estadias.services.alert.AlertService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

// Centinela de salud de las sincronizaciones.
// Rastrea fallos consecutivos por fuente y:
//   · Al llegar a MAX_FALLOS → crea alerta de sistema (visible a todos)
//   · Al recuperarse        → publica ConexionRecuperadaEvent (gap recovery)
// Usa ConcurrentHashMap para seguridad en entorno multi-hilo
// (los dos @Scheduled de Ambient y ThingSpeak corren en hilos separados).
@Service
@RequiredArgsConstructor
@Slf4j
public class SyncHealthService {

    public static final String FUENTE_AMBIENT    = "AMBIENT_WEATHER";
    public static final String FUENTE_THINGSPEAK = "THINGSPEAK";

    private static final int MAX_FALLOS = 3;

    private final AlertService alertService;
    private final ApplicationEventPublisher eventPublisher;

    // Contadores de fallos consecutivos por fuente
    private final Map<String, Integer> fallosConsecutivos = new ConcurrentHashMap<>();

    // ── Registrar un fallo de sincronización ─────────────────
    public void registrarFallo(String fuente, Exception ex) {
        int fallos = fallosConsecutivos.merge(fuente, 1, Integer::sum);
        log.warn("[SYNC] Fallo #{} en {}: {}", fallos, fuente, ex.getMessage());

        // Al llegar exactamente a MAX_FALLOS → alerta pública (una sola vez)
        if (fallos == MAX_FALLOS) {
            alertService.crearAlertaSistema("SYNC_ERROR",
                    "Sin datos actualizados de " + fuente + " (+" + MAX_FALLOS
                            + " fallos consecutivos). El sistema opera con datos en caché.");
        }
    }

    // ── Registrar éxito ───────────────────────────────────────
    public void marcarExito(String fuente) {
        Integer fallosPrevios = fallosConsecutivos.put(fuente, 0);

        // Si venía de un estado de error, publicar evento de recuperación
        if (fallosPrevios != null && fallosPrevios >= MAX_FALLOS) {
            log.info("[SYNC] Conexión restablecida con {}. Iniciando Gap Recovery.", fuente);
            eventPublisher.publishEvent(new ConexionRecuperadaEvent(this, fuente));
        }
    }

    // ── Consultar estado actual (para SyncStatusController) ──
    public int obtenerFallosConsecutivos(String fuente) {
        return fallosConsecutivos.getOrDefault(fuente, 0);
    }

    public boolean estaEnError(String fuente) {
        return obtenerFallosConsecutivos(fuente) >= MAX_FALLOS;
    }
}