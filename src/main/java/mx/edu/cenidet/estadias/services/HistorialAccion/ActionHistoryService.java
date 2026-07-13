package mx.edu.cenidet.estadias.services.HistorialAccion;

import lombok.RequiredArgsConstructor;
import mx.edu.cenidet.estadias.dtos.administrador.ActionHistorySummaryDTO;
import mx.edu.cenidet.estadias.dtos.comunes.PageResponseDTO;
import mx.edu.cenidet.estadias.excepciones.ResourceNotFoundException;
import mx.edu.cenidet.estadias.modelos.HistorialAccion.BeanHistorialAccion;
import mx.edu.cenidet.estadias.repositorios.HistorialAccion.HistorialAccionRepository;
import mx.edu.cenidet.estadias.repositorios.usuario.UsuarioRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ActionHistoryService {

    private final HistorialAccionRepository historialAccionRepository;
    private final UsuarioRepository usuarioRepository;

    //Registrar una acción
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrar(Long idUsuario, String tipoAccion, String descripcion) {
        BeanHistorialAccion acccion = BeanHistorialAccion.builder()
                .tipoAccion(tipoAccion)
                .descripcion(descripcion)
                // getReferenceById crea un proxy JPA sin SELECT adicional
                .usuario(usuarioRepository.getReferenceById(idUsuario))
                .build();
        historialAccionRepository.save(acccion);
    }

    //Historial de un usuario
    @Transactional(readOnly = true)
    public List<ActionHistorySummaryDTO> listarPorUsuario(Long idUsuario) {
        if (!usuarioRepository.existsById(idUsuario)) {
            throw new ResourceNotFoundException("Usuario no encontrado con id: " + idUsuario);
        }
        return historialAccionRepository
                .findByUsuario_IdUsuarioOrderByFechaAccionDesc(idUsuario)
                .stream().map(this::mapToDTO).collect(Collectors.toList());
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