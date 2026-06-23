package mx.edu.cenidet.estadias.controllers.sync;

import lombok.RequiredArgsConstructor;
import mx.edu.cenidet.estadias.dtos.comunes.ApiResponseDTO;
import mx.edu.cenidet.estadias.services.sync.SyncHealthService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/sync")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMINISTRADOR', 'SUPERADMINISTRADOR')")
public class SyncStatusController {

    private final SyncHealthService syncHealthService;

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
