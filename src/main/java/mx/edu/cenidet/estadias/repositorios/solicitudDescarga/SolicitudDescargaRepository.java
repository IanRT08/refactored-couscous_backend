package mx.edu.cenidet.estadias.repositorios.solicitudDescarga;

import mx.edu.cenidet.estadias.modelos.solicitudDescarga.EstadoSolicitud;
import mx.edu.cenidet.estadias.modelos.solicitudDescarga.BeanSolicitudDescarga;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SolicitudDescargaRepository extends JpaRepository<BeanSolicitudDescarga, Long> {

    //Restricción: una sola solicitud activa
    boolean existsByUsuario_IdUsuarioAndEstadoAndFechaSolicitudAfter(
            Long idUsuario,
            EstadoSolicitud estado,
            LocalDateTime fechaLimite);

    //Historial de solicitudes del usuario
    List<BeanSolicitudDescarga> findByUsuario_IdUsuarioOrderByFechaSolicitudDesc(Long idUsuario);

    //Solicitud PENDIENTE más reciente (para mostrar estado actual al usuario)
    Optional<BeanSolicitudDescarga> findFirstByUsuario_IdUsuarioAndEstadoOrderByFechaSolicitudDesc(
            Long idUsuario,
            EstadoSolicitud estado);

    //Tabla de solicitudes para el Admin
    //Paginada y filtrables por estado
    Page<BeanSolicitudDescarga> findByEstadoOrderByFechaSolicitudDesc(
            EstadoSolicitud estado,
            Pageable pageable);

    //Todas las solicitudes (sin filtro de estado) para el admin
    Page<BeanSolicitudDescarga> findAllByOrderByFechaSolicitudDesc(Pageable pageable);
}
