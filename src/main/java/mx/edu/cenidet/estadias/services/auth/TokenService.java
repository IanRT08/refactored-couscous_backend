package mx.edu.cenidet.estadias.services.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.edu.cenidet.estadias.excepciones.BusinessRuleException;
import mx.edu.cenidet.estadias.modelos.token.BeanToken;
import mx.edu.cenidet.estadias.modelos.token.TipoToken;
import mx.edu.cenidet.estadias.repositorios.token.TokenRepository;
import mx.edu.cenidet.estadias.repositorios.usuario.UsuarioRepository;
import mx.edu.cenidet.estadias.services.envioCorreos.MailService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class TokenService {

    private static final int    DURACION_MINUTOS    = 15;
    private static final int    MINUTOS_PARA_REENVIO = 5;

    private final TokenRepository tokenRepository;
    private final UsuarioRepository usuarioRepository;
    private final MailService mailService;


    @Transactional
    public BeanToken generarYEnviarToken(Long idUsuario, TipoToken tipo, String correo, String nombre) {
        LocalDateTime ahora = LocalDateTime.now();
        tokenRepository.findUltimoTokenPorTipo(idUsuario, tipo).ifPresent(ultimo -> {
            long minutosRestantes = Duration.between(ahora, ultimo.getFechaExpiracion()).toMinutes();
            if (minutosRestantes > DURACION_MINUTOS - MINUTOS_PARA_REENVIO) {
                throw new BusinessRuleException(
                        "Ya se envió un código recientemente. Espera unos minutos antes de solicitar uno nuevo.");
            }
        });

        //Invalidar tokens previos del mismo tipo
        tokenRepository.findTokenVigente(idUsuario, tipo, ahora)
                .ifPresent(t -> { t.setUsado(true); tokenRepository.save(t); });

        //Código numérico de 6 dígitos
        int codigo = 100_000 + new SecureRandom().nextInt(900_000);

        BeanToken token = BeanToken.builder()
                .codigo(codigo)
                .tipo(tipo)
                .fechaExpiracion(ahora.plusMinutes(DURACION_MINUTOS))
                .usado(false)
                .usuario(usuarioRepository.getReferenceById(idUsuario))
                .build();
        tokenRepository.save(token);

        //Enviar correo según tipo de token
        switch (tipo) {
            case RECUPERACION ->
                    mailService.enviarRecuperacionPassword(correo, nombre, codigo);
            case CONFIRMACION_CUENTA ->
                    mailService.enviarConfirmacionCuenta(correo, nombre, codigo);
            case DESACTIVACION_CUENTA ->
                    mailService.enviarDesactivacionCuenta(correo, nombre, codigo);
        }

        log.info("Token {} generado para usuario {}", tipo, idUsuario);
        return token;
    }

    // Validar sin consumir — usado para verificar el código de recuperación antes del reseteo
    public void validarSinConsumir(Long idUsuario, Integer codigoIngresado, TipoToken tipo) {
        BeanToken token = tokenRepository.findTokenVigente(idUsuario, tipo, LocalDateTime.now())
                .orElseThrow(() -> new BusinessRuleException(
                        "El código es inválido o ha expirado. Solicita uno nuevo."));
        if (!token.getCodigo().equals(codigoIngresado)) {
            throw new BusinessRuleException("El código ingresado es incorrecto.");
        }
    }

    //Validar y consumir token
    public void validarYConsumir(Long idUsuario, Integer codigoIngresado, TipoToken tipo) {
        BeanToken token = tokenRepository.findTokenVigente(idUsuario, tipo, LocalDateTime.now())
                .orElseThrow(() -> new BusinessRuleException(
                        "El código es inválido o ha expirado. Solicita uno nuevo."));

        if (!token.getCodigo().equals(codigoIngresado)) {
            throw new BusinessRuleException("El código ingresado es incorrecto.");
        }

        token.setUsado(true);
        tokenRepository.save(token);
        log.info("Token {} consumido exitosamente para usuario {}", tipo, idUsuario);
    }

    @Scheduled(fixedRate = 3_600_000) // cada 60 minutos
    @Transactional
    public void purgarTokensExpirados() {
        try {
            tokenRepository.deleteByFechaExpiracionBefore(LocalDateTime.now());
            log.debug("Purga de tokens expirados completada.");
        } catch (Exception e) {
            log.warn("Error durante la purga de tokens expirados — la tabla token puede crecer. Causa: {}",
                    e.getMessage());
        }
    }
}
