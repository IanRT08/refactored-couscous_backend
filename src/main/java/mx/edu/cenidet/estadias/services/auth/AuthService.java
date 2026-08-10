package mx.edu.cenidet.estadias.services.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.edu.cenidet.estadias.dtos.auth.*;
import mx.edu.cenidet.estadias.excepciones.BusinessRuleException;
import mx.edu.cenidet.estadias.excepciones.InvalidCredentialsException;
import mx.edu.cenidet.estadias.modelos.usuario.BeanUsuario;
import mx.edu.cenidet.estadias.modelos.usuario.EstadoUsuario;
import mx.edu.cenidet.estadias.modelos.token.TipoToken;
import mx.edu.cenidet.estadias.repositorios.administrador.AdministradorRepository;
import mx.edu.cenidet.estadias.repositorios.usuario.UsuarioRepository;
import mx.edu.cenidet.estadias.services.HistorialAccion.ActionHistoryService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private static final int MAX_INTENTOS     = 3;
    private static final int MINUTOS_BLOQUEO  = 15;

    private final UsuarioRepository usuarioRepository;
    private final AdministradorRepository administradorRepository;
    private final ActionHistoryService actionHistoryService;
    private final TokenService tokenService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    @Transactional(noRollbackFor = InvalidCredentialsException.class)
    public LoginResponseDTO autenticar(LoginRequestDTO dto) {

        //1. Resolver identificador (nombreUsuario o correo)
        BeanUsuario usuario = usuarioRepository.findByCorreo(dto.getCorreo())
                .orElseThrow(() -> new InvalidCredentialsException("Credenciales incorrectas."));

        //2. Verificar estado BLOQUEADO
        if (EstadoUsuario.BLOQUEADO.equals(usuario.getEstado())) {
            if (usuario.getFechaBloqueo() != null &&
                    LocalDateTime.now().isBefore(usuario.getFechaBloqueo().plusMinutes(MINUTOS_BLOQUEO))) {
                throw new BusinessRuleException("Cuenta bloqueada temporalmente. Intente de nuevo en "
                        + MINUTOS_BLOQUEO + " minutos.");
            }
            // Han pasado 15 min → desbloquear automáticamente
            desbloquear(usuario);
        }

        //3. Verificar estado SIN_CONFIRMAR (registro pendiente de verificar correo)
        if (EstadoUsuario.SIN_CONFIRMAR.equals(usuario.getEstado())) {
            throw new BusinessRuleException("Cuenta pendiente de confirmación. Revisa tu correo para activarla.");
        }

        //4. Verificar estado INACTIVO (desactivada por el usuario o el administrador)
        if (EstadoUsuario.INACTIVO.equals(usuario.getEstado())) {
            throw new BusinessRuleException("Cuenta desactivada. Comunícate con el administrador.");
        }

        //5. Verificar contraseña
        if (!passwordEncoder.matches(dto.getContrasenia(), usuario.getContrasenia())) {
            registrarIntentoFallido(usuario);
            throw new InvalidCredentialsException("Credenciales incorrectas.");
        }

        //Login exitoso: resetear intentos
        usuario.setIntentosFallidos(0);
        usuario.setFechaBloqueo(null);
        usuarioRepository.save(usuario);

        //Determinar rol efectivo
        String rol = determinarRol(usuario.getIdUsuario());

        //Generar JWT con rol como claim
        String token = jwtService.generarToken(
                usuario.getNombreUsuario(),
                Map.of("rol", rol, "idUsuario", usuario.getIdUsuario())
        );

        //Registrar acción
        actionHistoryService.registrar(usuario.getIdUsuario(), "LOGIN_EXITOSO",
                "Inicio de sesión desde el sistema");

        log.info("Login exitoso: idUsuario={} (rol: {})", usuario.getIdUsuario(), rol);

        return LoginResponseDTO.builder()
                .token(token)
                .tipo("Bearer")
                .idUsuario(usuario.getIdUsuario())
                .nombreUsuario(usuario.getNombreUsuario())
                .nombreCompleto(usuario.getNombreCompleto())
                .correo(usuario.getCorreo())
                .rol(rol)
                .fechaExpiracionToken(
                        LocalDateTime.now().plusSeconds(jwtService.getExpirationMs() / 1000))
                .build();
    }

    @Transactional
    public void registrar(RegisterRequestDTO dto) {
        if (usuarioRepository.existsByNombreUsuario(dto.getNombreUsuario())) {
            throw new BusinessRuleException("El nombre de usuario ya está en uso.");
        }
        if (usuarioRepository.existsByCorreo(dto.getCorreo())) {
            throw new BusinessRuleException("El correo ya está registrado.");
        }

        //Estado SIN_CONFIRMAR hasta confirmar el correo (distinto de INACTIVO,
        //que se reserva para cuentas desactivadas por el usuario o el administrador)
        BeanUsuario nuevoUsuario = BeanUsuario.builder()
                .nombreUsuario(dto.getNombreUsuario())
                .correo(dto.getCorreo())
                .nombreCompleto(dto.getNombreCompleto())
                .contrasenia(passwordEncoder.encode(dto.getContrasenia()))
                .estado(EstadoUsuario.SIN_CONFIRMAR)
                .intentosFallidos(0)
                .build();

        usuarioRepository.save(nuevoUsuario);

        //Generar y enviar token de confirmación
        tokenService.generarYEnviarToken(nuevoUsuario.getIdUsuario(),
                TipoToken.CONFIRMACION_CUENTA,
                nuevoUsuario.getCorreo(), nuevoUsuario.getNombreCompleto());

        log.info("Usuario registrado (pendiente de confirmación): {}", dto.getNombreUsuario());
    }

    private void registrarIntentoFallido(BeanUsuario usuario) {
        usuarioRepository.incrementarIntentosFallidos(usuario.getIdUsuario());
        int intentos = usuarioRepository.leerIntentosFallidos(usuario.getIdUsuario());

        if (intentos >= MAX_INTENTOS) {
            usuarioRepository.bloquearCuenta(usuario.getIdUsuario(), EstadoUsuario.BLOQUEADO, LocalDateTime.now());
            log.warn("Cuenta bloqueada por exceder {} intentos: idUsuario={}", MAX_INTENTOS, usuario.getIdUsuario());
            actionHistoryService.registrar(usuario.getIdUsuario(), "CUENTA_BLOQUEADA",
                    "Cuenta bloqueada por " + MAX_INTENTOS + " intentos fallidos");
        } else {
            actionHistoryService.registrar(usuario.getIdUsuario(), "LOGIN_FALLIDO",
                    "Intento fallido #" + intentos);
        }
    }

    private void desbloquear(BeanUsuario usuario) {
        usuario.setEstado(EstadoUsuario.ACTIVO);
        usuario.setIntentosFallidos(0);
        usuario.setFechaBloqueo(null);
        usuarioRepository.save(usuario);
        log.info("Cuenta desbloqueada automáticamente: {}", usuario.getNombreUsuario());
    }

    private String determinarRol(Long idUsuario) {
        return administradorRepository.findByUsuario_IdUsuario(idUsuario)
                .map(a -> a.getTipoAdministrador().name())
                .orElse("USUARIO");
    }

    @Transactional
        public void confirmarCuenta(VerifyTokenRequestDTO dto) {
        BeanUsuario usuario = usuarioRepository.findByCorreo(dto.getCorreo())
        .orElseThrow(() -> new BusinessRuleException("Correo no registrado."));
        tokenService.validarYConsumir(usuario.getIdUsuario(), dto.getCodigo(),
            TipoToken.CONFIRMACION_CUENTA);
            usuario.setEstado(EstadoUsuario.ACTIVO);
            usuarioRepository.save(usuario);
            actionHistoryService.registrar(usuario.getIdUsuario(), "CUENTA_CONFIRMADA", "Cuenta activada tras verificación de correo");
    }

    @Transactional
    public void solicitarRecuperacionPassword(ForgotPasswordRequestDTO dto) {
        // No revelar si el correo existe o no (previene enumeración de usuarios).
        // El controlador ya retorna el mismo mensaje genérico en ambos casos.
        usuarioRepository.findByCorreo(dto.getCorreo()).ifPresent(usuario ->
            tokenService.generarYEnviarToken(usuario.getIdUsuario(),
                    TipoToken.RECUPERACION,
                    usuario.getCorreo(), usuario.getNombreCompleto())
        );
    }

    @Transactional(readOnly = true)
    public void verificarCodigoRecuperacion(VerifyTokenRequestDTO dto) {
        BeanUsuario usuario = usuarioRepository.findByCorreo(dto.getCorreo())
                .orElseThrow(() -> new BusinessRuleException("Correo no registrado."));
        tokenService.validarSinConsumir(usuario.getIdUsuario(), dto.getCodigo(), TipoToken.RECUPERACION);
    }

    @Transactional
    public void resetearPassword(ResetPasswordRequestDTO dto) {
        BeanUsuario usuario = usuarioRepository.findByCorreo(dto.getCorreo())
             .orElseThrow(() -> new BusinessRuleException("Correo no registrado."));
        tokenService.validarYConsumir(usuario.getIdUsuario(), dto.getCodigo(),
             TipoToken.RECUPERACION);
        usuario.setContrasenia(passwordEncoder.encode(dto.getNuevaContrasenia()));
        usuarioRepository.save(usuario);
        actionHistoryService.registrar(usuario.getIdUsuario(), "PASSWORD_RESETEADO",
             "Contraseña restablecida por flujo de recuperación");
    }



}
