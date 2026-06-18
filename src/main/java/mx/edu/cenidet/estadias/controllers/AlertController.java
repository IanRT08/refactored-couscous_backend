package mx.edu.cenidet.estadias.controllers;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mx.edu.cenidet.estadias.dtos.alertas.AlertDTO;
import mx.edu.cenidet.estadias.dtos.alertas.AlertSettingsDTO;
import mx.edu.cenidet.estadias.dtos.comunes.ApiResponseDTO;
import mx.edu.cenidet.estadias.dtos.comunes.PageResponseDTO;
import mx.edu.cenidet.estadias.services.alert.AlertService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Módulo 8 — Alertas y configuración de notificaciones.
// Acceso diferenciado por endpoint:
//   /sistema  → público (visitantes y usuarios)
//   demás     → requieren autenticación
// DFR RN: "Todos los usuarios (registrados y no registrados) podrán
//           ver las alertas del sistema pero solo los registrados
//           podrán configurarlas y ver alertas más específicas."
@RestController
@RequestMapping("/api/alertas")
@RequiredArgsConstructor
public class AlertController {

    private final AlertService alertService;
    private final AuthUtils    authUtils;

    // ── GET /api/alertas/sistema ──────────────────────────────
    // Módulo 8.1 — Alertas generales del sistema (usuario = null).
    // Acceso: PÚBLICO — visitantes y usuarios sin sesión pueden verlas.
    // DFR RN: "Los visitantes solo verán alertas muy generales del sistema"
    // El frontend las muestra en el header/banner para todos.
    @GetMapping("/sistema")
    public ResponseEntity<ApiResponseDTO<List<AlertDTO>>> listarAlertasSistema() {
        return ResponseEntity.ok(
                ApiResponseDTO.ok("Alertas del sistema obtenidas.",
                        alertService.listarParaVisitante()));
    }

    // ── GET /api/alertas ──────────────────────────────────────
    // Módulo 8.1 — Alertas personales + generales para el usuario autenticado.
    // DFR RN: "Los usuarios registrados tendrán alertas más específicas"
    //          → combina sus alertas personales (solicitud resuelta, etc.)
    //            con las alertas generales del sistema.
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponseDTO<PageResponseDTO<AlertDTO>>> listarAlertasUsuario(
            HttpServletRequest request,
            @PageableDefault(size = 20, sort = "fechaCreacion",
                    direction = Sort.Direction.DESC) Pageable pageable) {

        Long idUsuario = authUtils.getIdUsuarioActual(request);
        return ResponseEntity.ok(
                ApiResponseDTO.ok("Alertas obtenidas.",
                        alertService.listarParaUsuario(idUsuario, pageable)));
    }

    // ── GET /api/alertas/configuracion ───────────────────────
    // Módulo 8 — Ver la preferencia actual de alertas del usuario.
    // Valores posibles: "TODAS", "SISTEMA", "NINGUNA"
    @GetMapping("/configuracion")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponseDTO<AlertSettingsDTO>> obtenerConfiguracion(
            HttpServletRequest request) {

        Long idUsuario = authUtils.getIdUsuarioActual(request);
        return ResponseEntity.ok(
                ApiResponseDTO.ok("Configuración de alertas obtenida.",
                        alertService.obtenerConfiguracion(idUsuario)));
    }

    // ── PUT /api/alertas/configuracion ───────────────────────
    // Módulo 8 — Actualizar preferencia de alertas.
    // DFR RN: "Las alertas se pueden desactivar en los ajustes de perfil"
    //          con opciones mutuamente exclusivas (radio en el frontend):
    //          TODAS | SISTEMA | NINGUNA
    // @Valid valida el @Pattern("TODAS|SISTEMA|NINGUNA") del DTO.
    @PutMapping("/configuracion")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponseDTO<Void>> actualizarConfiguracion(
            @Valid @RequestBody AlertSettingsDTO dto,
            HttpServletRequest request) {

        Long idUsuario = authUtils.getIdUsuarioActual(request);
        alertService.actualizarConfiguracion(idUsuario, dto);
        return ResponseEntity.ok(ApiResponseDTO.ok("Preferencia de alertas actualizada."));
    }
}
