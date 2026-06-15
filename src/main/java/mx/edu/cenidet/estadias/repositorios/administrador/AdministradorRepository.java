package mx.edu.cenidet.estadias.repositorios.administrador;

import mx.edu.cenidet.estadias.modelos.administrador.BeanAdministrador;
import mx.edu.cenidet.estadias.modelos.administrador.TipoAdministrador;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

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

}
