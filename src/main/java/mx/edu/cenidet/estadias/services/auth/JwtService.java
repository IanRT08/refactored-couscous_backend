package mx.edu.cenidet.estadias.services.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Getter
    @Value("${jwt.expiration-ms:86400000}")
    private long expirationMs;

    public String generarToken(String subject, Map<String, Object> extraClaims) {
        return Jwts.builder()
                .claims(extraClaims)
                .subject(subject)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(getSigningKey())
                .compact();
    }

    public Claims extraerClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extraerUsername(String token) {
        return extraerClaims(token).getSubject();
    }

    public String extraerRol(String token) {
        return (String) extraerClaims(token).get("rol");
    }

    public boolean esTokenValido(String token, UserDetails userDetails) {
        try {
            String username = extraerUsername(token);
            Date expiracion = extraerClaims(token).getExpiration();
            return username.equals(userDetails.getUsername())
                    && expiracion.after(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }

    public boolean esTokenValidoSoloFirma(String token) {
        try {
            extraerClaims(token); //lanza excepción si la firma es inválida o expiró
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public List<String> extraerAutoridades(String token) {
        String rol = extraerRol(token); //claim "rol" generado en autenticar()
        if (rol == null || rol.isBlank()) return List.of();
        return List.of("ROLE_" + rol);
    }

}
