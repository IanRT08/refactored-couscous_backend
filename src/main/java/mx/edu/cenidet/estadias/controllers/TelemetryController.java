package mx.edu.cenidet.estadias.controllers;

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

// Módulos 4 y 5 — Acceso a datos de telemetría.
// /resumen es semi-público: cualquiera lo llama (visitantes + usuarios)
//   para el Dashboard y el mapa (Módulo 4).
// /climatica y /electrica requieren autenticación (Módulo 5):
//   solo usuarios registrados pueden ver el histórico detallado.
@RestController
@RequestMapping("/api/telemetria")
@RequiredArgsConstructor
public class TelemetryController {

    private final ClimateReadingService climateReadingService;
    private final ElectricReadingService electricReadingService;
    private final SyncHealthService syncHealthService;

    // ── GET /api/telemetria/resumen ───────────────────────────
    // Módulo 4 — Dashboard principal (mapa + tarjetas de resumen).
    // Combina la última lectura climática y eléctrica en una sola respuesta.
    // El flag datosEnTiempoReal permite al OfflineBanner del frontend
    // mostrar "Datos en caché — sin conexión" cuando es false.
    // Acceso: público (visitantes y usuarios registrados).
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

    // ── GET /api/telemetria/climatica ─────────────────────────
    // Módulo 5 — Histórico climático paginado para gráficas.
    // DFR RN: "Las gráficas serán de los datos por separado"
    //         (histograma o barras según la variable).
    // El frontend (useChartData hook) puede llamar esto con distintos
    // rangos para alimentar react-chartjs-2 con hasta 10k+ puntos.
    // Parámetros: inicio (ISO), fin (ISO), page (default 0), size (default 500)
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

    // ── GET /api/telemetria/electrica ─────────────────────────
    // Módulo 5 — Histórico eléctrico paginado para gráficas.
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

    // ── Helper de validación ──────────────────────────────────
    private void validarRango(LocalDateTime inicio, LocalDateTime fin) {
        if (!inicio.isBefore(fin)) {
            throw new BusinessRuleException(
                    "La fecha de inicio debe ser anterior a la fecha de fin.");
        }
    }
}
