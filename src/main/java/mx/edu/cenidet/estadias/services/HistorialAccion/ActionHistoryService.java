package mx.edu.cenidet.estadias.services.HistorialAccion;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.edu.cenidet.estadias.dtos.administrador.ActionHistorySummaryDTO;
import mx.edu.cenidet.estadias.dtos.comunes.PageResponseDTO;
import mx.edu.cenidet.estadias.excepciones.ResourceNotFoundException;
import mx.edu.cenidet.estadias.modelos.HistorialAccion.BeanHistorialAccion;
import mx.edu.cenidet.estadias.modelos.usuario.EstadoUsuario;
import mx.edu.cenidet.estadias.repositorios.HistorialAccion.HistorialAccionRepository;
import mx.edu.cenidet.estadias.repositorios.administrador.AdministradorRepository;
import mx.edu.cenidet.estadias.repositorios.usuario.UsuarioRepository;
import mx.edu.cenidet.estadias.services.envioCorreos.MailService;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class ActionHistoryService {

    private final HistorialAccionRepository historialAccionRepository;
    private final UsuarioRepository usuarioRepository;
    private final AdministradorRepository administradorRepository;
    private final MailService mailService;

    //Registrar una acción
    @Transactional(propagation = Propagation.REQUIRED)
    public void registrar(Long idUsuario, String tipoAccion, String descripcion) {
        BeanHistorialAccion acccion = BeanHistorialAccion.builder()
                .tipoAccion(tipoAccion)
                .descripcion(descripcion)
                // getReferenceById crea un proxy JPA sin SELECT adicional
                .usuario(usuarioRepository.getReferenceById(idUsuario))
                .build();
        historialAccionRepository.save(acccion);
    }

    //Historial de un usuario (paginado)
    @Transactional(readOnly = true)
    public PageResponseDTO<ActionHistorySummaryDTO> listarPorUsuario(Long idUsuario, Pageable pageable) {
        if (!usuarioRepository.existsById(idUsuario)) {
            throw new ResourceNotFoundException("Usuario no encontrado con id: " + idUsuario);
        }
        return PageResponseDTO.of(
                historialAccionRepository
                        .findByUsuario_IdUsuarioOrderByFechaAccionDesc(idUsuario, pageable)
                        .map(this::mapToDTO)
        );
    }

    //Historial global paginado
    @Transactional(readOnly = true)
    public PageResponseDTO<ActionHistorySummaryDTO> listarGlobal(Pageable pageable) {
        return PageResponseDTO.of(
                historialAccionRepository.findAllByOrderByFechaAccionDesc(pageable)
                        .map(this::mapToDTO)
        );
    }

    //Filtro por rango de fechas
    @Transactional(readOnly = true)
    public PageResponseDTO<ActionHistorySummaryDTO> listarPorRango(
            LocalDateTime inicio, LocalDateTime fin, Pageable pageable) {
        return PageResponseDTO.of(
                historialAccionRepository.findByFechaAccionBetweenOrderByFechaAccionDesc(
                        inicio, fin, pageable).map(this::mapToDTO)
        );
    }

    //Purga mensual automática: elimina registros anteriores al primer día del mes actual
    //y notifica por correo a todos los administradores activos
    @Scheduled(cron = "0 0 2 1 * *", zone = "America/Mexico_City")
    @Transactional
    public void purgarHistorialMensual() {
        try {
            LocalDateTime corte = LocalDate.now().withDayOfMonth(1).atStartOfDay();
            long total = historialAccionRepository.countByFechaAccionBefore(corte);
            if (total == 0) {
                log.info("Purga mensual del historial: sin registros anteriores a {}", corte);
                return;
            }
            int eliminados = historialAccionRepository.eliminarAnterioresA(corte);
            String fechaCorte = corte.format(
                    DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", new Locale("es", "MX")));
            log.info("Purga mensual del historial: {} registros eliminados (anteriores al {})", eliminados, fechaCorte);
            administradorRepository.findAllByOrderByUsuario_NombreUsuarioAsc()
                    .stream()
                    .filter(a -> EstadoUsuario.ACTIVO.equals(a.getUsuario().getEstado()))
                    .forEach(a -> mailService.enviarPurgeHistorialAdmin(
                            a.getUsuario().getCorreo(),
                            a.getUsuario().getNombreCompleto(),
                            eliminados,
                            fechaCorte));
        } catch (Exception e) {
            log.error("Error en purga mensual del historial: {}", e.getMessage(), e);
        }
    }

    private ActionHistorySummaryDTO mapToDTO(BeanHistorialAccion h) {
        return ActionHistorySummaryDTO.builder()
                .idAccion(h.getIdAccion())
                .tipoAccion(h.getTipoAccion())
                .descripcion(h.getDescripcion())
                .fechaAccion(h.getFechaAccion())
                .idUsuario(h.getUsuario().getIdUsuario())
                .nombreUsuario(h.getUsuario().getNombreUsuario())
                .nombreCompleto(h.getUsuario().getNombreCompleto())
                .build();
    }
}