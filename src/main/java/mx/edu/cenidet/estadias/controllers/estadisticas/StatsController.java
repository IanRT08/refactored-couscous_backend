package mx.edu.cenidet.estadias.controllers.estadisticas;

import lombok.RequiredArgsConstructor;
import mx.edu.cenidet.estadias.dtos.comunes.ApiResponseDTO;
import mx.edu.cenidet.estadias.dtos.estadisticas.ClimateResumenDTO;
import mx.edu.cenidet.estadias.dtos.estadisticas.ClimateStatisticsDTO;
import mx.edu.cenidet.estadias.dtos.estadisticas.ElectricCombinedStatisticsDTO;
import mx.edu.cenidet.estadias.dtos.estadisticas.ElectricStatisticsDTO;
import mx.edu.cenidet.estadias.dtos.estadisticas.StatsFilterDTO;
import mx.edu.cenidet.estadias.excepciones.BusinessRuleException;
import mx.edu.cenidet.estadias.modelos.lecturaElectrica.FuenteElectrica;
import mx.edu.cenidet.estadias.services.stats.StatisticsService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/estadisticas")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class StatsController {

    private final StatisticsService statisticsService;

    //Promedio/máximo/mínimo de todas las variables climáticas
    @GetMapping("/climatica")
    public ResponseEntity<ApiResponseDTO<ClimateStatisticsDTO>> estadisticasClimaticas(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin) {

        validarRango(inicio, fin);
        StatsFilterDTO filtro = new StatsFilterDTO(inicio, fin);

        return ResponseEntity.ok(
                ApiResponseDTO.ok("Estadísticas climáticas calculadas.",
                        statisticsService.calcularClimaticas(filtro)));
    }

    //Promedio/máximo/mínimo/moda de voltaje, corriente, potencia, Voc y energía
    //de un canal específico (Fotovoltaico o Eólico).
    @GetMapping("/electrica")
    public ResponseEntity<ApiResponseDTO<ElectricStatisticsDTO>> estadisticasElectricas(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin,
            @RequestParam FuenteElectrica fuente) {

        validarRango(inicio, fin);
        StatsFilterDTO filtro = new StatsFilterDTO(inicio, fin);

        return ResponseEntity.ok(
                ApiResponseDTO.ok("Estadísticas eléctricas calculadas.",
                        statisticsService.calcularElectricas(filtro, fuente)));
    }

    //Vista combinada del sistema híbrido (potencia promedio y energía total
    //sumadas entre los dos canales), con desglose por fuente.
    @GetMapping("/electrica/combinada")
    public ResponseEntity<ApiResponseDTO<ElectricCombinedStatisticsDTO>> estadisticasElectricasCombinadas(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin) {

        validarRango(inicio, fin);
        StatsFilterDTO filtro = new StatsFilterDTO(inicio, fin);

        return ResponseEntity.ok(
                ApiResponseDTO.ok("Estadísticas eléctricas combinadas calculadas.",
                        statisticsService.calcularElectricasCombinadas(filtro)));
    }

    //Resumen de 5 períodos para la tabla del dashboard (sin lanzar error si un período no tiene datos)
    @GetMapping("/climatica/resumen")
    public ResponseEntity<ApiResponseDTO<ClimateResumenDTO>> resumenClimatico() {
        return ResponseEntity.ok(
                ApiResponseDTO.ok("Resumen climático calculado.",
                        statisticsService.calcularResumenClimatico()));
    }

    //Fechas mínimas disponibles por tipo de dato — para inicializar los date pickers del frontend
    @GetMapping("/fechas")
    public ResponseEntity<ApiResponseDTO<Map<String, String>>> fechasDisponibles() {
        return ResponseEntity.ok(
                ApiResponseDTO.ok("Fechas disponibles.", statisticsService.obtenerFechasDisponibles()));
    }

    private void validarRango(LocalDateTime inicio, LocalDateTime fin) {
        if (!inicio.isBefore(fin)) {
            throw new BusinessRuleException(
                    "La fecha de inicio debe ser anterior a la fecha de fin.");
        }
    }
}

