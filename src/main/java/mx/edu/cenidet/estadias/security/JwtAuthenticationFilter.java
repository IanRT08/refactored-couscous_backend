package mx.edu.cenidet.estadias.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import mx.edu.cenidet.estadias.modelos.usuario.EstadoUsuario;
import mx.edu.cenidet.estadias.repositorios.usuario.UsuarioRepository;
import mx.edu.cenidet.estadias.services.auth.JwtService;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;

    public JwtAuthenticationFilter(JwtService jwtService, UsuarioRepository usuarioRepository) {
        this.jwtService = jwtService;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest  request,
            HttpServletResponse response,
            FilterChain         filterChain) throws ServletException, IOException {

        //1. Extraer y validar cabecera
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String jwt = authHeader.substring(7).trim();

        //2. Validar firma + expiración del JWT
        //Usa esTokenValidoSoloFirma() (no requiere UserDetails) para
        //que cada petición no haga una consulta a BD.
        if (jwt.isEmpty() || !jwtService.esTokenValidoSoloFirma(jwt)) {
            filterChain.doFilter(request, response);
            return;
        }

        //3. Extraer identificador del subject (nombreUsuario)
        //El JWT fue generado con subject = nombreUsuario en AuthService.autenticar()
        String nombreUsuario = jwtService.extraerUsername(jwt);
        if (nombreUsuario == null || nombreUsuario.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        //3b. Verificar estado en BD: tokens de cuentas bloqueadas/inactivas quedan revocados.
        //Se usa idUsuario (claim inmutable) en lugar de nombreUsuario para evitar 401 tras cambiar el nombre.
        Long idUsuario = jwtService.extraerIdUsuario(jwt);
        boolean activo = idUsuario != null
                && usuarioRepository.findEstadoByIdUsuario(idUsuario)
                       .map(EstadoUsuario.ACTIVO::equals)
                       .orElse(false);
        if (!activo) {
            filterChain.doFilter(request, response);
            return;
        }

        //4. Construir lista de autoridades desde el claim "rol"
        //extraerAutoridades() lee el claim "rol" (ej: "ADMINISTRADOR") y
        //lo convierte a formato Spring Security "ROLE_ADMINISTRADOR".
        List<SimpleGrantedAuthority> authorities = jwtService.extraerAutoridades(jwt)
                .stream()
                .map(SimpleGrantedAuthority::new)
                .toList();

        //5. Crear token de autenticación y colocar en el contexto
        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(nombreUsuario, null, authorities);

        //details enriquece con IP y session-id para auditoría
        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        SecurityContextHolder.getContext().setAuthentication(authToken);

        //6. Continuar la cadena de filtros
        filterChain.doFilter(request, response);
    }
}
