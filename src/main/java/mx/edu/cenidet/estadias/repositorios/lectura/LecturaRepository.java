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

    //Idempotencia / no duplicar registros
    boolean existsByFechaLectura(LocalDateTime fechaLectura);

    //Dashboard: última lectura disponible
    Optional<BeanLectura> findTopByOrderByFechaLecturaDesc();

    //Auditoría del último timestamp guardado ─
    @Query("SELECT MAX(l.fechaLectura) FROM BeanLectura l")
    Optional<LocalDateTime> findMaxFechaLectura();

    //Fecha del dato más antiguo — para limitar el date picker del frontend
    @Query("SELECT MIN(l.fechaLectura) FROM BeanLectura l")
    Optional<LocalDateTime> findMinFechaLectura();

    //Gap Recovery: recuperar rango perdido
    //Devuelve lecturas en orden cronológico ascendente para que
    //el Service las procese en secuencia sin generar huecos.
    List<BeanLectura> findByFechaLecturaBetweenOrderByFechaLecturaAsc(
            LocalDateTime inicio,
            LocalDateTime fin);

    //Gap Recovery chequeo de duplicados en bloque
    //Una sola consulta por página en vez de un existsBy... por cada
    //registro descargado (evita N+1 contra la BD durante el respaldo inicial).
    @Query("SELECT l.fechaLectura FROM BeanLectura l WHERE l.fechaLectura BETWEEN :inicio AND :fin")
    List<LocalDateTime> findFechasByFechaLecturaBetween(
            @Param("inicio") LocalDateTime inicio,
            @Param("fin")    LocalDateTime fin);

    //Gráficas: datos históricos paginados
    //Canvas de Chart.js puede manejar 10k+ puntos, pero la API
    //expone paginación para que el frontend decida cuántos cargar.
    Page<BeanLectura> findByFechaLecturaBetween(
            LocalDateTime inicio,
            LocalDateTime fin,
            Pageable pageable);

    //Tabla pública: datos anteriores al mes en curso, sin filtros
    Page<BeanLectura> findByFechaLecturaLessThan(LocalDateTime limite, Pageable pageable);

    //Estadísticas climáticas agregadas ──────────
    //Una sola consulta calcula todos los agregados en la BD
    //en lugar de traer miles de filas a Java.
    //Retorna una projection interface definida abajo.
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

    //Reporte: datos para Excel/PDF
    //Sin paginación porque el reporte necesita el rango completo.
    //El ReportService lo llama solo cuando el usuario tiene permiso.
    List<BeanLectura> findByFechaLecturaBetween(LocalDateTime inicio, LocalDateTime fin);

    //Moda por variable
    @Query(value = "SELECT temperatura FROM Lectura " +
            "WHERE fecha_lectura BETWEEN :inicio AND :fin AND temperatura IS NOT NULL " +
            "GROUP BY temperatura ORDER BY COUNT(*) DESC, temperatura ASC LIMIT 1", nativeQuery = true)
    Optional<Float> modaTemperatura(@Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);

    @Query(value = "SELECT viento FROM Lectura " +
            "WHERE fecha_lectura BETWEEN :inicio AND :fin AND viento IS NOT NULL " +
            "GROUP BY viento ORDER BY COUNT(*) DESC, viento ASC LIMIT 1", nativeQuery = true)
    Optional<Float> modaViento(@Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);

    @Query(value = "SELECT humedad FROM Lectura " +
            "WHERE fecha_lectura BETWEEN :inicio AND :fin AND humedad IS NOT NULL " +
            "GROUP BY humedad ORDER BY COUNT(*) DESC, humedad ASC LIMIT 1", nativeQuery = true)
    Optional<Float> modaHumedad(@Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);

    @Query(value = "SELECT radiacion FROM Lectura " +
            "WHERE fecha_lectura BETWEEN :inicio AND :fin AND radiacion IS NOT NULL " +
            "GROUP BY radiacion ORDER BY COUNT(*) DESC, radiacion ASC LIMIT 1", nativeQuery = true)
    Optional<Float> modaRadiacion(@Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);

    @Query(value = "SELECT presion FROM Lectura " +
            "WHERE fecha_lectura BETWEEN :inicio AND :fin AND presion IS NOT NULL " +
            "GROUP BY presion ORDER BY COUNT(*) DESC, presion ASC LIMIT 1", nativeQuery = true)
    Optional<Float> modaPresion(@Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);

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