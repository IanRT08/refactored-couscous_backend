package mx.edu.cenidet.estadias.dtos.comunes;

import lombok.*;
import org.springframework.data.domain.Page;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PageResponseDTO<T> {

    private List<T> contenido;
    private int paginaActual;
    private int totalPaginas;
    private long totalElementos;
    private boolean esPrimera;
    private boolean esUltima;

    public static <T> PageResponseDTO<T> of(Page<T> page) {

        return new PageResponseDTO<>(
                page.getContent(),
                page.getNumber(),
                page.getTotalPages(),
                page.getTotalElements(),
                page.isFirst(),
                page.isLast()
        );

    }

}
