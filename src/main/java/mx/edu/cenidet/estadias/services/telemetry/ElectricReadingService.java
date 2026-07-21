package mx.edu.cenidet.estadias.services.telemetry;

import lombok.RequiredArgsConstructor;

import mx.edu.cenidet.estadias.dtos.comunes.PageResponseDTO;
import mx.edu.cenidet.estadias.dtos.telemetria.ElectricReadingDTO;
import mx.edu.cenidet.estadias.dtos.telemetria.TelemetryFilterDTO;
import mx.edu.cenidet.estadias.excepciones.ResourceNotFoundException;
import mx.edu.cenidet.estadias.modelos.lecturaElectrica.BeanLecturaElectrica;
import mx.edu.cenidet.estadias.modelos.lecturaElectrica.FuenteElectrica;
import mx.edu.cenidet.estadias.repositorios.lecturaElectrica.LecturaElectricaRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

//Acceso de lectura a datos eléctricos almacenados. Todo se consulta por
//fuente (FOTOVOLTAICO / EOLICO) porque son dos canales con variables distintas.
@Service
@RequiredArgsConstructor
public class ElectricReadingService {

    private static final int MAX_PAGE_SIZE = 1000;

    private final LecturaElectricaRepository electricaRepository;

    //Última lectura de una fuente para el Dashboard
    @Transactional(readOnly = true)
    public ElectricReadingDTO obtenerUltima(FuenteElectrica fuente) {
        return electricaRepository.findTopByFuenteOrderByFechaLecturaDesc(fuente)
                .map(this::mapToDTO)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No hay lecturas eléctricas registradas aún para " + fuente + "."));
    }

    //Histórico paginado para gráficas, de una fuente
    @Transactional(readOnly = true)
    public PageResponseDTO<ElectricReadingDTO> obtenerPorRango(TelemetryFilterDTO filtro, FuenteElectrica fuente) {
        var pageable = PageRequest.of(filtro.getPage(), Math.min(filtro.getSize(), MAX_PAGE_SIZE),
                Sort.by("fechaLectura").ascending());
        return PageResponseDTO.of(
                electricaRepository.findByFuenteAndFechaLecturaBetween(
                                fuente, filtro.getInicio(), filtro.getFin(), pageable)
                        .map(this::mapToDTO)
        );
    }

    //Dataset completo para generación de reporte, de una fuente
    @Transactional(readOnly = true)
    public List<BeanLecturaElectrica> obtenerEntidadesPorRango(
            LocalDateTime inicio, LocalDateTime fin, FuenteElectrica fuente) {
        return electricaRepository.findByFuenteAndFechaLecturaBetweenOrderByFechaLecturaAsc(fuente, inicio, fin);
    }

    public ElectricReadingDTO mapToDTO(BeanLecturaElectrica l) {
        return ElectricReadingDTO.builder()
                .idLecturaElectrica(l.getIdLecturaElectrica())
                .fechaLectura(l.getFechaLectura())
                .fuente(l.getFuente())
                .voltaje(l.getVoltaje())
                .corriente(l.getCorriente())
                .potencia(l.getPotencia())
                .voc(l.getVoc())
                .energia(l.getEnergia())
                .build();
    }
}

