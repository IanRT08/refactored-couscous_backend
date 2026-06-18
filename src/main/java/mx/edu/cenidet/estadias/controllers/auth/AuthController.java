package mx.edu.cenidet.estadias.controllers.auth;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mx.edu.cenidet.estadias.dtos.auth.*;
import mx.edu.cenidet.estadias.dtos.comunes.ApiResponseDTO;
import mx.edu.cenidet.estadias.services.auth.AuthService;
import org.apache.coyote.Response;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponseDTO<LoginResponseDTO>> login(@Valid @RequestBody LoginRequestDTO dto) {
        LoginResponseDTO respuesta = authService.autenticar(dto);
        return ResponseEntity.ok(ApiResponseDTO.ok("Inicio de sesión exitoso", respuesta));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponseDTO<Void>> registrar(@Valid @RequestBody RegisterRequestDTO dto) {
        authService.registrar(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponseDTO.ok("Registro exitoso. Revisa tu correo para activar tu cuenta"));
    }

    @PostMapping("/verify-account")
    public ResponseEntity<ApiResponseDTO<Void>> verificarCuenta(@Valid @RequestBody VerifyTokenRequestDTO dto) {
        authService.confirmarCuenta(dto);
        return ResponseEntity.ok(ApiResponseDTO.ok("Cuenta activada. Ya puedes iniciar sesión."));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponseDTO<Void>> olvidarPassword(@Valid @RequestBody ForgotPasswordRequestDTO dto) {
        authService.solicitarRecuperacionPassword(dto);
        return ResponseEntity.ok(ApiResponseDTO.ok("Si el correo está registrado, recibirás un código en los próximos minutos."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponseDTO<Void>> resetearPassword(@Valid @RequestBody ResetPasswordRequestDTO dto) {
        authService.resetearPassword(dto);
        return ResponseEntity.ok(ApiResponseDTO.ok("Contraseña restablecida con éxito. Ya puedes iniciar sesión."));
    }

}
