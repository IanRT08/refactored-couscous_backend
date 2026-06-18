package mx.edu.cenidet.estadias.controllers.alert;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mx.edu.cenidet.estadias.dtos.alertas.AlertDTO;
import mx.edu.cenidet.estadias.dtos.alertas.AlertSettingsDTO;
import mx.edu.cenidet.estadias.dtos.comunes.ApiResponseDTO;
import mx.edu.cenidet.estadias.dtos.comunes.PageResponseDTO;
import mx.edu.cenidet.estadias.services.alert.AlertService;
import mx.edu.cenidet.estadias.util.AuthUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/alertas")
@RequiredArgsConstructor
public class AlertController {

    private final AlertService alertService;
    private final AuthUtils authUtils;

    //Alertas generales del sistema
    @GetMapping("/sistema")
    public ResponseEntity<ApiResponseDTO<List<AlertDTO>>> listarAlertasSistema() {
        return ResponseEntity.ok(
                ApiResponseDTO.ok("Alertas del sistema obtenidas.",
                        alertService.listarParaVisitante()));
    }

    //Alertas personales + generales para el usuario autenticado
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

    //Ver la preferencia actual de alertas del usuario.
    @GetMapping("/configuracion")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponseDTO<AlertSettingsDTO>> obtenerConfiguracion(
            HttpServletRequest request) {

        Long idUsuario = authUtils.getIdUsuarioActual(request);
        return ResponseEntity.ok(
                ApiResponseDTO.ok("Configuración de alertas obtenida.",
                        alertService.obtenerConfiguracion(idUsuario)));
    }

    //Actualizar preferencia de alertas.
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
