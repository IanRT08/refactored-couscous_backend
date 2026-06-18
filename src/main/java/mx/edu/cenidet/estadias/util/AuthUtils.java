package mx.edu.cenidet.estadias.util;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import mx.edu.cenidet.estadias.excepciones.BusinessRuleException;
import mx.edu.cenidet.estadias.services.auth.JwtService;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthUtils {

    private final JwtService jwtService;

    public Long getIdUsuarioActual(HttpServletRequest request) {
        String token = extraerToken(request);
        Object id = jwtService.extraerClaims(token).get("idUsuario");
        if (id == null) {
            throw new BusinessRuleException("El token no contiene información de usuario.");
        }
        return ((Number) id).longValue();
    }

    public String getRolActual(HttpServletRequest request) {
        return jwtService.extraerRol(extraerToken(request));
    }

    private String extraerToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith("Bearer ")) {
            throw new BusinessRuleException("Token de autenticación no proporcionado.");
        }
        return header.substring(7);
    }

}
