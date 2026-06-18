package mx.edu.cenidet.estadias.controllers.telemetria;

import lombok.RequiredArgsConstructor;
import mx.edu.cenidet.estadias.dtos.comunes.ApiResponseDTO;
import mx.edu.cenidet.estadias.dtos.comunes.PageResponseDTO;
import mx.edu.cenidet.estadias.dtos.telemetria.ClimateReadingDTO;
import mx.edu.cenidet.estadias.dtos.telemetria.ElectricReadingDTO;
import mx.edu.cenidet.estadias.dtos.telemetria.LatestSummaryDTO;
import mx.edu.cenidet.estadias.dtos.telemetria.TelemetryFilterDTO;
import mx.edu.cenidet.estadias.excepciones.BusinessRuleException;
import mx.edu.cenidet.estadias.services.sync.SyncHealthService;
import mx.edu.cenidet.estadias.services.telemetry.ClimateReadingService;
import mx.edu.cenidet.estadias.services.telemetry.ElectricReadingService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/telemetria")
@RequiredArgsConstructor
public class TelemetryController {

    private final ClimateReadingService climateReadingService;
    private final ElectricReadingService electricReadingService;
    private final SyncHealthService syncHealthService;

    //Dashboard principal (mapa + tarjetas de resumen).
    @GetMapping("/resumen")
    public ResponseEntity<ApiResponseDTO<LatestSummaryDTO>> obtenerResumen() {

        LatestSummaryDTO resumen = LatestSummaryDTO.builder()
                .ultimaLecturaClimatica(climateReadingService.obtenerUltima())
                .ultimaLecturaElectrica(electricReadingService.obtenerUltima())
                .datosEnTiempoReal(
                        !syncHealthService.estaEnError(SyncHealthService.FUENTE_AMBIENT)
                                && !syncHealthService.estaEnError(SyncHealthService.FUENTE_THINGSPEAK))
                .timestampConsulta(LocalDateTime.now())
                .build();

        return ResponseEntity.ok(ApiResponseDTO.ok("Resumen de telemetría obtenido.", resumen));
    }

    //Histórico climático paginado para gráficas.
    @GetMapping("/climatica")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponseDTO<PageResponseDTO<ClimateReadingDTO>>> obtenerClimatica(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin,
            @RequestParam(defaultValue = "0")   int page,
            @RequestParam(defaultValue = "500") int size) {

        validarRango(inicio, fin);
        TelemetryFilterDTO filtro = new TelemetryFilterDTO(inicio, fin, page, size);

        return ResponseEntity.ok(
                ApiResponseDTO.ok("Datos climáticos obtenidos.",
                        climateReadingService.obtenerPorRango(filtro)));
    }

    //Histórico eléctrico paginado para gráficas.
    @GetMapping("/electrica")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponseDTO<PageResponseDTO<ElectricReadingDTO>>> obtenerElectrica(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin,
            @RequestParam(defaultValue = "0")   int page,
            @RequestParam(defaultValue = "500") int size) {

        validarRango(inicio, fin);
        TelemetryFilterDTO filtro = new TelemetryFilterDTO(inicio, fin, page, size);

        return ResponseEntity.ok(
                ApiResponseDTO.ok("Datos eléctricos obtenidos.",
                        electricReadingService.obtenerPorRango(filtro)));
    }

    private void validarRango(LocalDateTime inicio, LocalDateTime fin) {
        if (!inicio.isBefore(fin)) {
            throw new BusinessRuleException(
                    "La fecha de inicio debe ser anterior a la fecha de fin.");
        }
    }
}
