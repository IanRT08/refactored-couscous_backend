package mx.edu.cenidet.estadias.repositorios.usuario;

import mx.edu.cenidet.estadias.modelos.usuario.BeanUsuario;
import mx.edu.cenidet.estadias.modelos.usuario.EstadoUsuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository <BeanUsuario, Long> {

    //Login
    Optional<BeanUsuario> findByNombreUsuario(String nombreUsuario);
    Optional<BeanUsuario> findByCorreo(String correo);

    //Registro
    boolean existsByNombreUsuario(String nombreUsuario);
    boolean existsByCorreo(String correo);

    //Tabla de usuarios con filtros
    //Devuelve usuarios según estado
    Page<BeanUsuario> findByEstado(EstadoUsuario estado, Pageable pageable);

    //Buscador (excluye cuentas de administrador)
    @Query("""
            SELECT u FROM BeanUsuario u
            WHERE (LOWER(u.nombreUsuario)   LIKE LOWER(CONCAT('%', :termino, '%'))
               OR  LOWER(u.correo)           LIKE LOWER(CONCAT('%', :termino, '%'))
               OR  LOWER(u.nombreCompleto)   LIKE LOWER(CONCAT('%', :termino, '%')))
              AND  (:estado IS NULL OR u.estado = :estado)
              AND  NOT EXISTS (SELECT a FROM BeanAdministrador a WHERE a.usuario = u)
            ORDER BY u.fechaRegistro DESC
            """)
    Page<BeanUsuario> buscarPorTerminoYEstado(@Param("termino") String termino, @Param("estado")  EstadoUsuario estado, Pageable pageable);

    //Bloquear la cuenta 15 minutos
    List<BeanUsuario> findByEstado(EstadoUsuario estado);

    //Solo el estado (evita cargar el BLOB de foto en cada petición autenticada)
    @Query("SELECT u.estado FROM BeanUsuario u WHERE u.nombreUsuario = :nombreUsuario")
    Optional<EstadoUsuario> findEstadoByNombreUsuario(@Param("nombreUsuario") String nombreUsuario);

    //Lookup por id inmutable — usado por JwtAuthenticationFilter tras cambio de nombreUsuario
    @Query("SELECT u.estado FROM BeanUsuario u WHERE u.idUsuario = :idUsuario")
    Optional<EstadoUsuario> findEstadoByIdUsuario(@Param("idUsuario") Long idUsuario);

}
