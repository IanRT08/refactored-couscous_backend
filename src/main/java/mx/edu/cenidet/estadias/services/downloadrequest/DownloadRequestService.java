package mx.edu.cenidet.estadias.services.downloadrequest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import mx.edu.cenidet.estadias.dtos.comunes.PageResponseDTO;
import mx.edu.cenidet.estadias.dtos.downloadrequest.CreateDownloadRequestDTO;
import mx.edu.cenidet.estadias.dtos.downloadrequest.DownloadRequestDetailDTO;
import mx.edu.cenidet.estadias.dtos.downloadrequest.DownloadRequestSummaryDTO;
import mx.edu.cenidet.estadias.dtos.downloadrequest.ResolveDownloadRequestDTO;
import mx.edu.cenidet.estadias.excepciones.BusinessRuleException;
import mx.edu.cenidet.estadias.excepciones.ResourceNotFoundException;
import mx.edu.cenidet.estadias.modelos.permisosDescarga.BeanPermisoDescarga;
import mx.edu.cenidet.estadias.modelos.permisosDescarga.EstadoPermiso;
import mx.edu.cenidet.estadias.modelos.solicitudDescarga.BeanSolicitudDescarga;
import mx.edu.cenidet.estadias.modelos.solicitudDescarga.EstadoSolicitud;
import mx.edu.cenidet.estadias.modelos.usuario.BeanUsuario;
import mx.edu.cenidet.estadias.repositorios.permisoDescarga.PermisoDescargaRepository;
import mx.edu.cenidet.estadias.repositorios.solicitudDescarga.SolicitudDescargaRepository;
import mx.edu.cenidet.estadias.repositorios.usuario.UsuarioRepository;
import mx.edu.cenidet.estadias.services.envioCorreos.MailService;
import mx.edu.cenidet.estadias.services.HistorialAccion.ActionHistoryService;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

// Módulos 7 y 2.5 — Solicitudes de permiso de descarga.
// DEPENDENCIAS CRUZADAS (Parte A, ya mergeada):
//   · ActionHistoryService — para registrar auditoría
//   · MailService          — para notificar al usuario del resultado
@Service
@RequiredArgsConstructor
@Slf4j
public class DownloadRequestService {

    private final SolicitudDescargaRepository solicitudRepository;
    private final PermisoDescargaRepository permisoRepository;
    private final UsuarioRepository usuarioRepository;
    private final ActionHistoryService        actionHistoryService; // ← Parte A
    private final MailService                 mailService;          // ← Parte A

    // ── Módulo 7.1 — El usuario crea una solicitud ───────────
    @Transactional
    public DownloadRequestSummaryDTO crearSolicitud(Long idUsuario,
                                                    CreateDownloadRequestDTO dto) {
        // RN 7.1: "El usuario solo puede tener una solicitud activa"
        //         "Las solicitudes tienen 7 días para ser aprobadas"
        boolean tieneActiva = solicitudRepository
                .existsByUsuario_IdUsuarioAndEstadoAndFechaSolicitudAfter(
                        idUsuario,
                        EstadoSolicitud.PENDIENTE,
                        LocalDateTime.now().minusDays(7));

        if (tieneActiva) {
            throw new BusinessRuleException(
                    "Ya tienes una solicitud pendiente. Espera la resolución o que pasen 7 días para enviar otra.");
        }

        BeanSolicitudDescarga solicitud = BeanSolicitudDescarga.builder()
                .motivo(dto.getMotivo())
                .estado(EstadoSolicitud.PENDIENTE)
                .usuario(usuarioRepository.getReferenceById(idUsuario))
                .build();

        solicitud = solicitudRepository.save(solicitud);
        actionHistoryService.registrar(idUsuario, "SOLICITUD_DESCARGA_CREADA",
                "Solicitud #" + solicitud.getIdSolicitudDescarga() + " enviada.");
        log.info("Solicitud de descarga creada: usuario={}, id={}", idUsuario,
                solicitud.getIdSolicitudDescarga());

        return mapToSummaryDTO(solicitud);
    }

    // ── Historial de solicitudes del usuario ──────────────────
    @Transactional(readOnly = true)
    public List<DownloadRequestSummaryDTO> listarPorUsuario(Long idUsuario) {
        return solicitudRepository
                .findByUsuario_IdUsuarioOrderByFechaSolicitudDesc(idUsuario)
                .stream().map(this::mapToSummaryDTO).collect(Collectors.toList());
    }

    // ── Módulo 2.5 — Admin: tabla de todas las solicitudes ────
    @Transactional(readOnly = true)
    public PageResponseDTO<DownloadRequestDetailDTO> listarParaAdmin(
            EstadoSolicitud estado, Pageable pageable) {

        var pagina = (estado != null)
                ? solicitudRepository.findByEstadoOrderByFechaSolicitudDesc(estado, pageable)
                : solicitudRepository.findAllByOrderByFechaSolicitudDesc(pageable);

        return PageResponseDTO.of(pagina.map(this::mapToDetailDTO));
    }

