package mx.edu.cenidet.estadias.dtos.sync;

import java.time.LocalDateTime;

public record BackfillEstadoDTO(
        boolean climaticoEnProgreso,
        boolean fotovoltaicoEnProgreso,
        boolean eolicoEnProgreso,
        LocalDateTime fechaMinClimatica,
        LocalDateTime fechaMinFotovoltaico,
        LocalDateTime fechaMinEolico
) {}
