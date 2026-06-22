package mx.edu.cenidet.estadias.services.stats;

import lombok.RequiredArgsConstructor;
import mx.edu.cenidet.estadias.dtos.estadisticas.ClimateStatisticsDTO;
import mx.edu.cenidet.estadias.dtos.estadisticas.ElectricStatisticsDTO;
import mx.edu.cenidet.estadias.dtos.estadisticas.StatsFilterDTO;
import mx.edu.cenidet.estadias.excepciones.BusinessRuleException;
import mx.edu.cenidet.estadias.repositorios.lectura.LecturaRepository;
import mx.edu.cenidet.estadias.repositorios.lecturaElectrica.LecturaElectricaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final LecturaRepository lecturaRepository;
    private final LecturaElectricaRepository electricaRepository;

    //Estadísticas climáticas
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
                .modaTemperatura(aDouble(lecturaRepository.modaTemperatura(filtro.getInicio(), filtro.getFin())))
                .promedioViento(stats.getPromedioViento())
                .maxViento(stats.getMaxViento())
                .minViento(stats.getMinViento())
                .modaViento(aDouble(lecturaRepository.modaViento(filtro.getInicio(), filtro.getFin())))
                .promedioHumedad(stats.getPromedioHumedad())
                .maxHumedad(stats.getMaxHumedad())
                .minHumedad(stats.getMinHumedad())
                .modaHumedad(aDouble(lecturaRepository.modaHumedad(filtro.getInicio(), filtro.getFin())))
                .promedioRadiacion(stats.getPromedioRadiacion())
                .maxRadiacion(stats.getMaxRadiacion())
                .minRadiacion(stats.getMinRadiacion())
                .modaRadiacion(aDouble(lecturaRepository.modaRadiacion(filtro.getInicio(), filtro.getFin())))
                .promedioPresion(stats.getPromedioPresion())
                .maxPresion(stats.getMaxPresion())
                .minPresion(stats.getMinPresion())
                .modaPresion(aDouble(lecturaRepository.modaPresion(filtro.getInicio(), filtro.getFin())))
                .build();
    }

    //Estadísticas eléctricas
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
                .modaCorriente(aDouble(electricaRepository.modaCorriente(filtro.getInicio(), filtro.getFin())))
                .promedioVoltaje(stats.getPromedioVoltaje())
                .maxVoltaje(stats.getMaxVoltaje())
                .minVoltaje(stats.getMinVoltaje())
                .modaVoltaje(aDouble(electricaRepository.modaVoltaje(filtro.getInicio(), filtro.getFin())))
                .promedioPotencia(stats.getPromedioPotencia())
                .maxPotencia(stats.getMaxPotencia())
                .minPotencia(stats.getMinPotencia())
                .modaPotencia(aDouble(electricaRepository.modaPotencia(filtro.getInicio(), filtro.getFin())))
                .promedioEnergia(stats.getPromedioEnergia())
                .maxEnergia(stats.getMaxEnergia())
                .minEnergia(stats.getMinEnergia())
                .modaEnergia(aDouble(electricaRepository.modaEnergia(filtro.getInicio(), filtro.getFin())))
                .build();
    }

    //Optional<Float> de las consultas nativas de moda -> Double para los DTO (null si no hay datos)
    private Double aDouble(Optional<Float> valor) {
        return valor.map(Float::doubleValue).orElse(null);
    }
}