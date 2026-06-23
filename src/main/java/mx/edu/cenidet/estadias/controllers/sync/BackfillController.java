package mx.edu.cenidet.estadias.controllers.sync;

import lombok.RequiredArgsConstructor;
import mx.edu.cenidet.estadias.dtos.comunes.ApiResponseDTO;
import mx.edu.cenidet.estadias.excepciones.BusinessRuleException;
import mx.edu.cenidet.estadias.modelos.lecturaElectrica.FuenteElectrica;
import mx.edu.cenidet.estadias.services.sync.HistoricalBackfillService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

//Respaldo histórico manual (una sola vez): importa a la base de datos local
//meses de datos que ya existen en Ambient Weather / ThingSpeak, pero que el
//GapRecoveryService normal nunca trae (ese solo cubre desconexiones cortas).
//Solo SUPERADMINISTRADOR puede dispararlo: consume muchas llamadas de las
//APIs externas y puede tardar bastante, por eso corre en segundo plano.
@RestController
@RequestMapping("/api/admin/respaldo-historico")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPERADMINISTRADOR')")
public class BackfillController {

    private final HistoricalBackfillService historicalBackfillService;

    @PostMapping("/climatico")
    public ResponseEntity<ApiResponseDTO<Void>> respaldarClimatico(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta) {

        if (historicalBackfillService.climaticoEstaEnProgreso()) {
            throw new BusinessRuleException("Ya hay un respaldo histórico climático en curso. Espera a que termine.");
        }
        LocalDateTime fin = validarRango(desde, hasta);
        historicalBackfillService.respaldarClimatico(desde, fin);

        return ResponseEntity.accepted().body(ApiResponseDTO.ok(
                "Respaldo histórico climático iniciado en segundo plano. Revisa los logs del servidor para ver el progreso."));
    }

    @PostMapping("/electrico")
    public ResponseEntity<ApiResponseDTO<Void>> respaldarElectrico(
            @RequestParam FuenteElectrica fuente,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta) {

        if (historicalBackfillService.electricoEstaEnProgreso(fuente)) {
            throw new BusinessRuleException("Ya hay un respaldo histórico en curso para " + fuente + ". Espera a que termine.");
        }
        LocalDateTime fin = validarRango(desde, hasta);
        historicalBackfillService.respaldarElectrico(fuente, desde, fin);

        return ResponseEntity.accepted().body(ApiResponseDTO.ok(
                "Respaldo histórico eléctrico (" + fuente + ") iniciado en segundo plano. Revisa los logs del servidor para ver el progreso."));
    }

    private LocalDateTime validarRango(LocalDateTime desde, LocalDateTime hasta) {
        LocalDateTime fin = hasta != null ? hasta : LocalDateTime.now();
        if (!desde.isBefore(fin)) {
            throw new BusinessRuleException("La fecha 'desde' debe ser anterior a 'hasta'.");
        }
        return fin;
    }
}
