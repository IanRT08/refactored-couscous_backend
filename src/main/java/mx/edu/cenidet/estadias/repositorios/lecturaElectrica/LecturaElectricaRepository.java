package mx.edu.cenidet.estadias.repositorios.lecturaElectrica;

import mx.edu.cenidet.estadias.modelos.lecturaElectrica.BeanLecturaElectrica;
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

    // ── Módulo 3.2 — Idempotencia ─────────────────────────────
    boolean existsByFechaLectura(LocalDateTime fechaLectura);

    // ── Módulo 4 — Dashboard: última lectura eléctrica ────────
    Optional<BeanLecturaElectrica> findTopByOrderByFechaLecturaDesc();

    // ── Gap Recovery ThingSpeak ────────────────────────────────
    @Query("SELECT MAX(le.fechaLectura) FROM BeanLecturaElectrica le")
    Optional<LocalDateTime> findMaxFechaLectura();

    // ── Módulo 3 — Gap Recovery: recuperar rango perdido ──────
    List<BeanLecturaElectrica> findByFechaLecturaBetweenOrderByFechaLecturaAsc(
            LocalDateTime inicio,
            LocalDateTime fin);

    // ── Módulo 5 — Gráficas eléctricas paginadas ──────────────
    Page<BeanLecturaElectrica> findByFechaLecturaBetween(
            LocalDateTime inicio,
            LocalDateTime fin,
            Pageable pageable);

    // ── Módulo 6 — Estadísticas eléctricas agregadas ──────────
    @Query("""
            SELECT AVG(le.corriente) AS promedioCorriente,
                   MAX(le.corriente) AS maxCorriente,
                   MIN(le.corriente) AS minCorriente,
                   AVG(le.voltaje)   AS promedioVoltaje,
                   MAX(le.voltaje)   AS maxVoltaje,
                   MIN(le.voltaje)   AS minVoltaje,
                   AVG(le.potencia)  AS promedioPotencia,
                   MAX(le.potencia)  AS maxPotencia,
                   MIN(le.potencia)  AS minPotencia,
                   AVG(le.energia)   AS promedioEnergia,
                   MAX(le.energia)   AS maxEnergia,
                   MIN(le.energia)   AS minEnergia
            FROM BeanLecturaElectrica le
            WHERE le.fechaLectura BETWEEN :inicio AND :fin
            """)
    Optional<EstadisticasElectricas> calcularEstadisticas(
            @Param("inicio") LocalDateTime inicio,
            @Param("fin")    LocalDateTime fin);

    // ── Módulo 7 — Reporte completo sin paginación ────────────
    List<BeanLecturaElectrica> findByFechaLecturaBetween(LocalDateTime inicio, LocalDateTime fin);

    // ── Módulo 6 — Moda por variable (ver nota en LecturaRepository) ──
    @Query(value = "SELECT corriente FROM LecturaElectrica " +
            "WHERE fechaLectura BETWEEN :inicio AND :fin AND corriente IS NOT NULL " +
            "GROUP BY corriente ORDER BY COUNT(*) DESC, corriente ASC LIMIT 1", nativeQuery = true)
    Optional<Float> modaCorriente(@Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);

    @Query(value = "SELECT voltaje FROM LecturaElectrica " +
            "WHERE fechaLectura BETWEEN :inicio AND :fin AND voltaje IS NOT NULL " +
            "GROUP BY voltaje ORDER BY COUNT(*) DESC, voltaje ASC LIMIT 1", nativeQuery = true)
    Optional<Float> modaVoltaje(@Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);

    @Query(value = "SELECT potencia FROM LecturaElectrica " +
            "WHERE fechaLectura BETWEEN :inicio AND :fin AND potencia IS NOT NULL " +
            "GROUP BY potencia ORDER BY COUNT(*) DESC, potencia ASC LIMIT 1", nativeQuery = true)
    Optional<Float> modaPotencia(@Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);

    @Query(value = "SELECT energia FROM LecturaElectrica " +
            "WHERE fechaLectura BETWEEN :inicio AND :fin AND energia IS NOT NULL " +
            "GROUP BY energia ORDER BY COUNT(*) DESC, energia ASC LIMIT 1", nativeQuery = true)
    Optional<Float> modaEnergia(@Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);

    // ─────────────────────────────────────────────────────────
    // Projection interface — Módulo 6
    // ─────────────────────────────────────────────────────────
    interface EstadisticasElectricas {
        Double getPromedioCorriente();
        Double getMaxCorriente();
        Double getMinCorriente();
        Double getPromedioVoltaje();
        Double getMaxVoltaje();
        Double getMinVoltaje();
        Double getPromedioPotencia();
        Double getMaxPotencia();
        Double getMinPotencia();
        Double getPromedioEnergia();
        Double getMaxEnergia();
        Double getMinEnergia();
    }
}
