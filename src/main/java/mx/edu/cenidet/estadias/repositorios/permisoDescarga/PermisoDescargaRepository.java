package mx.edu.cenidet.estadias.repositorios.permisoDescarga;

import mx.edu.cenidet.estadias.modelos.permisosDescarga.BeanPermisoDescarga;
import mx.edu.cenidet.estadias.modelos.permisosDescarga.EstadoPermiso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PermisoDescargaRepository extends JpaRepository<BeanPermisoDescarga, Long> {

    // ── Módulo 7 — Guardia de acceso a la sección de reportes ─
    // El ReportController usa esto en cada petición para verificar
    // que el usuario tiene permiso ACTIVO antes de servir el reporte.
    // RN: "Solo los usuarios con permiso de descarga pueden ver esta sección"
    boolean existsByUsuario_IdUsuarioAndPermisoDescarga(Long idUsuario, EstadoPermiso permisoDescarga);

    // Recuperar el permiso activo de un usuario (para mostrarlo en su perfil)
    Optional<BeanPermisoDescarga> findByUsuario_IdUsuarioAndPermisoDescarga(
            Long idUsuario,
            EstadoPermiso permisoDescarga);

    // Buscar por solicitud de origen (usado al aprobar/rechazar desde AdminService)
    Optional<BeanPermisoDescarga> findBySolicitudDescarga_IdSolicitudDescarga(Long idSolicitudDescarga);
}

