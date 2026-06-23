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
import java.util.List;

@RestController
@RequestMapping("/api/reportes")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class ReportController {

    private final ClimateReadingService climateReadingService;
    private final ElectricReadingService electricReadingService;
    private final ExcelReportService excelReportService;
    private final PdfReportService pdfReportService;
    private final DownloadRequestService downloadRequestService;
    private final AuthUtils authUtils;

    //Genera y descarga el archivo en el formato solicitado.
    @GetMapping("/descargar")
    public ResponseEntity<byte[]> descargar(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin,
            @RequestParam TipoReporte tipo,
            @RequestParam FormatoReporte formato,
            @RequestParam(required = false) FuenteElectrica fuente,
            HttpServletRequest request) {

        //Rango de fechas válido
        if (!inicio.isBefore(fin)) {
            throw new BusinessRuleException(
                    "La fecha de inicio debe ser anterior a la fecha de fin.");
        }

        //Para reportes eléctricos hay que indicar de qué canal (no se reporta combinado)
        if (TipoReporte.ELECTRICO.equals(tipo) && fuente == null) {
            throw new BusinessRuleException(
                    "Debes indicar la fuente (FOTOVOLTAICO o EOLICO) para el reporte eléctrico.");
        }

        // ── Guardia 2: permiso de descarga ACTIVO ────────────
        Long idUsuario = authUtils.getIdUsuarioActual(request);
        if (!downloadRequestService.tienePermisoActivo(idUsuario)) {
            throw new BusinessRuleException(
                    "ACCESO RESTRINGIDO. Lo sentimos, pero actualmente no cuentas con el permiso "
                            + "para descargar reportes. Necesitas solicitarlo.");
        }

        //DTO de filtro
        ReportFilterDTO filtro = new ReportFilterDTO(inicio, fin, tipo, formato, fuente);

        //Obtener datos y generar reporte
        byte[] reporte = generarReporte(filtro);

        //Construir cabeceras de descarga
        String contentType = FormatoReporte.XLSX.equals(formato)
                ? "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                : "application/pdf";

        String extension = FormatoReporte.XLSX.equals(formato) ? "xlsx" : "pdf";
        String sufijoFuente = fuente != null ? "-" + fuente.name().toLowerCase() : "";
        String filename  = "reporte-" + tipo.name().toLowerCase() + sufijoFuente
                + "-" + LocalDate.now() + "." + extension;

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\"")
                .body(reporte);
    }

    //Helper: delega en el servicio correcto
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

