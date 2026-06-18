package mx.edu.cenidet.estadias.services.stats;

import lombok.RequiredArgsConstructor;
import mx.edu.cenidet.estadias.dtos.estadisticas.ClimateStatisticsDTO;
import mx.edu.cenidet.estadias.dtos.estadisticas.ElectricStatisticsDTO;
import mx.edu.cenidet.estadias.dtos.estadisticas.StatsFilterDTO;
import mx.edu.cenidet.estadias.repositorios.lectura.LecturaRepository;
import mx.edu.cenidet.estadias.repositorios.lecturaElectrica.LecturaElectricaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Módulo 6 — Cálculo de promedios, máximos y mínimos.
// Las consultas agregadas se ejecutan completamente en MySQL
// (a través de las @Query en los repositorios) para manejar
// eficientemente datasets de 10 000+ registros.
@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final LecturaRepository lecturaRepository;
    private final LecturaElectricaRepository electricaRepository;

    // ── Estadísticas climáticas ───────────────────────────────
    @Transactional(readOnly = true)
    public ClimateStatisticsDTO calcularClimaticas(StatsFilterDTO filtro) {
        // DFR CA 6.1: "Si no hay datos en el periodo → mensaje específico"
        LecturaRepository.EstadisticasClimaticas stats =
                lecturaRepository.calcularEstadisticas(filtro.getInicio(), filtro.getFin())
                        .filter(s -> s.getPromedioTemperatura() != null) // null = sin registros
                        .orElseThrow(() -> new BusinessRuleException(
                                "No hay datos para este periodo."));

        return ClimateStatisticsDTO.builder()
                .inicio(filtro.getInicio())
                .fin(filtro.getFin())
                .promedioTemperatura(stats.getPromedioTemperatura())
                .maxTemperatura(stats.getMaxTemperatura())
                .minTemperatura(stats.getMinTemperatura())
                .promedioViento(stats.getPromedioViento())
                .maxViento(stats.getMaxViento())
                .minViento(stats.getMinViento())
                .promedioHumedad(stats.getPromedioHumedad())
                .maxHumedad(stats.getMaxHumedad())
                .minHumedad(stats.getMinHumedad())
                .promedioRadiacion(stats.getPromedioRadiacion())
                .maxRadiacion(stats.getMaxRadiacion())
                .minRadiacion(stats.getMinRadiacion())
                .promedioPresion(stats.getPromedioPresion())
                .maxPresion(stats.getMaxPresion())
                .minPresion(stats.getMinPresion())
                .build();
    }

    // ── Estadísticas eléctricas ───────────────────────────────
    @Transactional(readOnly = true)
    public ElectricStatisticsDTO calcularElectricas(StatsFilterDTO filtro) {
        LecturaElectricaRepository.EstadisticasElectricas stats =
                electricaRepository.calcularEstadisticas(filtro.getInicio(), filtro.getFin())
                        .filter(s -> s.getPromedioCorriente() != null)
                        .orElseThrow(() -> new BusinessRuleException(
                                "No hay datos para este periodo."));

        return ElectricStatisticsDTO.builder()
                .inicio(filtro.getInicio())
                .fin(filtro.getFin())
                .promedioCorriente(stats.getPromedioCorriente())
                .maxCorriente(stats.getMaxCorriente())
                .minCorriente(stats.getMinCorriente())
                .promedioVoltaje(stats.getPromedioVoltaje())
                .maxVoltaje(stats.getMaxVoltaje())
                .minVoltaje(stats.getMinVoltaje())
                .promedioPotencia(stats.getPromedioPotencia())
                .maxPotencia(stats.getMaxPotencia())
                .minPotencia(stats.getMinPotencia())
                .promedioEnergia(stats.getPromedioEnergia())
                .maxEnergia(stats.getMaxEnergia())
                .minEnergia(stats.getMinEnergia())
                .build();
    }
}