package mx.edu.cenidet.estadias.controllers;

import lombok.RequiredArgsConstructor;
import mx.edu.cenidet.estadias.dtos.comunes.ApiResponseDTO;
import mx.edu.cenidet.estadias.services.sync.SyncHealthService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

// Módulo 3.3 — Estado de las sincronizaciones en tiempo real.
// Solo accesible para administradores para diagnóstico del sistema.
// El frontend (Módulo 3 RN: "notificar si 3 fallos consecutivos")
// también puede usar /api/alertas/sistema para mostrar esto al usuario,
// pero este endpoint da más detalle técnico al administrador.
@RestController
@RequestMapping("/api/sync")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMINISTRADOR', 'SUPERADMINISTRADOR')")
public class SyncStatusController {

    private final SyncHealthService syncHealthService;

    // ── GET /api/sync/estado ──────────────────────────────────
    // Módulo 3.3 — Estado actual de los dos orígenes de sincronización.
    // Respuesta:
    // {
    //   "AMBIENT_WEATHER": { "fallosConsecutivos": 0, "enError": false },
    //   "THINGSPEAK":      { "fallosConsecutivos": 0, "enError": false }
    // }
    @GetMapping("/estado")
    public ResponseEntity<ApiResponseDTO<Map<String, Object>>> obtenerEstado() {

        Map<String, Object> estado = Map.of(
                SyncHealthService.FUENTE_AMBIENT, Map.of(
                        "fallosConsecutivos",
                        syncHealthService.obtenerFallosConsecutivos(SyncHealthService.FUENTE_AMBIENT),
                        "enError",
                        syncHealthService.estaEnError(SyncHealthService.FUENTE_AMBIENT)
                ),
                SyncHealthService.FUENTE_THINGSPEAK, Map.of(
                        "fallosConsecutivos",
                        syncHealthService.obtenerFallosConsecutivos(SyncHealthService.FUENTE_THINGSPEAK),
                        "enError",
                        syncHealthService.estaEnError(SyncHealthService.FUENTE_THINGSPEAK)
                )
        );

        return ResponseEntity.ok(ApiResponseDTO.ok("Estado de sincronización obtenido.", estado));
    }
}
