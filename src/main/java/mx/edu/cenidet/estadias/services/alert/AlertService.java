package mx.edu.cenidet.estadias.services.alert;

import lombok.RequiredArgsConstructor;
import mx.edu.cenidet.estadias.dtos.alertas.AlertDTO;
import mx.edu.cenidet.estadias.dtos.alertas.AlertSettingsDTO;
import mx.edu.cenidet.estadias.dtos.comunes.PageResponseDTO;
import mx.edu.cenidet.estadias.modelos.alerta.BeanAlerta;
import mx.edu.cenidet.estadias.repositorios.alerta.AlertaRepository;
import mx.edu.cenidet.estadias.repositorios.usuario.UsuarioRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

// Módulo 8 — Gestión de alertas del sistema y de usuarios.
// SyncHealthService y DownloadRequestService llaman a este servicio
// para crear alertas sin pasar por el Controller.
//
// ⚠ CAMPO ADICIONAL REQUERIDO EN Usuario.java:
//   preferenciasAlertas (ya listado en el aviso al tope de ParteA_Services.java)
@Service
@RequiredArgsConstructor
public class AlertService {

    private final AlertaRepository alertaRepository;
    private final UsuarioRepository usuarioRepository;

    // ── Crear alerta de sistema (visible para todos) ──────────
    // Llamado por SyncHealthService cuando hay ≥3 fallos.
    // usuario = null → alerta general pública (Módulo 8.1 RN)
    @Transactional
    public BeanAlerta crearAlertaSistema(String tipo, String mensaje) {
        BeanAlerta alerta = BeanAlerta.builder()
                .tipo(tipo)
                .mensaje(mensaje)
                .usuario(null) // null = pública
                .build();
        return alertaRepository.save(alerta);
    }

    // ── Crear alerta personal para un usuario ────────────────
    // Llamado por DownloadRequestService al resolver solicitudes.
    @Transactional
    public BeanAlerta crearAlertaUsuario(Long idUsuario, String tipo, String mensaje) {
        BeanAlerta alerta = BeanAlerta.builder()
                .tipo(tipo)
                .mensaje(mensaje)
                .usuario(usuarioRepository.getReferenceById(idUsuario))
                .build();
        return alertaRepository.save(alerta);
    }

    // ── Módulo 8.1 — Alertas para visitantes (sin sesión) ────
    // DFR RN: "Los visitantes solo verán alertas muy generales del sistema"
    @Transactional(readOnly = true)
    public List<AlertDTO> listarParaVisitante() {
        return alertaRepository.findByUsuarioIsNullOrderByFechaCreacionDesc()
                .stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    // ── Módulo 8.1 — Alertas para usuario registrado ─────────
    // Combina alertas personales + generales (AlertaRepository @Query)
    @Transactional(readOnly = true)
    public PageResponseDTO<AlertDTO> listarParaUsuario(Long idUsuario, Pageable pageable) {
        if (!usuarioRepository.existsById(idUsuario)) {
            throw new ResourceNotFoundException("Usuario no encontrado: " + idUsuario);
        }
        return PageResponseDTO.of(
                alertaRepository.findAlertasParaUsuario(idUsuario, pageable)
                        .map(this::mapToDTO)
        );
    }

    // ── Módulo 8 — Leer preferencias del usuario ─────────────
    @Transactional(readOnly = true)
    public AlertSettingsDTO obtenerConfiguracion(Long idUsuario) {
        String pref = usuarioRepository.findById(idUsuario)
                .map(u -> u.getPreferenciasAlertas())
                .orElse("TODAS");
        return new AlertSettingsDTO(pref);
    }

    // ── Módulo 8 — Actualizar preferencias ───────────────────
    // DFR RN: "Las alertas se pueden desactivar en los ajustes de perfil"
    @Transactional
    public void actualizarConfiguracion(Long idUsuario, AlertSettingsDTO dto) {
        usuarioRepository.findById(idUsuario).ifPresent(u -> {
            u.setPreferenciasAlertas(dto.getPreferencia());
            usuarioRepository.save(u);
        });
    }

    // ── Mapper privado ────────────────────────────────────────
    private AlertDTO mapToDTO(BeanAlerta a) {
        return AlertDTO.builder()
                .idAlerta(a.getIdAlerta())
                .tipo(a.getTipo())
                .mensaje(a.getMensaje())
                .fechaCreacion(a.getFechaCreacion())
                .esGeneral(a.getUsuario() == null)
                .build();
    }
}

