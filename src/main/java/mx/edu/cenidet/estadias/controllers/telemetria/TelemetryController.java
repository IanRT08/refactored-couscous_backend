package mx.edu.cenidet.estadias.controllers.telemetria;

import lombok.RequiredArgsConstructor;
import mx.edu.cenidet.estadias.dtos.comunes.ApiResponseDTO;
import mx.edu.cenidet.estadias.dtos.comunes.PageResponseDTO;
import mx.edu.cenidet.estadias.dtos.telemetria.ClimateReadingDTO;
import mx.edu.cenidet.estadias.dtos.telemetria.ElectricReadingDTO;
import mx.edu.cenidet.estadias.dtos.telemetria.LatestSummaryDTO;
import mx.edu.cenidet.estadias.dtos.telemetria.TelemetryFilterDTO;
import mx.edu.cenidet.estadias.excepciones.BusinessRuleException;
import mx.edu.cenidet.estadias.excepciones.ResourceNotFoundException;
import mx.edu.cenidet.estadias.modelos.lecturaElectrica.FuenteElectrica;
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
    //Si una de las dos fuentes aún no tiene datos (p.ej. recién desplegado el
    //sistema, o solo una API está fallando), se muestra lo que sí está
    //disponible en vez de fallar el resumen completo.
    @GetMapping("/resumen")
    public ResponseEntity<ApiResponseDTO<LatestSummaryDTO>> obtenerResumen() {

        ClimateReadingDTO climatica = null;
        try {
            climatica = climateReadingService.obtenerUltima();
        } catch (ResourceNotFoundException ignored) {
            // Sin lecturas climáticas todavía; se deja null y se informa con datosEnTiempoReal.
        }

        ElectricReadingDTO fotovoltaica = null;
        try {
            fotovoltaica = electricReadingService.obtenerUltima(FuenteElectrica.FOTOVOLTAICO);
        } catch (ResourceNotFoundException ignored) {
            //Sin lecturas de este canal todavía; se deja null y se informa con datosEnTiempoReal.
        }

        ElectricReadingDTO eolica = null;
        try {
            eolica = electricReadingService.obtenerUltima(FuenteElectrica.EOLICO);
        } catch (ResourceNotFoundException ignored) {
            //Sin lecturas de este canal todavía; se deja null y se informa con datosEnTiempoReal.
        }

        LatestSummaryDTO resumen = LatestSummaryDTO.builder()
                .ultimaLecturaClimatica(climatica)
                .ultimaLecturaFotovoltaica(fotovoltaica)
                .ultimaLecturaEolica(eolica)
                .datosEnTiempoReal(
                        !syncHealthService.estaEnError(SyncHealthService.FUENTE_AMBIENT)
                                && !syncHealthService.estaEnError(SyncHealthService.FUENTE_THINGSPEAK))
                .timestampConsulta(LocalDateTime.now())
                .build();

        return ResponseEntity.ok(ApiResponseDTO.ok("Resumen de telemetría obtenido.", resumen));
    }

    //Tabla pública: datos climáticos anteriores al mes en curso, sin autenticación.
    @GetMapping("/publica/tabla")
    public ResponseEntity<ApiResponseDTO<PageResponseDTO<ClimateReadingDTO>>> obtenerTablaPublica(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "12") int size) {

        return ResponseEntity.ok(
                ApiResponseDTO.ok("Datos públicos obtenidos.",
                        climateReadingService.obtenerPublicaTabla(page, size)));
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

    //Histórico eléctrico paginado para gráficas, de un canal (Fotovoltaico o Eólico).
    @GetMapping("/electrica")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponseDTO<PageResponseDTO<ElectricReadingDTO>>> obtenerElectrica(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin,
            @RequestParam FuenteElectrica fuente,
            @RequestParam(defaultValue = "0")   int page,
            @RequestParam(defaultValue = "500") int size) {

        validarRango(inicio, fin);
        TelemetryFilterDTO filtro = new TelemetryFilterDTO(inicio, fin, page, size);

        return ResponseEntity.ok(
                ApiResponseDTO.ok("Datos eléctricos obtenidos.",
                        electricReadingService.obtenerPorRango(filtro, fuente)));
    }

    private void validarRango(LocalDateTime inicio, LocalDateTime fin) {
        if (!inicio.isBefore(fin)) {
            throw new BusinessRuleException(
                    "La fecha de inicio debe ser anterior a la fecha de fin.");
        }
    }
}
