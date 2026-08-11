package mx.edu.cenidet.estadias.security;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Limita la tasa de peticiones por IP en los endpoints de autenticación
 * para mitigar ataques de fuerza bruta y abuso de recursos.
 *
 * Ventana deslizante de 1 minuto por IP y ruta.
 */
@Component
@Order(1)
@Slf4j
public class RateLimitingFilter implements Filter {

    // Ruta -> [maxPeticiones, ventanaMs]
    private static final Map<String, int[]> LIMITES = Map.of(
        "/api/auth/login",             new int[]{5,  60_000},
        "/api/auth/forgot-password",   new int[]{3,  60_000},
        "/api/auth/register",          new int[]{5,  60_000},
        "/api/auth/verify-reset-code", new int[]{10, 60_000},
        "/api/auth/verify-account",    new int[]{10, 60_000}
    );

    // IPs de proxies confiables (ej. nginx en localhost → "127.0.0.1").
    // Solo se lee X-Forwarded-For cuando la petición viene de una de estas IPs.
    // Si está vacío, solo se usa getRemoteAddr() — seguro sin proxy delante.
    @Value("${rate-limiting.trusted-proxies:}")
    private String trustedProxiesConfig;

    private Set<String> trustedProxies = Set.of();

    @PostConstruct
    public void init() {
        if (trustedProxiesConfig != null && !trustedProxiesConfig.isBlank()) {
            trustedProxies = Arrays.stream(trustedProxiesConfig.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toUnmodifiableSet());
            log.info("RateLimiter: {} proxy(ies) de confianza: {}", trustedProxies.size(), trustedProxies);
        }
    }

    private final ConcurrentHashMap<String, ArrayDeque<Long>> buckets = new ConcurrentHashMap<>();

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpReq = (HttpServletRequest) req;
        String path = httpReq.getRequestURI();

        int[] config = LIMITES.get(path);
        if (config == null) {
            chain.doFilter(req, res);
            return;
        }

        int limite = config[0];
        long ventanaMs = config[1];
        String ip = resolverIp(httpReq);
        String key = ip + ":" + path;

        ArrayDeque<Long> deque = buckets.computeIfAbsent(key, k -> new ArrayDeque<>());
        long ahora = System.currentTimeMillis();
        boolean permitido;

        synchronized (deque) {
            // Eliminar timestamps fuera de la ventana
            while (!deque.isEmpty() && deque.peekFirst() < ahora - ventanaMs) {
                deque.pollFirst();
            }
            permitido = deque.size() < limite;
            if (permitido) deque.addLast(ahora);
        }

        if (!permitido) {
            log.warn("Rate limit alcanzado: ip={} ruta={}", ip, path);
            HttpServletResponse httpRes = (HttpServletResponse) res;
            httpRes.setStatus(429);
            httpRes.setContentType("application/json;charset=UTF-8");
            httpRes.getWriter().write(
                "{\"exitoso\":false,\"mensaje\":\"Demasiadas solicitudes. Espera un momento e inténtalo de nuevo.\"}");
            return;
        }

        chain.doFilter(req, res);
    }

    private String resolverIp(HttpServletRequest req) {
        String remoteAddr = req.getRemoteAddr();
        if (!trustedProxies.isEmpty() && trustedProxies.contains(remoteAddr)) {
            String xff = req.getHeader("X-Forwarded-For");
            if (xff != null && !xff.isBlank()) {
                return xff.split(",")[0].trim();
            }
        }
        return remoteAddr;
    }
}
