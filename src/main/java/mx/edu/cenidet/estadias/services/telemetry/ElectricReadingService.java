package mx.edu.cenidet.estadias.services.telemetry;

import lombok.RequiredArgsConstructor;

import mx.edu.cenidet.estadias.dtos.comunes.PageResponseDTO;
import mx.edu.cenidet.estadias.dtos.telemetria.ElectricReadingDTO;
import mx.edu.cenidet.estadias.dtos.telemetria.TelemetryFilterDTO;
import mx.edu.cenidet.estadias.excepciones.ResourceNotFoundException;
import mx.edu.cenidet.estadias.modelos.lecturaElectrica.BeanLecturaElectrica;
import mx.edu.cenidet.estadias.repositorios.lecturaElectrica.LecturaElectricaRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

//Acceso de lectura a datos eléctricos almacenados.
@Service
@RequiredArgsConstructor
public class ElectricReadingService {

    private final LecturaElectricaRepository electricaRepository;

    //Última lectura para el Dashboard
    @Transactional(readOnly = true)
    public ElectricReadingDTO obtenerUltima() {
        return electricaRepository.findTopByOrderByFechaLecturaDesc()
                .map(this::mapToDTO)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No hay lecturas eléctricas registradas aún."));
    }

    //Histórico paginado para gráficas
    @Transactional(readOnly = true)
    public PageResponseDTO<ElectricReadingDTO> obtenerPorRango(TelemetryFilterDTO filtro) {
        var pageable = PageRequest.of(filtro.getPage(), filtro.getSize(),
                Sort.by("fechaLectura").ascending());
        return PageResponseDTO.of(
                electricaRepository.findByFechaLecturaBetween(
                                filtro.getInicio(), filtro.getFin(), pageable)
                        .map(this::mapToDTO)
        );
    }

    //Dataset completo para generación de reporte
    @Transactional(readOnly = true)
    public List<BeanLecturaElectrica> obtenerEntidadesPorRango(LocalDateTime inicio, LocalDateTime fin) {
        return electricaRepository.findByFechaLecturaBetween(inicio, fin);
    }

    public ElectricReadingDTO mapToDTO(BeanLecturaElectrica l) {
        return ElectricReadingDTO.builder()
                .idLecturaElectrica(l.getIdLecturaElectrica())
                .fechaLectura(l.getFechaLectura())
                .corriente(l.getCorriente())
                .voltaje(l.getVoltaje())
                .potencia(l.getPotencia())
                .energia(l.getEnergia())
                .build();
    }
}

