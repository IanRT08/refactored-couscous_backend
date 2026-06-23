package mx.edu.cenidet.estadias.repositorios.alerta;

import mx.edu.cenidet.estadias.modelos.alerta.BeanAlerta;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlertaRepository extends JpaRepository<BeanAlerta, Long> {

    //Alertas para visitantes (sin sesión)
    List<BeanAlerta> findByUsuarioIsNullOrderByFechaCreacionDesc();

    //Alertas para usuario registrado
    @Query("""
            SELECT a FROM BeanAlerta a
            WHERE a.usuario.idUsuario = :idUsuario
               OR a.usuario           IS NULL
            ORDER BY a.fechaCreacion DESC
            """)
    Page<BeanAlerta> findAlertasParaUsuario(@Param("idUsuario") Long idUsuario, Pageable pageable);

    //Filtro por tipo de alerta
    List<BeanAlerta> findByUsuario_IdUsuarioAndTipoOrderByFechaCreacionDesc(
            Long idUsuario,
            String tipo);

    //Limpieza de alertas antiguas (mantenimiento)
    //El AlertService puede programar la purga de alertas de sistema
    //más antiguas de N días para no crecer indefinidamente.
    void deleteByUsuarioIsNullAndTipo(String tipo);
}

