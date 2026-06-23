package mx.edu.cenidet.estadias.services.telemetry;

import lombok.RequiredArgsConstructor;

import mx.edu.cenidet.estadias.dtos.comunes.PageResponseDTO;
import mx.edu.cenidet.estadias.dtos.telemetria.ClimateReadingDTO;
import mx.edu.cenidet.estadias.dtos.telemetria.TelemetryFilterDTO;
import mx.edu.cenidet.estadias.excepciones.ResourceNotFoundException;
import mx.edu.cenidet.estadias.modelos.lectura.BeanLectura;
import mx.edu.cenidet.estadias.repositorios.lectura.LecturaRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClimateReadingService {

    private final LecturaRepository lecturaRepository;

    //Última lectura para el Dashboard
    @Transactional(readOnly = true)
    public ClimateReadingDTO obtenerUltima() {
        return lecturaRepository.findTopByOrderByFechaLecturaDesc()
                .map(this::mapToDTO)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No hay lecturas climáticas registradas aún."));
    }

    //Histórico paginado para gráficas
    @Transactional(readOnly = true)
    public PageResponseDTO<ClimateReadingDTO> obtenerPorRango(TelemetryFilterDTO filtro) {
        var pageable = PageRequest.of(filtro.getPage(), filtro.getSize(),
                Sort.by("fechaLectura").ascending());
        return PageResponseDTO.of(
                lecturaRepository.findByFechaLecturaBetween(
                                filtro.getInicio(), filtro.getFin(), pageable)
                        .map(this::mapToDTO)
        );
    }

    //Dataset completo para generación de reporte
    @Transactional(readOnly = true)
    public List<BeanLectura> obtenerEntidadesPorRango(LocalDateTime inicio, LocalDateTime fin) {
        return lecturaRepository.findByFechaLecturaBetween(inicio, fin);
    }

    public ClimateReadingDTO mapToDTO(BeanLectura l) {
        return ClimateReadingDTO.builder()
                .idLectura(l.getIdLectura())
                .fechaLectura(l.getFechaLectura())
                .temperatura(l.getTemperatura())
                .viento(l.getViento())
                .humedad(l.getHumedad())
                .radiacion(l.getRadiacion())
                .presion(l.getPresion())
                .rainRate(l.getRainRate())
                .dailyRain(l.getDailyRain())
                .rainTotal(l.getRainTotal())
                .build();
    }
}