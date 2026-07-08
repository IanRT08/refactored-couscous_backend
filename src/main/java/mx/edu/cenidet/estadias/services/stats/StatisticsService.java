package mx.edu.cenidet.estadias.services.stats;

import lombok.RequiredArgsConstructor;
import mx.edu.cenidet.estadias.dtos.estadisticas.ClimateStatisticsDTO;
import mx.edu.cenidet.estadias.dtos.estadisticas.ElectricCombinedStatisticsDTO;
import mx.edu.cenidet.estadias.dtos.estadisticas.ElectricStatisticsDTO;
import mx.edu.cenidet.estadias.dtos.estadisticas.StatsFilterDTO;
import mx.edu.cenidet.estadias.excepciones.BusinessRuleException;
import mx.edu.cenidet.estadias.modelos.lecturaElectrica.FuenteElectrica;
import mx.edu.cenidet.estadias.repositorios.lectura.LecturaRepository;
import mx.edu.cenidet.estadias.repositorios.lecturaElectrica.LecturaElectricaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
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

    //Estadísticas eléctricas de una fuente (Fotovoltaico o Eólico)
    @Transactional(readOnly = true)
    public ElectricStatisticsDTO calcularElectricas(StatsFilterDTO filtro, FuenteElectrica fuente) {
        return calcularElectricasInterno(filtro, fuente)
                .orElseThrow(() -> new BusinessRuleException(
                        "No hay datos para este periodo (" + fuente + ")."));
    }

    //Estadísticas combinadas del sistema híbrido. Solo potencia (promedio) y
    //energía (total) son sumables entre los dos canales; voltaje/corriente/Voc
    //se reportan únicamente desglosados por fuente (ver ElectricCombinedStatisticsDTO).
    @Transactional(readOnly = true)
    public ElectricCombinedStatisticsDTO calcularElectricasCombinadas(StatsFilterDTO filtro) {
        Optional<ElectricStatisticsDTO> fotovoltaico = calcularElectricasInterno(filtro, FuenteElectrica.FOTOVOLTAICO);
        Optional<ElectricStatisticsDTO> eolico = calcularElectricasInterno(filtro, FuenteElectrica.EOLICO);

        if (fotovoltaico.isEmpty() && eolico.isEmpty()) {
            throw new BusinessRuleException("No hay datos para este periodo.");
        }

        double potenciaCombinada = fotovoltaico.map(ElectricStatisticsDTO::getPromedioPotencia).orElse(0.0)
                + eolico.map(ElectricStatisticsDTO::getPromedioPotencia).orElse(0.0);
        double energiaCombinada = fotovoltaico.map(ElectricStatisticsDTO::getEnergiaTotalPeriodo).orElse(0.0)
                + eolico.map(ElectricStatisticsDTO::getEnergiaTotalPeriodo).orElse(0.0);

        return ElectricCombinedStatisticsDTO.builder()
                .inicio(filtro.getInicio())
                .fin(filtro.getFin())
                .potenciaPromedioCombinada(potenciaCombinada)
                .energiaTotalCombinada(energiaCombinada)
                .fotovoltaico(fotovoltaico.orElse(null))
                .eolico(eolico.orElse(null))
                .build();
    }

    private Optional<ElectricStatisticsDTO> calcularElectricasInterno(StatsFilterDTO filtro, FuenteElectrica fuente) {
        return electricaRepository.calcularEstadisticas(fuente, filtro.getInicio(), filtro.getFin())
                .filter(s -> s.getPromedioPotencia() != null)
                .map(stats -> {
                    String nombreFuente = fuente.name();
                    return ElectricStatisticsDTO.builder()
                            .inicio(filtro.getInicio())
                            .fin(filtro.getFin())
                            .fuente(fuente)
                            .promedioVoltaje(stats.getPromedioVoltaje())
                            .maxVoltaje(stats.getMaxVoltaje())
                            .minVoltaje(stats.getMinVoltaje())
                            .modaVoltaje(aDouble(electricaRepository.modaVoltaje(nombreFuente, filtro.getInicio(), filtro.getFin())))
                            .promedioCorriente(stats.getPromedioCorriente())
                            .maxCorriente(stats.getMaxCorriente())
                            .minCorriente(stats.getMinCorriente())
                            .modaCorriente(aDouble(electricaRepository.modaCorriente(nombreFuente, filtro.getInicio(), filtro.getFin())))
                            .promedioPotencia(stats.getPromedioPotencia())
                            .maxPotencia(stats.getMaxPotencia())
                            .minPotencia(stats.getMinPotencia())
                            .modaPotencia(aDouble(electricaRepository.modaPotencia(nombreFuente, filtro.getInicio(), filtro.getFin())))
                            .promedioVoc(stats.getPromedioVoc())
                            .maxVoc(stats.getMaxVoc())
                            .minVoc(stats.getMinVoc())
                            .modaVoc(aDouble(electricaRepository.modaVoc(nombreFuente, filtro.getInicio(), filtro.getFin())))
                            .promedioEnergia(stats.getPromedioEnergia())
                            .maxEnergia(stats.getMaxEnergia())
                            .minEnergia(stats.getMinEnergia())
                            .modaEnergia(aDouble(electricaRepository.modaEnergia(nombreFuente, filtro.getInicio(), filtro.getFin())))
                            .energiaTotalPeriodo(stats.getEnergiaTotalPeriodo())
                            .build();
                });
    }

    //Fechas mínimas disponibles por tipo de dato — para limitar los date pickers del frontend
    @Transactional(readOnly = true)
    public Map<String, String> obtenerFechasDisponibles() {
        Map<String, String> result = new LinkedHashMap<>();
        lecturaRepository.findMinFechaLectura()
            .map(ldt -> ldt.toLocalDate().toString())
            .ifPresent(d -> result.put("climaticaMin", d));
        electricaRepository.findMinFechaLectura(FuenteElectrica.FOTOVOLTAICO)
            .map(ldt -> ldt.toLocalDate().toString())
            .ifPresent(d -> result.put("fotovoltaicoMin", d));
        electricaRepository.findMinFechaLectura(FuenteElectrica.EOLICO)
            .map(ldt -> ldt.toLocalDate().toString())
            .ifPresent(d -> result.put("eolicoMin", d));
        result.put("hoy", LocalDate.now().toString());
        return result;
    }

    //Optional<Float> de las consultas nativas de moda -> Double para los DTO (null si no hay datos)
    private Double aDouble(Optional<Float> valor) {
        return valor.map(Float::doubleValue).orElse(null);
    }
}