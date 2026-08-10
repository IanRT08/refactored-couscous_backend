package mx.edu.cenidet.estadias.repositorios.permisoDescarga;

import mx.edu.cenidet.estadias.modelos.permisosDescarga.BeanPermisoDescarga;
import mx.edu.cenidet.estadias.modelos.permisosDescarga.EstadoPermiso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

@Repository
public interface PermisoDescargaRepository extends JpaRepository<BeanPermisoDescarga, Long> {

    //Guardia de acceso a la sección de reportes ─
    //El ReportController usa esto en cada petición para verificar
    //que el usuario tiene permiso ACTIVO antes de servir el reporte.
    boolean existsByUsuario_IdUsuarioAndPermisoDescarga(Long idUsuario, EstadoPermiso permisoDescarga);

    //Recuperar el permiso activo de un usuario (para mostrarlo en su perfil)
    Optional<BeanPermisoDescarga> findByUsuario_IdUsuarioAndPermisoDescarga(
            Long idUsuario,
            EstadoPermiso permisoDescarga);

    //Buscar por solicitud de origen (usado al aprobar/rechazar desde AdminService)
    Optional<BeanPermisoDescarga> findBySolicitudDescarga_IdSolicitudDescarga(Long idSolicitudDescarga);

    //Batch: IDs de usuarios con permiso activo (elimina N+1 en el listado paginado)
    @Query("SELECT p.usuario.idUsuario FROM BeanPermisoDescarga p WHERE p.usuario.idUsuario IN :ids AND p.permisoDescarga = :estado")
    Set<Long> findUserIdsWithActivePermission(@Param("ids") Collection<Long> ids, @Param("estado") EstadoPermiso estado);
}

