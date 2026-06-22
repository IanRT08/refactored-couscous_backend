package mx.edu.cenidet.estadias.repositorios.lectura;


import mx.edu.cenidet.estadias.modelos.lectura.BeanLectura;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface LecturaRepository extends JpaRepository<BeanLectura, Long> {

    // ── Módulo 3.2 — Idempotencia / no duplicar registros ────
    // Antes de cada INSERT el SyncService consulta esto.
    // El constraint UNIQUE en la tabla es la red de seguridad;
    // este check evita el costo de lanzar una excepción de BD.
    boolean existsByFechaLectura(LocalDateTime fechaLectura);

    // ── Módulo 4 — Dashboard: última lectura disponible ───────
    Optional<BeanLectura> findTopByOrderByFechaLecturaDesc();

    // ── Gap Recovery — Auditoría del último timestamp guardado ─
    // GapRecoveryService llama esto al arrancar y al reconectar.
    @Query("SELECT MAX(l.fechaLectura) FROM BeanLectura l")
    Optional<LocalDateTime> findMaxFechaLectura();

    // ── Módulo 3 — Gap Recovery: recuperar rango perdido ──────
    // Devuelve lecturas en orden cronológico ascendente para que
    // el Service las procese en secuencia sin generar huecos.
    List<BeanLectura> findByFechaLecturaBetweenOrderByFechaLecturaAsc(
            LocalDateTime inicio,
            LocalDateTime fin);

    // ── Módulo 5 — Gráficas: datos históricos paginados ───────
    // Canvas de Chart.js puede manejar 10k+ puntos, pero la API
    // expone paginación para que el frontend decida cuántos cargar.
    Page<BeanLectura> findByFechaLecturaBetween(
            LocalDateTime inicio,
            LocalDateTime fin,
            Pageable pageable);

    // ── Módulo 6 — Estadísticas climáticas agregadas ──────────
    // Una sola consulta calcula todos los agregados en la BD
    // en lugar de traer miles de filas a Java.
    // Retorna una projection interface definida abajo.
    @Query("""
            SELECT AVG(l.temperatura) AS promedioTemperatura,
                   MAX(l.temperatura) AS maxTemperatura,
                   MIN(l.temperatura) AS minTemperatura,
                   AVG(l.viento)      AS promedioViento,
                   MAX(l.viento)      AS maxViento,
                   MIN(l.viento)      AS minViento,
                   AVG(l.humedad)     AS promedioHumedad,
                   MAX(l.humedad)     AS maxHumedad,
                   MIN(l.humedad)     AS minHumedad,
                   AVG(l.radiacion)   AS promedioRadiacion,
                   MAX(l.radiacion)   AS maxRadiacion,
                   MIN(l.radiacion)   AS minRadiacion,
                   AVG(l.presion)     AS promedioPresion,
                   MAX(l.presion)     AS maxPresion,
                   MIN(l.presion)     AS minPresion
            FROM BeanLectura l
            WHERE l.fechaLectura BETWEEN :inicio AND :fin
            """)
    Optional<EstadisticasClimaticas> calcularEstadisticas(
            @Param("inicio") LocalDateTime inicio,
            @Param("fin")    LocalDateTime fin);

    // ── Módulo 7 — Reporte: datos para Excel/PDF ──────────────
    // Sin paginación porque el reporte necesita el rango completo.
    // El ReportService lo llama solo cuando el usuario tiene permiso.
    List<BeanLectura> findByFechaLecturaBetween(LocalDateTime inicio, LocalDateTime fin);

    // ── Módulo 6 — Moda por variable ──────────────────────────
    // MySQL no tiene una función de agregado MODE(); se obtiene con
    // GROUP BY + COUNT(*) DESC. Se usa nativeQuery porque JPQL no
    // soporta "ORDER BY COUNT(*)" junto con un GROUP BY simple así.
    // Empate -> se desempata con el valor más pequeño (determinista).
    @Query(value = "SELECT temperatura FROM Lectura " +
            "WHERE fechaLectura BETWEEN :inicio AND :fin AND temperatura IS NOT NULL " +
            "GROUP BY temperatura ORDER BY COUNT(*) DESC, temperatura ASC LIMIT 1", nativeQuery = true)
    Optional<Float> modaTemperatura(@Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);

    @Query(value = "SELECT viento FROM Lectura " +
            "WHERE fechaLectura BETWEEN :inicio AND :fin AND viento IS NOT NULL " +
            "GROUP BY viento ORDER BY COUNT(*) DESC, viento ASC LIMIT 1", nativeQuery = true)
    Optional<Float> modaViento(@Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);

    @Query(value = "SELECT humedad FROM Lectura " +
            "WHERE fechaLectura BETWEEN :inicio AND :fin AND humedad IS NOT NULL " +
            "GROUP BY humedad ORDER BY COUNT(*) DESC, humedad ASC LIMIT 1", nativeQuery = true)
    Optional<Float> modaHumedad(@Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);

    @Query(value = "SELECT radiacion FROM Lectura " +
            "WHERE fechaLectura BETWEEN :inicio AND :fin AND radiacion IS NOT NULL " +
            "GROUP BY radiacion ORDER BY COUNT(*) DESC, radiacion ASC LIMIT 1", nativeQuery = true)
    Optional<Float> modaRadiacion(@Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);

    @Query(value = "SELECT presion FROM Lectura " +
            "WHERE fechaLectura BETWEEN :inicio AND :fin AND presion IS NOT NULL " +
            "GROUP BY presion ORDER BY COUNT(*) DESC, presion ASC LIMIT 1", nativeQuery = true)
    Optional<Float> modaPresion(@Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);

    // ─────────────────────────────────────────────────────────
    // Projection interface — Módulo 6
    // Los alias del SELECT de calcularEstadisticas mapean 1-a-1
    // con los nombres de getter (sin "get", camelCase).
    // ─────────────────────────────────────────────────────────
    interface EstadisticasClimaticas {
        Double getPromedioTemperatura();
        Double getMaxTemperatura();
        Double getMinTemperatura();
        Double getPromedioViento();
        Double getMaxViento();
        Double getMinViento();
        Double getPromedioHumedad();
        Double getMaxHumedad();
        Double getMinHumedad();
        Double getPromedioRadiacion();
        Double getMaxRadiacion();
        Double getMinRadiacion();
        Double getPromedioPresion();
        Double getMaxPresion();
        Double getMinPresion();
    }
}