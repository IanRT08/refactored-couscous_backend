package mx.edu.cenidet.estadias.config;

import mx.edu.cenidet.estadias.repositorios.usuario.UsuarioRepository;
import mx.edu.cenidet.estadias.security.JwtAccessDeniedHandler;
import mx.edu.cenidet.estadias.security.JwtAuthenticationEntryPoint;
import mx.edu.cenidet.estadias.security.JwtAuthenticationFilter;
import mx.edu.cenidet.estadias.services.auth.JwtService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import org.springframework.beans.factory.annotation.Value;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Value("#{'${cors.allowed-origins}'.split(',')}")
    private List<String> allowedOrigins;

    //Bean obligatorio para los Services que cifran contraseñas
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(allowedOrigins);
        config.setAllowedMethods(List.of(
                HttpMethod.GET.name(), HttpMethod.POST.name(),
                HttpMethod.PUT.name(), HttpMethod.DELETE.name(),
                HttpMethod.PATCH.name(), HttpMethod.OPTIONS.name()));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Authorization"));
        config.setAllowCredentials(false);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    //JwtAuthenticationFilter
    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(JwtService jwtService,
                                                           UsuarioRepository usuarioRepository) {
        return new JwtAuthenticationFilter(jwtService, usuarioRepository);
    }

    //SecurityFilterChain
    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint,
            JwtAccessDeniedHandler jwtAccessDeniedHandler) throws Exception {

        http
                //CORS centralizado (usa el bean corsConfigurationSource)
                .cors(Customizer.withDefaults())

                //Sin CSRF — la API usa JWT en header, no cookies de sesión
                .csrf(AbstractHttpConfigurer::disable)

                //Stateless: Spring no crea ni usa sesión HTTP
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                //Respuestas 401 y 403 en JSON (no HTML por defecto)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                        .accessDeniedHandler(jwtAccessDeniedHandler))

                //Filtro JWT antes del filtro usuario/contraseña clásico
                .addFilterBefore(jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class)

                //Reglas de acceso
                .authorizeHttpRequests(auth -> auth

                        //Auth: login, registro, verificación, recuperación
                        .requestMatchers("/api/auth/**").permitAll()

                        //Dashboard público: última telemetría + mapa
                        .requestMatchers(HttpMethod.GET, "/api/telemetria/resumen").permitAll()

                        //Tabla pública de datos históricos (anterior al mes en curso)
                        .requestMatchers(HttpMethod.GET, "/api/telemetria/publica/**").permitAll()

                        //Alertas generales del sistema (visitantes)
                        .requestMatchers(HttpMethod.GET, "/api/alertas/sistema").permitAll()

                        //CORS preflight: el navegador envía OPTIONS antes de la petición real
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        //Errores de Spring Boot (redirect interno a /error)
                        .requestMatchers("/error").permitAll()

                        //Jwt valido para lo demas
                        .anyRequest().authenticated()
                );

        return http.build();
    }


}
