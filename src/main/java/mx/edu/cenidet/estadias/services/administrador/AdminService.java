package mx.edu.cenidet.estadias.services.administrador;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.edu.cenidet.estadias.dtos.administrador.*;
import mx.edu.cenidet.estadias.excepciones.BusinessRuleException;
import mx.edu.cenidet.estadias.excepciones.ResourceNotFoundException;
import mx.edu.cenidet.estadias.services.HistorialAccion.ActionHistoryService;
import mx.edu.cenidet.estadias.services.envioCorreos.MailService;
import org.springframework.stereotype.Service;
import mx.edu.cenidet.estadias.dtos.comunes.PageResponseDTO;
import mx.edu.cenidet.estadias.modelos.administrador.BeanAdministrador;
import mx.edu.cenidet.estadias.modelos.administrador.TipoAdministrador;
import mx.edu.cenidet.estadias.modelos.permisosDescarga.BeanPermisoDescarga;
import mx.edu.cenidet.estadias.modelos.permisosDescarga.EstadoPermiso;
import mx.edu.cenidet.estadias.modelos.usuario.BeanUsuario;
import mx.edu.cenidet.estadias.modelos.usuario.EstadoUsuario;
import mx.edu.cenidet.estadias.repositorios.administrador.AdministradorRepository;
import mx.edu.cenidet.estadias.repositorios.permisoDescarga.PermisoDescargaRepository;
import mx.edu.cenidet.estadias.repositorios.usuario.UsuarioRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminService {

    private final UsuarioRepository usuarioRepository;
    private final AdministradorRepository administradorRepository;
    private final PermisoDescargaRepository permisoDescargaRepository;
    private final ActionHistoryService actionHistoryService;
    private final MailService mailService;
    private final PasswordEncoder passwordEncoder;

    //Listar usuarios con búsqueda y filtros
    @Transactional(readOnly = true)
    public PageResponseDTO<UserSummaryDTO> listarUsuarios(
            String termino, EstadoUsuario estado, Pageable pageable) {

        String t = (termino == null || termino.isBlank()) ? "" : termino.trim();
        Page<BeanUsuario> pagina = usuarioRepository.buscarPorTerminoYEstado(t, estado, pageable);

        List<Long> ids = pagina.stream().map(BeanUsuario::getIdUsuario).toList();
        Set<Long> conPermiso = ids.isEmpty() ? Set.of() :
                permisoDescargaRepository.findUserIdsWithActivePermission(ids, EstadoPermiso.ACTIVO);

        return PageResponseDTO.of(pagina.map(u ->
                mapToListUserSummaryDTO(u, conPermiso.contains(u.getIdUsuario()))));
    }

    //Ver detalle de un usuario
    @Transactional(readOnly = true)
    public UserDetailDTO obtenerDetalle(Long idUsuario) {
        BeanUsuario usuario = buscarPorId(idUsuario);
        return mapToUserDetailDTO(usuario);
    }

    //Cambiar estado de un usuario
    @Transactional
    public UserSummaryDTO cambiarEstadoUsuario(Long idAdmin, Long idUsuario, UpdateUserStatusRequestDTO dto) {
        verificarEsAdmin(idAdmin);

        //Ningún administrador (ni el superadmin) puede cambiar su propio estado
        if (idAdmin.equals(idUsuario)) {
            throw new BusinessRuleException("No puedes cambiar tu propio estado.");
        }

        //SIN_CONFIRMAR es un estado transitorio del registro, no debe asignarse manualmente
        if (EstadoUsuario.SIN_CONFIRMAR.equals(dto.getNuevoEstado())) {
            throw new BusinessRuleException("No se puede asignar manualmente el estado SIN_CONFIRMAR.");
        }

        //Solo el superadministrador puede cambiar el estado de OTROS administradores
        if (administradorRepository.existsByUsuario_IdUsuario(idUsuario)) {
            verificarEsSuperAdmin(idAdmin);
        }

        BeanUsuario usuario = buscarPorId(idUsuario);
        EstadoUsuario estadoAnterior = usuario.getEstado();
        usuario.setEstado(dto.getNuevoEstado());
        //Si se reactiva un bloqueado o inactivo, resetear intentos fallidos
        if (EstadoUsuario.ACTIVO.equals(dto.getNuevoEstado())) {
            usuario.setIntentosFallidos(0);
            usuario.setFechaBloqueo(null);
        }
        usuarioRepository.save(usuario);
        actionHistoryService.registrar(idAdmin, "ESTADO_USUARIO_CAMBIADO",
                String.format("Usuario %s: %s → %s. Motivo: %s",
                        usuario.getNombreUsuario(), estadoAnterior, dto.getNuevoEstado(),
                        dto.getMotivo() != null ? dto.getMotivo() : "Sin motivo especificado"));

        log.info("Estado de usuario {} cambiado: {} → {}", usuario.getNombreUsuario(),
                estadoAnterior, dto.getNuevoEstado());
        return mapToUserSummaryDTO(usuario);
    }

    @Transactional
    public AdminSummaryDTO crearAdministrador(Long idSuperAdmin, CreateAdminRequestDTO dto) {
        verificarEsSuperAdmin(idSuperAdmin);

        //El superadmin debe confirmar la acción con su propia contraseña
        BeanUsuario superAdminActual = buscarPorId(idSuperAdmin);
        if (!passwordEncoder.matches(dto.getContraseniaConfirmacion(), superAdminActual.getContrasenia())) {
            throw new BusinessRuleException("Contraseña incorrecta. No se pudo confirmar la acción.");
        }

        if (TipoAdministrador.SUPERADMINISTRADOR.equals(dto.getTipoAdministrador())) {
            long countSuper = administradorRepository.countByTipoAdministradorAndUsuario_Estado(
                    TipoAdministrador.SUPERADMINISTRADOR, EstadoUsuario.ACTIVO);
            if (countSuper >= 5) {
                throw new BusinessRuleException("No se pueden tener más de 5 superadministradores activos.");
            }
        }

        if (usuarioRepository.existsByNombreUsuario(dto.getNombreUsuario())) {
            throw new BusinessRuleException("El nombre de usuario ya está en uso.");
        }
        if (usuarioRepository.existsByCorreo(dto.getCorreo())) {
            throw new BusinessRuleException("El correo ya está registrado.");
        }

        String passwordTemporal = generarPasswordTemporal();

        BeanUsuario nuevoUsuario = BeanUsuario.builder()
                .nombreUsuario(dto.getNombreUsuario())
                .correo(dto.getCorreo())
                .nombreCompleto(dto.getNombreCompleto())
                .contrasenia(passwordEncoder.encode(passwordTemporal))
                .estado(EstadoUsuario.ACTIVO)
                .intentosFallidos(0)
                .build();
        nuevoUsuario = usuarioRepository.save(nuevoUsuario);

        BeanAdministrador admin = BeanAdministrador.builder()
                .tipoAdministrador(dto.getTipoAdministrador())
                .usuario(nuevoUsuario)
                .build();
        admin = administradorRepository.save(admin);

        mailService.enviarCredencialesAdmin(
                nuevoUsuario.getCorreo(), nuevoUsuario.getNombreCompleto(), passwordTemporal);

        actionHistoryService.registrar(idSuperAdmin, "ADMIN_CREADO",
                "Nuevo administrador: " + nuevoUsuario.getNombreUsuario()
                        + " (" + dto.getTipoAdministrador() + ")");

        log.info("Administrador creado: {} ({})", nuevoUsuario.getNombreUsuario(),
                dto.getTipoAdministrador());
        return mapToAdminSummaryDTO(admin, nuevoUsuario);
    }

    //Listar administradores
    @Transactional(readOnly = true)
    public List<AdminSummaryDTO> listarAdministradores(Long idSuperAdmin) {
        verificarEsSuperAdmin(idSuperAdmin);
        return administradorRepository.findAllByOrderByUsuario_NombreUsuarioAsc()
                .stream()
                .map(a -> mapToAdminSummaryDTO(a, a.getUsuario()))
                .collect(Collectors.toList());
    }

    //Editar permiso de descarga de un usuario
    @Transactional
    public void cambiarPermisoDescarga(Long idAdmin, Long idUsuario, UpdateDownloadPermissionRequestDTO dto) {
        verificarEsAdmin(idAdmin);
        BeanUsuario usuario = buscarPorId(idUsuario);

        Optional<BeanPermisoDescarga> permisoActivo = permisoDescargaRepository
                .findByUsuario_IdUsuarioAndPermisoDescarga(idUsuario, EstadoPermiso.ACTIVO);

        if (EstadoPermiso.ACTIVO.equals(dto.getNuevoPermiso())) {
            if (permisoActivo.isEmpty()) {
                BeanPermisoDescarga nuevo = BeanPermisoDescarga.builder()
                        .permisoDescarga(EstadoPermiso.ACTIVO)
                        .usuario(usuario)
                        .solicitudDescarga(null)
                        .build();
                permisoDescargaRepository.save(nuevo);
            }
        } else {
            permisoActivo.ifPresent(p -> {
                p.setPermisoDescarga(EstadoPermiso.INACTIVO);
                permisoDescargaRepository.save(p);
            });
        }

        actionHistoryService.registrar(idAdmin, "PERMISO_DESCARGA_CAMBIADO",
                String.format("Usuario %s: permiso de descarga → %s. Motivo: %s",
                        usuario.getNombreUsuario(), dto.getNuevoPermiso(),
                        dto.getMotivo() != null ? dto.getMotivo() : "Sin motivo especificado"));

        log.info("Permiso de descarga de {} cambiado a {}", usuario.getNombreUsuario(), dto.getNuevoPermiso());
    }

    @Transactional
    public AdminSummaryDTO cambiarTipoAdministrador(Long idSuperAdmin, Long idAdministrador, UpdateAdminTypeRequestDTO dto) {
        verificarEsSuperAdmin(idSuperAdmin);

        BeanAdministrador admin = administradorRepository.findById(idAdministrador)
                .orElseThrow(() -> new ResourceNotFoundException("Administrador no encontrado."));

        if (idSuperAdmin.equals(admin.getUsuario().getIdUsuario())) {
            throw new BusinessRuleException("No puedes cambiar tu propio tipo de administrador.");
        }

        if (TipoAdministrador.SUPERADMINISTRADOR.equals(dto.getNuevoTipo())) {
            long countSuper = administradorRepository.countByTipoAdministradorAndUsuario_Estado(
                    TipoAdministrador.SUPERADMINISTRADOR, EstadoUsuario.ACTIVO);
            if (countSuper >= 5) {
                throw new BusinessRuleException("No se pueden tener más de 5 superadministradores activos.");
            }
        }

        TipoAdministrador tipoAnterior = admin.getTipoAdministrador();
        admin.setTipoAdministrador(dto.getNuevoTipo());
        administradorRepository.save(admin);

        actionHistoryService.registrar(idSuperAdmin, "TIPO_ADMIN_CAMBIADO",
                String.format("Admin %s: %s → %s",
                        admin.getUsuario().getNombreUsuario(), tipoAnterior, dto.getNuevoTipo()));

        log.info("Tipo de administrador {} cambiado: {} → {}",
                admin.getUsuario().getNombreUsuario(), tipoAnterior, dto.getNuevoTipo());

        return mapToAdminSummaryDTO(admin, admin.getUsuario());
    }

    private void verificarEsAdmin(Long idUsuario) {
        if (!administradorRepository.existsByUsuario_IdUsuario(idUsuario)) {
            throw new BusinessRuleException("No tienes permisos de administrador.");
        }
    }

    private void verificarEsSuperAdmin(Long idUsuario) {
        administradorRepository.findByUsuario_IdUsuario(idUsuario)
                .filter(a -> TipoAdministrador.SUPERADMINISTRADOR.equals(a.getTipoAdministrador()))
                .orElseThrow(() -> new BusinessRuleException(
                        "Solo el Superadministrador puede realizar esta acción."));
    }

    private BeanUsuario buscarPorId(Long idUsuario) {
        return usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Usuario no encontrado con id: " + idUsuario));
    }

    private String generarPasswordTemporal() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789@$!";
        SecureRandom random = new SecureRandom();
        return random.ints(12, 0, chars.length())
                .mapToObj(i -> String.valueOf(chars.charAt(i)))
                .collect(Collectors.joining());
    }

    private UserSummaryDTO mapToUserSummaryDTO(BeanUsuario u) {
        boolean esAdmin = administradorRepository.existsByUsuario_IdUsuario(u.getIdUsuario());
        String tipo = null;
        if (esAdmin) {
            tipo = administradorRepository.findByUsuario_IdUsuario(u.getIdUsuario())
                    .map(a -> a.getTipoAdministrador().name()).orElse(null);
        }
        boolean tienePermiso = permisoDescargaRepository
                .existsByUsuario_IdUsuarioAndPermisoDescarga(u.getIdUsuario(), EstadoPermiso.ACTIVO);
        return UserSummaryDTO.builder()
                .idUsuario(u.getIdUsuario())
                .nombreUsuario(u.getNombreUsuario())
                .correo(u.getCorreo())
                .nombreCompleto(u.getNombreCompleto())
                .estado(u.getEstado().name())
                .fechaRegistro(u.getFechaRegistro())
                .esAdministrador(esAdmin)
                .tipoAdministrador(tipo)
                .tienePermisoDescarga(tienePermiso)
                .build();
    }

    private UserSummaryDTO mapToListUserSummaryDTO(BeanUsuario u, boolean tienePermiso) {
        // esAdministrador siempre false: el JPQL ya excluye usuarios vinculados a admins
        return UserSummaryDTO.builder()
                .idUsuario(u.getIdUsuario())
                .nombreUsuario(u.getNombreUsuario())
                .correo(u.getCorreo())
                .nombreCompleto(u.getNombreCompleto())
                .estado(u.getEstado().name())
                .fechaRegistro(u.getFechaRegistro())
                .esAdministrador(false)
                .tipoAdministrador(null)
                .tienePermisoDescarga(tienePermiso)
                .build();
    }

    private UserDetailDTO mapToUserDetailDTO(BeanUsuario u) {
        boolean esAdmin = administradorRepository.existsByUsuario_IdUsuario(u.getIdUsuario());
        String tipo = null;
        if (esAdmin) {
            tipo = administradorRepository.findByUsuario_IdUsuario(u.getIdUsuario())
                    .map(a -> a.getTipoAdministrador().name()).orElse(null);
        }
        String foto = (u.getFotoPerfil() != null && u.getFotoPerfil().length > 0)
                ? Base64.getEncoder().encodeToString(u.getFotoPerfil()) : null;
        boolean tienePermiso = permisoDescargaRepository
                .existsByUsuario_IdUsuarioAndPermisoDescarga(u.getIdUsuario(), EstadoPermiso.ACTIVO);
        return UserDetailDTO.builder()
                .idUsuario(u.getIdUsuario())
                .nombreUsuario(u.getNombreUsuario())
                .correo(u.getCorreo())
                .nombreCompleto(u.getNombreCompleto())
                .estado(u.getEstado().name())
                .fotoPerfil(foto)
                .fechaRegistro(u.getFechaRegistro())
                .esAdministrador(esAdmin)
                .tipoAdministrador(tipo)
                .tienePermisoDescarga(tienePermiso)
                .build();
    }

    private AdminSummaryDTO mapToAdminSummaryDTO(BeanAdministrador a, BeanUsuario u) {
        return AdminSummaryDTO.builder()
                .idAdministrador(a.getIdAdministrador())
                .idUsuario(u.getIdUsuario())
                .nombreUsuario(u.getNombreUsuario())
                .correo(u.getCorreo())
                .nombreCompleto(u.getNombreCompleto())
                .tipoAdministrador(a.getTipoAdministrador().name())
                .estadoUsuario(u.getEstado().name())
                .build();
    }
}
