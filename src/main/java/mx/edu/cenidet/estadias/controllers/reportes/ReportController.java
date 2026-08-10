package mx.edu.cenidet.estadias.controllers.reportes;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import mx.edu.cenidet.estadias.dtos.reportes.FormatoReporte;
import mx.edu.cenidet.estadias.dtos.reportes.ReportFilterDTO;
import mx.edu.cenidet.estadias.dtos.reportes.TipoReporte;
import mx.edu.cenidet.estadias.excepciones.BusinessRuleException;
import mx.edu.cenidet.estadias.modelos.lectura.BeanLectura;
import mx.edu.cenidet.estadias.modelos.lecturaElectrica.BeanLecturaElectrica;
import mx.edu.cenidet.estadias.modelos.lecturaElectrica.FuenteElectrica;
import mx.edu.cenidet.estadias.services.downloadrequest.DownloadRequestService;
import mx.edu.cenidet.estadias.services.report.ExcelReportService;
import mx.edu.cenidet.estadias.services.report.PdfReportService;
import mx.edu.cenidet.estadias.services.telemetry.ClimateReadingService;
import mx.edu.cenidet.estadias.services.telemetry.ElectricReadingService;
import mx.edu.cenidet.estadias.util.AuthUtils;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/reportes")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class ReportController {

    private static final Set<String> VARS_CLIMATICAS     = Set.of("TEMPERATURA", "VIENTO", "HUMEDAD", "RADIACION", "PRESION");
    private static final Set<String> VARS_ELECTRICAS     = Set.of("VOLTAJE", "CORRIENTE", "POTENCIA", "VOC", "ENERGIA");
    private static final Set<String> ESTADISTICAS_VALIDAS = Set.of("PROMEDIO", "MAXIMO", "MINIMO", "MODA");

    private final ClimateReadingService climateReadingService;
    private final ElectricReadingService electricReadingService;
    private final ExcelReportService excelReportService;
    private final PdfReportService pdfReportService;
    private final DownloadRequestService downloadRequestService;
    private final AuthUtils authUtils;

    @GetMapping("/descargar")
    public ResponseEntity<byte[]> descargar(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin,
            @RequestParam TipoReporte tipo,
            @RequestParam FormatoReporte formato,
            @RequestParam(required = false) FuenteElectrica fuente,
            // Variables a incluir (repetidas: ?variables=TEMPERATURA&variables=VIENTO …)
            @RequestParam(required = false) List<String> variables,
            // Filas de estadísticas (?estadisticas=PROMEDIO&estadisticas=MAXIMO …)
            @RequestParam(required = false) List<String> estadisticas,
            HttpServletRequest request) {

        if (!inicio.isBefore(fin)) {
            throw new BusinessRuleException(
                    "La fecha de inicio debe ser anterior a la fecha de fin.");
        }

        if (ChronoUnit.MONTHS.between(inicio, fin) > 6) {
            throw new BusinessRuleException(
                    "El rango máximo permitido por reporte es de 6 meses.");
        }

        if (TipoReporte.ELECTRICO.equals(tipo) && fuente == null) {
            throw new BusinessRuleException(
                    "Debes indicar la fuente (FOTOVOLTAICO o EOLICO) para el reporte eléctrico.");
        }

        Long idUsuario = authUtils.getIdUsuarioActual(request);
        if (!downloadRequestService.tienePermisoActivo(idUsuario)) {
            throw new BusinessRuleException(
                    "ACCESO RESTRINGIDO. Lo sentimos, pero actualmente no cuentas con el permiso "
                            + "para descargar reportes. Necesitas solicitarlo.");
        }

        Set<String> varsValidas = TipoReporte.CLIMATICO.equals(tipo) ? VARS_CLIMATICAS : VARS_ELECTRICAS;
        if (variables != null && variables.stream().anyMatch(v -> !varsValidas.contains(v))) {
            throw new BusinessRuleException("Variable no válida. Valores permitidos: " + varsValidas);
        }
        if (estadisticas != null && estadisticas.stream().anyMatch(e -> !ESTADISTICAS_VALIDAS.contains(e))) {
            throw new BusinessRuleException("Estadística no válida. Valores permitidos: " + ESTADISTICAS_VALIDAS);
        }

        ReportFilterDTO filtro = new ReportFilterDTO(inicio, fin, tipo, formato, fuente);
        filtro.setVariables(variables);
        filtro.setEstadisticas(estadisticas);

        byte[] reporte = generarReporte(filtro);

        String contentType = FormatoReporte.XLSX.equals(formato)
                ? "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                : "application/pdf";
        String extension   = FormatoReporte.XLSX.equals(formato) ? "xlsx" : "pdf";
        String sufijoFuente = fuente != null ? "-" + fuente.name().toLowerCase() : "";
        String filename = "reporte-" + tipo.name().toLowerCase() + sufijoFuente
                + "-" + LocalDate.now() + "." + extension;

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\"")
                .body(reporte);
    }

    private byte[] generarReporte(ReportFilterDTO filtro) {
        if (TipoReporte.CLIMATICO.equals(filtro.getTipo())) {
            List<BeanLectura> datos = climateReadingService
                    .obtenerEntidadesPorRango(filtro.getInicio(), filtro.getFin());
            return FormatoReporte.XLSX.equals(filtro.getFormato())
                    ? excelReportService.generarReporteClimatico(datos, filtro)
                    : pdfReportService.generarReporteClimatico(datos, filtro);
        } else {
            List<BeanLecturaElectrica> datos = electricReadingService
                    .obtenerEntidadesPorRango(filtro.getInicio(), filtro.getFin(), filtro.getFuente());
            return FormatoReporte.XLSX.equals(filtro.getFormato())
                    ? excelReportService.generarReporteElectrico(datos, filtro)
                    : pdfReportService.generarReporteElectrico(datos, filtro);
        }
    }
}
