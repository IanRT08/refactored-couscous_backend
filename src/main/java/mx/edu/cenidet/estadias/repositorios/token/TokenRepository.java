package mx.edu.cenidet.estadias.repositorios.token;

import mx.edu.cenidet.estadias.modelos.token.BeanToken;
import mx.edu.cenidet.estadias.modelos.token.TipoToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

public interface TokenRepository extends JpaRepository<BeanToken, Integer> {

    //Recuperacion de contraseña
    @Query("""
            SELECT t FROM BeanToken t
            WHERE t.usuario.idUsuario = :idUsuario
              AND t.tipo              = :tipo
              AND t.usado             = false
              AND t.fechaExpiracion   > :ahora
            """)
    Optional<BeanToken> findTokenVigente(@Param("idUsuario") Long idUsuario, @Param("tipo") TipoToken tipo, @Param("ahora") LocalDateTime ahora);

    //Reenvio de token
    @Query("""
            SELECT t FROM BeanToken t
            WHERE t.usuario.idUsuario = :idUsuario
              AND t.tipo              = :tipo
            ORDER BY t.fechaExpiracion DESC
            LIMIT 1
            """)
    Optional<BeanToken> findUltimoTokenPorTipo(@Param("idUsuario") Long idUsuario, @Param("tipo") TipoToken tipo);

    @Transactional
    @Modifying
    void deleteByFechaExpiracionBefore(LocalDateTime fechaLimite);

}
