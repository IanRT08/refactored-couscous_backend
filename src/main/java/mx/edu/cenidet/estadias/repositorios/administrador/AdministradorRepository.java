package mx.edu.cenidet.estadias.repositorios.administrador;

import mx.edu.cenidet.estadias.modelos.administrador.BeanAdministrador;
import mx.edu.cenidet.estadias.modelos.administrador.TipoAdministrador;
import mx.edu.cenidet.estadias.modelos.usuario.EstadoUsuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository
public interface AdministradorRepository extends JpaRepository<BeanAdministrador, Long> {

    //Autorizacion post-login
    //Verifica si tiene registro de administrador y redirige al dashboard correcto
    Optional<BeanAdministrador> findByUsuario_IdUsuario(Long idUsuario);
    boolean existsByUsuario_IdUsuario(Long idUsuario);

    //Gestion desde Superadmin
    List<BeanAdministrador> findByTipoAdministrador(TipoAdministrador tipoAdministrador);

    //Mostrar todos los admins
    List<BeanAdministrador> findAllByOrderByUsuario_NombreUsuarioAsc();
    Page<BeanAdministrador> findAllByOrderByUsuario_NombreUsuarioAsc(Pageable pageable);

    //Contar superadmins activos (para limitar a 5)
    long countByTipoAdministradorAndUsuario_Estado(TipoAdministrador tipo, EstadoUsuario estado);

}
