package mx.edu.cenidet.estadias.controllers.user;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mx.edu.cenidet.estadias.dtos.comunes.ApiResponseDTO;
import mx.edu.cenidet.estadias.dtos.usuario.*;
import mx.edu.cenidet.estadias.services.usuario.UserService;
import mx.edu.cenidet.estadias.util.AuthUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class UserController {

    private final UserService userService;
    private final AuthUtils authUtils;

    //Ver perfil de usuario en sesion
    @GetMapping("/profile")
    public ResponseEntity<ApiResponseDTO<UserProfileDTO>> obtenerPerfil(HttpServletRequest request) {
        Long idUsuario = authUtils.getIdUsuarioActual(request);
        return ResponseEntity.ok(ApiResponseDTO.ok("Perfil obtenido", userService.obtenerPerfil(idUsuario)));
    }

    //Actualizar nombre completo y foto de perfil
    @PutMapping("/profile")
    public ResponseEntity<ApiResponseDTO<UserProfileDTO>> actualizarPerfil(@Valid @RequestBody UpdateProfileRequestDTO dto, HttpServletRequest request) {
        Long idUsuario = authUtils.getIdUsuarioActual(request);
        return ResponseEntity.ok(ApiResponseDTO.ok("Perfil actualizado.", userService.actualizarPerfil(idUsuario, dto)));
    }

    //Cambiar la contraseña
    @PutMapping("/change-password")
    public ResponseEntity<ApiResponseDTO<Void>> cambiarContraseña(@Valid @RequestBody ChangePasswordRequestDTO dto, HttpServletRequest request) {
        Long idUsuario = authUtils.getIdUsuarioActual(request);
        userService.cambiarContrasenia(idUsuario, dto);
        return ResponseEntity.ok(ApiResponseDTO.ok("Contraseña actualizada con éxito"));
    }

    //Solicitar codigo de desactivacion de cuenta
    @PostMapping("/request-deactivation")
    public ResponseEntity<ApiResponseDTO<Void>> solicitarDesactivacion(HttpServletRequest request) {
        Long idUsuario = authUtils.getIdUsuarioActual(request);
        userService.solicitarDesactivacion(idUsuario);
        return ResponseEntity.ok(ApiResponseDTO.ok("Se envió un código a tu correo. Úsalo junto con tu contraseña para confirmar la desactivación."));
    }

    //Confirmar desactivacion de cuenta con contraseña
    @PostMapping("/deactivate")
    public ResponseEntity<ApiResponseDTO<Void>> desactivarCuenta(@Valid @RequestBody DesactiveAccountRequestDTO dto, HttpServletRequest request) {
        Long idUsuario = authUtils.getIdUsuarioActual(request);
        userService.desactivarCuenta(idUsuario, dto);
        return ResponseEntity.ok(ApiResponseDTO.ok("Cuenta desactivada. Tu sesión ha sido cerrada."));
    }



}
