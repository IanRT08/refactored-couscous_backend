package mx.edu.cenidet.estadias.services.usuario;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.edu.cenidet.estadias.dtos.usuario.ChangePasswordRequestDTO;
import mx.edu.cenidet.estadias.dtos.usuario.DesactiveAccountRequestDTO;
import mx.edu.cenidet.estadias.dtos.usuario.UpdateProfileRequestDTO;
import mx.edu.cenidet.estadias.dtos.usuario.UserProfileDTO;
import mx.edu.cenidet.estadias.excepciones.BusinessRuleException;
import mx.edu.cenidet.estadias.excepciones.ResourceNotFoundException;
import mx.edu.cenidet.estadias.modelos.permisosDescarga.EstadoPermiso;
import mx.edu.cenidet.estadias.modelos.token.TipoToken;
import mx.edu.cenidet.estadias.modelos.usuario.BeanUsuario;
import mx.edu.cenidet.estadias.modelos.usuario.EstadoUsuario;
import mx.edu.cenidet.estadias.repositorios.administrador.AdministradorRepository;
import mx.edu.cenidet.estadias.repositorios.permisoDescarga.PermisoDescargaRepository;
import mx.edu.cenidet.estadias.repositorios.usuario.UsuarioRepository;
import mx.edu.cenidet.estadias.services.HistorialAccion.ActionHistoryService;
import mx.edu.cenidet.estadias.services.auth.TokenService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Base64;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UsuarioRepository usuarioRepository;
    private final AdministradorRepository administradorRepository;
    private final PermisoDescargaRepository permisoDescargaRepository;
    private final ActionHistoryService actionHistoryService;
    private final TokenService tokenService;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public UserProfileDTO obtenerPerfil(Long idUsuario) {
        BeanUsuario usuario = buscarUsuarioPorId(idUsuario);

        boolean tienePermiso = permisoDescargaRepository
                .existsByUsuario_IdUsuarioAndPermisoDescarga(idUsuario, EstadoPermiso.ACTIVO);

        return mapToUserProfileDTO(usuario, tienePermiso);
    }

    //Actualizar perfil
    @Transactional
    public UserProfileDTO actualizarPerfil(Long idUsuario, UpdateProfileRequestDTO dto) {
        BeanUsuario usuario = buscarUsuarioPorId(idUsuario);

        if (dto.getNombreCompleto() != null && !dto.getNombreCompleto().isBlank()) {
            usuario.setNombreCompleto(dto.getNombreCompleto());
        }
        if (dto.getFotoPerfil() != null && !dto.getFotoPerfil().isBlank()) {
            usuario.setFotoPerfil(Base64.getDecoder().decode(dto.getFotoPerfil()));
        }

        usuarioRepository.save(usuario);
        actionHistoryService.registrar(idUsuario, "PERFIL_ACTUALIZADO", "El usuario actualizó su perfil");

        boolean tienePermiso = permisoDescargaRepository
                .existsByUsuario_IdUsuarioAndPermisoDescarga(idUsuario, EstadoPermiso.ACTIVO);
        return mapToUserProfileDTO(usuario, tienePermiso);
    }

    //Cambiar contraseña
    @Transactional
    public void cambiarContrasenia(Long idUsuario, ChangePasswordRequestDTO dto) {
        BeanUsuario usuario = buscarUsuarioPorId(idUsuario);

        if (!passwordEncoder.matches(dto.getContraseniaActual(), usuario.getContrasenia())) {
            throw new BusinessRuleException("La contraseña actual es incorrecta.");
        }

        usuario.setContrasenia(passwordEncoder.encode(dto.getNuevaContrasenia()));
        usuarioRepository.save(usuario);
        actionHistoryService.registrar(idUsuario, "PASSWORD_CAMBIADO",
                "El usuario cambió su contraseña");
        log.info("Contraseña cambiada para usuario {}", idUsuario);
    }

    @Transactional
    public void solicitarDesactivacion(Long idUsuario) {
        BeanUsuario usuario = buscarUsuarioPorId(idUsuario);
        tokenService.generarYEnviarToken(
                idUsuario, TipoToken.DESACTIVACION_CUENTA,
                usuario.getCorreo(), usuario.getNombreCompleto());
    }

    @Transactional
    public void desactivarCuenta(Long idUsuario, DesactiveAccountRequestDTO dto) {
        BeanUsuario usuario = buscarUsuarioPorId(idUsuario);

        if (!passwordEncoder.matches(dto.getContrasenia(), usuario.getContrasenia())) {
            throw new BusinessRuleException("Contraseña incorrecta. No se puede desactivar la cuenta.");
        }

        tokenService.validarYConsumir(idUsuario, dto.getCodigoToken(),
                TipoToken.DESACTIVACION_CUENTA);

        usuario.setEstado(EstadoUsuario.INACTIVO);
        usuarioRepository.save(usuario);
        actionHistoryService.registrar(idUsuario, "CUENTA_DESACTIVADA",
                "El usuario desactivó su propia cuenta");
        log.info("Cuenta desactivada: {}", usuario.getNombreUsuario());
    }

    private BeanUsuario buscarUsuarioPorId(Long idUsuario) {
        return usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Usuario no encontrado con id: " + idUsuario));
    }

    private UserProfileDTO mapToUserProfileDTO(BeanUsuario u, boolean tienePermiso) {
        return UserProfileDTO.builder()
                .idUsuario(u.getIdUsuario())
                .nombreUsuario(u.getNombreUsuario())
                .correo(u.getCorreo())
                .nombreCompleto(u.getNombreCompleto())
                .estado(u.getEstado().name())
                .fotoPerfil(encodeBase64(u.getFotoPerfil()))
                .fechaRegistro(u.getFechaRegistro())
                .tienePermisoDescarga(tienePermiso)
                .tipoAdministrador(
                        administradorRepository.findByUsuario_IdUsuario(u.getIdUsuario())
                                .map(a -> a.getTipoAdministrador().name())
                                .orElse(null))
                .build();
    }

    private String encodeBase64(byte[] data) {
        if (data == null || data.length == 0) return null;
        return Base64.getEncoder().encodeToString(data);
    }
}
