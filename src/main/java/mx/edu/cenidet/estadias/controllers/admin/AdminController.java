package mx.edu.cenidet.estadias.controllers.admin;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mx.edu.cenidet.estadias.dtos.administrador.*;
import mx.edu.cenidet.estadias.dtos.comunes.ApiResponseDTO;
import mx.edu.cenidet.estadias.dtos.comunes.PageResponseDTO;
import mx.edu.cenidet.estadias.modelos.usuario.EstadoUsuario;
import mx.edu.cenidet.estadias.services.HistorialAccion.ActionHistoryService;
import mx.edu.cenidet.estadias.services.administrador.AdminService;
import mx.edu.cenidet.estadias.util.AuthUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final ActionHistoryService actionHistoryService;
    private final AuthUtils authUtils;

    //Tabla de usuarios
    @GetMapping("/usuarios")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'SUPERADMINISTRADOR')")
    public ResponseEntity<ApiResponseDTO<PageResponseDTO<UserSummaryDTO>>> listarUsuarios(
            @RequestParam(required = false) String termino,
            @RequestParam(required = false) EstadoUsuario estado,
            @PageableDefault(size = 20, sort = "fechaRegistro", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponseDTO.ok("Usuarios obtenidos.", adminService.listarUsuarios(termino, estado, pageable)));
    }

    //Ver usuarios a detalle
    @GetMapping("/usuarios/{idUsuario}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'SUPERADMINISTRADOR')")
    public ResponseEntity<ApiResponseDTO<UserDetailDTO>> obtenerDetalleUsuario(@PathVariable Long idUsuario) {
        return ResponseEntity.ok(ApiResponseDTO.ok("Detalle de usuario obtenido.", adminService.obtenerDetalle(idUsuario)));
    }

    //Cambiar estado de usuarios
    @PutMapping("/usuarios/{idUsuario}/estado")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'SUPERADMINISTRADOR')")
    public ResponseEntity<ApiResponseDTO<UserSummaryDTO>> cambiarEstadoUsuario(
            @PathVariable Long idUsuario,
            @Valid @RequestBody UpdateUserStatusRequestDTO dto,
            HttpServletRequest request) {
        Long idAdmin = authUtils.getIdUsuarioActual(request);
        return ResponseEntity.ok(ApiResponseDTO.ok("Estado del usuario actualizado.", adminService.cambiarEstadoUsuario(idAdmin, idUsuario, dto)));
    }

    //Editar permiso de descarga de un usuario (acción directa, sin solicitud)
    @PutMapping("/usuarios/{idUsuario}/permiso-descarga")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'SUPERADMINISTRADOR')")
    public ResponseEntity<ApiResponseDTO<Void>> cambiarPermisoDescarga(
            @PathVariable Long idUsuario,
            @Valid @RequestBody UpdateDownloadPermissionRequestDTO dto,
            HttpServletRequest request) {
        Long idAdmin = authUtils.getIdUsuarioActual(request);
        adminService.cambiarPermisoDescarga(idAdmin, idUsuario, dto);
        return ResponseEntity.ok(ApiResponseDTO.ok("Permiso de descarga actualizado."));
    }

    //Ver lista de admins
    @GetMapping("/administradores")
    @PreAuthorize("hasRole('SUPERADMINISTRADOR')")
    public ResponseEntity<ApiResponseDTO<List<AdminSummaryDTO>>> listarAdministradores(
            HttpServletRequest request) {
        Long idSuperAdmin = authUtils.getIdUsuarioActual(request);
        return ResponseEntity.ok(ApiResponseDTO.ok("Administradores obtenidos.", adminService.listarAdministradores(idSuperAdmin)));
    }

    //Crear nuevos admins
    @PostMapping("/administradores")
    @PreAuthorize("hasRole('SUPERADMINISTRADOR')")
    public ResponseEntity<ApiResponseDTO<AdminSummaryDTO>> crearAdministrador(
            @Valid @RequestBody CreateAdminRequestDTO dto, HttpServletRequest request) {
        Long idSuperAdmin = authUtils.getIdUsuarioActual(request);
        AdminSummaryDTO nuevo = adminService.crearAdministrador(idSuperAdmin, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponseDTO.ok("Administrador creado. Se enviaron sus credenciales por correo.", nuevo));
    }

    //Cambiar tipo de administrador (SUPERADMINISTRADOR only)
    @PutMapping("/administradores/{idAdministrador}/tipo")
    @PreAuthorize("hasRole('SUPERADMINISTRADOR')")
    public ResponseEntity<ApiResponseDTO<AdminSummaryDTO>> cambiarTipoAdministrador(
            @PathVariable Long idAdministrador,
            @Valid @RequestBody UpdateAdminTypeRequestDTO dto,
            HttpServletRequest request) {
        Long idSuperAdmin = authUtils.getIdUsuarioActual(request);
        return ResponseEntity.ok(ApiResponseDTO.ok("Tipo de administrador actualizado.",
                adminService.cambiarTipoAdministrador(idSuperAdmin, idAdministrador, dto)));
    }

    //Historial global de todos los usuarios
    @GetMapping("/historial")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'SUPERADMINISTRADOR')")
    public ResponseEntity<ApiResponseDTO<PageResponseDTO<ActionHistorySummaryDTO>>> listarHistorial(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin,
            @PageableDefault(size = 30, sort = "fechaAccion", direction = Sort.Direction.DESC) Pageable pageable) {
        if (inicio != null && fin != null) {
            return ResponseEntity.ok(ApiResponseDTO.ok("Historial obtenido.", actionHistoryService.listarPorRango(inicio, fin, pageable)));
        }
        return ResponseEntity.ok(ApiResponseDTO.ok("Historial global obtenido.", actionHistoryService.listarGlobal(pageable)));
    }

    //Ver historial de un usuario especifico
    @GetMapping("/historial/{idUsuario}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'SUPERADMINISTRADOR')")
    public ResponseEntity<ApiResponseDTO<List<ActionHistorySummaryDTO>>> listarHistorialDeUsuario(
            @PathVariable Long idUsuario) {
        return ResponseEntity.ok(ApiResponseDTO.ok("Historial del usuario obtenido.", actionHistoryService.listarPorUsuario(idUsuario)));
    }

}
