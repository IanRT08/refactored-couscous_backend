package mx.edu.cenidet.estadias.repositorios.lecturaElectrica;

import mx.edu.cenidet.estadias.modelos.lecturaElectrica.BeanLecturaElectrica;
import mx.edu.cenidet.estadias.modelos.lecturaElectrica.FuenteElectrica;
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
public interface LecturaElectricaRepository extends JpaRepository<BeanLecturaElectrica, Long> {

    //Idempotencia (por canal/fuente)
    boolean existsByFechaLecturaAndFuente(LocalDateTime fechaLectura, FuenteElectrica fuente);

    //Dashboard / Gap Recovery: última lectura por fuente
    Optional<BeanLecturaElectrica> findTopByFuenteOrderByFechaLecturaDesc(FuenteElectrica fuente);

    //Fecha del dato más antiguo por fuente — para limitar el date picker del frontend
    @Query("SELECT MIN(le.fechaLectura) FROM BeanLecturaElectrica le WHERE le.fuente = :fuente")
    Optional<LocalDateTime> findMinFechaLectura(@Param("fuente") FuenteElectrica fuente);

    //Respaldo histórico — última lectura ANTES de una fecha dada
    //Necesario para encadenar el cálculo de energía cuando se rellenan
    //meses antiguos por debajo de datos que ya existen en la tabla.
    Optional<BeanLecturaElectrica> findTopByFuenteAndFechaLecturaLessThanOrderByFechaLecturaDesc(
            FuenteElectrica fuente, LocalDateTime fecha);

    //Gap Recovery — chequeo de duplicados en bloque
    //Una sola consulta por página en vez de un existsBy... por cada
    //registro descargado (evita N+1 contra la BD durante el respaldo inicial).
    @Query("SELECT le.fechaLectura FROM BeanLecturaElectrica le " +
            "WHERE le.fuente = :fuente AND le.fechaLectura BETWEEN :inicio AND :fin")
    List<LocalDateTime> findFechasByFuenteAndFechaLecturaBetween(
            @Param("fuente") FuenteElectrica fuente,
            @Param("inicio") LocalDateTime inicio,
            @Param("fin")    LocalDateTime fin);

    //Gráficas eléctricas paginadas (por fuente)
    Page<BeanLecturaElectrica> findByFuenteAndFechaLecturaBetween(
            FuenteElectrica fuente,
            LocalDateTime inicio,
            LocalDateTime fin,
            Pageable pageable);

    //Estadísticas eléctricas agregadas por fuente
    //energiaTotalPeriodo = SUM de los incrementos: es la métrica que sí
    //tiene sentido sumar entre Fotovoltaico y Eólico para la vista combinada.
    @Query("""
            SELECT AVG(le.voltaje)   AS promedioVoltaje,
                   MAX(le.voltaje)   AS maxVoltaje,
                   MIN(le.voltaje)   AS minVoltaje,
                   AVG(le.corriente) AS promedioCorriente,
                   MAX(le.corriente) AS maxCorriente,
                   MIN(le.corriente) AS minCorriente,
                   AVG(le.potencia)  AS promedioPotencia,
                   MAX(le.potencia)  AS maxPotencia,
                   MIN(le.potencia)  AS minPotencia,
                   AVG(le.voc)       AS promedioVoc,
                   MAX(le.voc)       AS maxVoc,
                   MIN(le.voc)       AS minVoc,
                   AVG(le.energia)   AS promedioEnergia,
                   MAX(le.energia)   AS maxEnergia,
                   MIN(le.energia)   AS minEnergia,
                   SUM(le.energia)   AS energiaTotalPeriodo
            FROM BeanLecturaElectrica le
            WHERE le.fuente = :fuente AND le.fechaLectura BETWEEN :inicio AND :fin
            """)
    Optional<EstadisticasElectricas> calcularEstadisticas(
            @Param("fuente") FuenteElectrica fuente,
            @Param("inicio") LocalDateTime inicio,
            @Param("fin")    LocalDateTime fin);

    //Reporte completo sin paginación (por fuente)
    List<BeanLecturaElectrica> findByFuenteAndFechaLecturaBetweenOrderByFechaLecturaAsc(
            FuenteElectrica fuente, LocalDateTime inicio, LocalDateTime fin);

    //Moda por variable y fuente
    //fuente se recibe como String (fuente.name()) para que el binding nativo
    //sea directo contra la columna VARCHAR, sin depender de cómo Hibernate
    //serialice un parámetro de tipo enum en consultas nativas.
    @Query(value = "SELECT voltaje FROM LecturaElectrica " +
            "WHERE fuente = :fuente AND fecha_lectura BETWEEN :inicio AND :fin AND voltaje IS NOT NULL " +
            "GROUP BY voltaje ORDER BY COUNT(*) DESC, voltaje ASC LIMIT 1", nativeQuery = true)
    Optional<Float> modaVoltaje(@Param("fuente") String fuente, @Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);

    @Query(value = "SELECT corriente FROM LecturaElectrica " +
            "WHERE fuente = :fuente AND fecha_lectura BETWEEN :inicio AND :fin AND corriente IS NOT NULL " +
            "GROUP BY corriente ORDER BY COUNT(*) DESC, corriente ASC LIMIT 1", nativeQuery = true)
    Optional<Float> modaCorriente(@Param("fuente") String fuente, @Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);

    @Query(value = "SELECT potencia FROM LecturaElectrica " +
            "WHERE fuente = :fuente AND fecha_lectura BETWEEN :inicio AND :fin AND potencia IS NOT NULL " +
            "GROUP BY potencia ORDER BY COUNT(*) DESC, potencia ASC LIMIT 1", nativeQuery = true)
    Optional<Float> modaPotencia(@Param("fuente") String fuente, @Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);

    @Query(value = "SELECT voc FROM LecturaElectrica " +
            "WHERE fuente = :fuente AND fecha_lectura BETWEEN :inicio AND :fin AND voc IS NOT NULL " +
            "GROUP BY voc ORDER BY COUNT(*) DESC, voc ASC LIMIT 1", nativeQuery = true)
    Optional<Float> modaVoc(@Param("fuente") String fuente, @Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);

    @Query(value = "SELECT energia FROM LecturaElectrica " +
            "WHERE fuente = :fuente AND fecha_lectura BETWEEN :inicio AND :fin AND energia IS NOT NULL " +
            "GROUP BY energia ORDER BY COUNT(*) DESC, energia ASC LIMIT 1", nativeQuery = true)
    Optional<Float> modaEnergia(@Param("fuente") String fuente, @Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);

    interface EstadisticasElectricas {
        Double getPromedioVoltaje();
        Double getMaxVoltaje();
        Double getMinVoltaje();
        Double getPromedioCorriente();
        Double getMaxCorriente();
        Double getMinCorriente();
        Double getPromedioPotencia();
        Double getMaxPotencia();
        Double getMinPotencia();
        Double getPromedioVoc();
        Double getMaxVoc();
        Double getMinVoc();
        Double getPromedioEnergia();
        Double getMaxEnergia();
        Double getMinEnergia();
        Double getEnergiaTotalPeriodo();
    }
}