    // ── Módulo 2.5 / 7 — Admin: aprobar o rechazar ────────────
    @Transactional
    public DownloadRequestDetailDTO resolverSolicitud(Long idAdmin,
                                                      ResolveDownloadRequestDTO dto) {
        // Validar que solo sea APROBADA o RECHAZADA
        if (EstadoSolicitud.PENDIENTE.equals(dto.getDecision())) {
            throw new BusinessRuleException("La decisión debe ser APROBADA o RECHAZADA.");
        }

        BeanSolicitudDescarga solicitud = solicitudRepository
                .findById(dto.getIdSolicitudDescarga())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Solicitud no encontrada: " + dto.getIdSolicitudDescarga()));

        if (!EstadoSolicitud.PENDIENTE.equals(solicitud.getEstado())) {
            throw new BusinessRuleException("Solo se pueden resolver solicitudes en estado PENDIENTE.");
        }

        solicitud.setEstado(dto.getDecision());
        solicitudRepository.save(solicitud);

        // Si se APRUEBA: desactivar permiso anterior y crear uno nuevo ACTIVO
        if (EstadoSolicitud.APROBADA.equals(dto.getDecision())) {
            permisoRepository.findByUsuario_IdUsuarioAndPermisoDescarga(
                            solicitud.getUsuario().getIdUsuario(), EstadoPermiso.ACTIVO)
                    .ifPresent(p -> {
                        p.setPermisoDescarga(EstadoPermiso.INACTIVO);
                        permisoRepository.save(p);
                    });

            BeanPermisoDescarga nuevoPermiso = BeanPermisoDescarga.builder()
                    .permisoDescarga(EstadoPermiso.ACTIVO)
                    .usuario(solicitud.getUsuario())
                    .solicitudDescarga(solicitud)
                    .build();
            permisoRepository.save(nuevoPermiso);
        }

        // Notificar al usuario por correo
        mailService.enviarResultadoSolicitud(
                solicitud.getUsuario().getCorreo(),
                solicitud.getUsuario().getNombreCompleto(),
                dto.getDecision(),
                dto.getComentario());

        actionHistoryService.registrar(idAdmin, "SOLICITUD_RESUELTA",
                "Solicitud #" + dto.getIdSolicitudDescarga()
                        + " → " + dto.getDecision()
                        + ". Motivo: " + (dto.getComentario() != null ? dto.getComentario() : "-"));

        log.info("Solicitud #{} resuelta: {}", dto.getIdSolicitudDescarga(), dto.getDecision());
        return mapToDetailDTO(solicitud);
    }

    // ── Verificar permiso activo ──────────────────────────────
    @Transactional(readOnly = true)
    public boolean tienePermisoActivo(Long idUsuario) {
        return permisoRepository.existsByUsuario_IdUsuarioAndPermisoDescarga(
                idUsuario, EstadoPermiso.ACTIVO);
    }

    // ── Mappers privados ──────────────────────────────────────
    private DownloadRequestSummaryDTO mapToSummaryDTO(BeanSolicitudDescarga s) {
        String estadoPermiso = permisoRepository
                .findBySolicitudDescarga_IdSolicitudDescarga(s.getIdSolicitudDescarga())
                .map(p -> p.getPermisoDescarga().name())
                .orElse(null);

        return DownloadRequestSummaryDTO.builder()
                .idSolicitudDescarga(s.getIdSolicitudDescarga())
                .motivo(s.getMotivo())
                .estado(s.getEstado().name())
                .fechaSolicitud(s.getFechaSolicitud())
                .estadoPermiso(estadoPermiso)
                .build();
    }

    private DownloadRequestDetailDTO mapToDetailDTO(BeanSolicitudDescarga s) {
        BeanUsuario u = s.getUsuario();
        String estadoPermiso = permisoRepository
                .findBySolicitudDescarga_IdSolicitudDescarga(s.getIdSolicitudDescarga())
                .map(p -> p.getPermisoDescarga().name())
                .orElse(null);

        return DownloadRequestDetailDTO.builder()
                .idSolicitudDescarga(s.getIdSolicitudDescarga())
                .estado(s.getEstado().name())
                .motivo(s.getMotivo())
                .fechaSolicitud(s.getFechaSolicitud())
                .idUsuario(u.getIdUsuario())
                .nombreUsuario(u.getNombreUsuario())
                .correo(u.getCorreo())
                .nombreCompleto(u.getNombreCompleto())
                .estadoPermiso(estadoPermiso)
                .build();
    }
}
