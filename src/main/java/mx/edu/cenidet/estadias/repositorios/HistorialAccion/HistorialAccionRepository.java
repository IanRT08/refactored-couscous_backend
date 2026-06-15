package mx.edu.cenidet.estadias.repositorios.HistorialAccion;

import mx.edu.cenidet.estadias.modelos.HistorialAccion.BeanHistorialAccion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface HistorialAccionRepository extends JpaRepository<BeanHistorialAccion, Long> {

    //Historial por usuario
    List<BeanHistorialAccion> findByUsuario_IdUsuarioOrderByFechaAccionDesc(Long idUsuario);

    //Historial global
    Page<BeanHistorialAccion> findAllByOrderByFechaAccionDesc(Pageable pageable);

    //Filtro por tipo de accion
    List<BeanHistorialAccion> findByUsuario_IdUsuarioAndTipoAccionOrderByFechaAccionDesc(Long idUsuario, String tipoAccion);

    //Filtro por rango de fechas
    Page<BeanHistorialAccion> findByFechaAccionBetweenOrderByFechaAccionDesc(LocalDateTime inicio, LocalDateTime fin ,Pageable pageable);

    //Filtro combinado de usuario y fechas
    List<BeanHistorialAccion> findByUsuario_IdUsuarioAndFechaAccionBetweenOrderByFechaAccionDesc(Long idUsuario, LocalDateTime inicio, LocalDateTime fin);
}
