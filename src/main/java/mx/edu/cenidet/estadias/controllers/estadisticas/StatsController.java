package mx.edu.cenidet.estadias.controllers.estadisticas;

import lombok.RequiredArgsConstructor;
import mx.edu.cenidet.estadias.dtos.comunes.ApiResponseDTO;
import mx.edu.cenidet.estadias.dtos.estadisticas.ClimateStatisticsDTO;
import mx.edu.cenidet.estadias.dtos.estadisticas.ElectricStatisticsDTO;
import mx.edu.cenidet.estadias.dtos.estadisticas.StatsFilterDTO;
import mx.edu.cenidet.estadias.excepciones.BusinessRuleException;
import mx.edu.cenidet.estadias.services.stats.StatisticsService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

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

    //Promedio/máximo/mínimo de corriente, voltaje, potencia y energía.
    @GetMapping("/electrica")
    public ResponseEntity<ApiResponseDTO<ElectricStatisticsDTO>> estadisticasElectricas(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin) {

        validarRango(inicio, fin);
        StatsFilterDTO filtro = new StatsFilterDTO(inicio, fin);

        return ResponseEntity.ok(
                ApiResponseDTO.ok("Estadísticas eléctricas calculadas.",
                        statisticsService.calcularElectricas(filtro)));
    }

    private void validarRango(LocalDateTime inicio, LocalDateTime fin) {
        if (!inicio.isBefore(fin)) {
            throw new BusinessRuleException(
                    "La fecha de inicio debe ser anterior a la fecha de fin.");
        }
    }
}

