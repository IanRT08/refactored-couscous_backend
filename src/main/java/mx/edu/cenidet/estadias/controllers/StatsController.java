package mx.edu.cenidet.estadias.controllers;

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

// Módulo 6 — Estadísticas y cálculos eléctricos y climáticos.
// DFR CA: "Los resultados se muestran en una tabla individual por cada dato"
//         "Si no hay datos en cierto periodo el sistema manda un mensaje"
//          → StatisticsService lanza BusinessRuleException que GlobalExceptionHandler
//            serializa como JSON (SweetAlert2 lo muestra en el frontend).
@RestController
@RequestMapping("/api/estadisticas")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class StatsController {

    private final StatisticsService statisticsService;

    // ── GET /api/estadisticas/climatica ──────────────────────
    // Módulo 6 — Promedio/máximo/mínimo de todas las variables climáticas
    //            en el rango de fechas indicado.
    // StatisticsService delega en LecturaRepository.calcularEstadisticas()
    // para que MySQL haga la agregación (eficiente con 10k+ registros).
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

    // ── GET /api/estadisticas/electrica ──────────────────────
    // Módulo 6 — Promedio/máximo/mínimo de corriente, voltaje, potencia y energía.
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

