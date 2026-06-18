package mx.edu.cenidet.estadias.services.alert;

import lombok.RequiredArgsConstructor;
import mx.edu.cenidet.estadias.dtos.alertas.AlertDTO;
import mx.edu.cenidet.estadias.dtos.alertas.AlertSettingsDTO;
import mx.edu.cenidet.estadias.dtos.comunes.PageResponseDTO;
import mx.edu.cenidet.estadias.excepciones.ResourceNotFoundException;
import mx.edu.cenidet.estadias.modelos.alerta.BeanAlerta;
import mx.edu.cenidet.estadias.repositorios.alerta.AlertaRepository;
import mx.edu.cenidet.estadias.repositorios.usuario.UsuarioRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AlertService {

    private final AlertaRepository alertaRepository;
    private final UsuarioRepository usuarioRepository;

    //Crear alerta de sistema
    @Transactional
    public BeanAlerta crearAlertaSistema(String tipo, String mensaje) {
        BeanAlerta alerta = BeanAlerta.builder()
                .tipo(tipo)
                .mensaje(mensaje)
                .usuario(null) // null = pública
                .build();
        return alertaRepository.save(alerta);
    }

    //Crear alerta personal para un usuario
    @Transactional
    public BeanAlerta crearAlertaUsuario(Long idUsuario, String tipo, String mensaje) {
        BeanAlerta alerta = BeanAlerta.builder()
                .tipo(tipo)
                .mensaje(mensaje)
                .usuario(usuarioRepository.getReferenceById(idUsuario))
                .build();
        return alertaRepository.save(alerta);
    }

    //Alertas para visitantes (sin sesión)
    @Transactional(readOnly = true)
    public List<AlertDTO> listarParaVisitante() {
        return alertaRepository.findByUsuarioIsNullOrderByFechaCreacionDesc()
                .stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    //Alertas para usuario registrado
    //Combina alertas personales + generales
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

    //Leer preferencias del usuario
    @Transactional(readOnly = true)
    public AlertSettingsDTO obtenerConfiguracion(Long idUsuario) {
        String pref = usuarioRepository.findById(idUsuario)
                .map(u -> u.getPreferenciasAlertas())
                .orElse("TODAS");
        return new AlertSettingsDTO(pref);
    }

    //Actualizar preferencias
    @Transactional
    public void actualizarConfiguracion(Long idUsuario, AlertSettingsDTO dto) {
        usuarioRepository.findById(idUsuario).ifPresent(u -> {
            u.setPreferenciasAlertas(dto.getPreferencia());
            usuarioRepository.save(u);
        });
    }

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

