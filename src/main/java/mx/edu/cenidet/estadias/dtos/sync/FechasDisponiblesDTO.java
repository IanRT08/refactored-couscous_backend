package mx.edu.cenidet.estadias.dtos.sync;

import java.time.LocalDateTime;

public record FechasDisponiblesDTO(
        LocalDateTime fechaInicioAW,
        LocalDateTime fechaInicioFV,
        LocalDateTime fechaInicioEolico
) {}
