package mx.edu.cenidet.estadias.controllers;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import mx.edu.cenidet.estadias.dtos.reportes.FormatoReporte;
import mx.edu.cenidet.estadias.dtos.reportes.ReportFilterDTO;
import mx.edu.cenidet.estadias.dtos.reportes.TipoReporte;
import mx.edu.cenidet.estadias.excepciones.BusinessRuleException;
import mx.edu.cenidet.estadias.modelos.lectura.BeanLectura;
import mx.edu.cenidet.estadias.modelos.lecturaElectrica.BeanLecturaElectrica;
import mx.edu.cenidet.estadias.services.downloadrequest.DownloadRequestService;
import mx.edu.cenidet.estadias.services.report.ExcelReportService;
import mx.edu.cenidet.estadias.services.report.PdfReportService;
import mx.edu.cenidet.estadias.services.telemetry.ClimateReadingService;
import mx.edu.cenidet.estadias.services.telemetry.ElectricReadingService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

// Módulo 7 — Generación y descarga de reportes (xlsx y pdf).
// Flujo de seguridad de dos capas:
//   1. Spring Security verifica JWT (isAuthenticated)
//   2. DownloadRequestService.tienePermisoActivo() verifica PermisoDescarga.ACTIVO
// DFR CA: "Si un usuario sin permiso entra a esta sección verá:
//          ACCESO RESTRINGIDO. No cuentas con permiso para descargar gráficas."
//          → BusinessRuleException → GlobalExceptionHandler → 400 JSON
//          → SweetAlert2 del frontend muestra el modal.
// DFR CA: "Antes de la descarga el usuario podrá elegir el formato"
//          → el frontend muestra DownloadFormatModal.jsx antes de llamar aquí.
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
    private final AuthUtils              authUtils;

    // ── GET /api/reportes/descargar ───────────────────────────
    // Módulo 7 — Genera y descarga el archivo en el formato solicitado.
    // El Content-Disposition: attachment fuerza la descarga en el browser.
    // Parámetros obligatorios:
    //   inicio  — fecha inicio (ISO 8601, ej: 2025-06-01T00:00:00)
    //   fin     — fecha fin    (ISO 8601, ej: 2025-06-15T23:59:59)
    //   tipo    — CLIMATICO | ELECTRICO
    //   formato — XLSX | PDF
    @GetMapping("/descargar")
    public ResponseEntity<byte[]> descargar(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin,
            @RequestParam TipoReporte tipo,
            @RequestParam FormatoReporte formato,
            HttpServletRequest request) {

        // ── Guardia 1: rango de fechas válido ────────────────
        if (!inicio.isBefore(fin)) {
            throw new BusinessRuleException(
                    "La fecha de inicio debe ser anterior a la fecha de fin.");
        }

        // ── Guardia 2: permiso de descarga ACTIVO ────────────
        Long idUsuario = authUtils.getIdUsuarioActual(request);
        if (!downloadRequestService.tienePermisoActivo(idUsuario)) {
            throw new BusinessRuleException(
                    "ACCESO RESTRINGIDO. Lo sentimos, pero actualmente no cuentas con el permiso "
                            + "para descargar reportes. Necesitas solicitarlo.");
        }

        // ── Construir DTO de filtro ───────────────────────────
        ReportFilterDTO filtro = new ReportFilterDTO(inicio, fin, tipo, formato);

        // ── Obtener datos y generar reporte ───────────────────
        byte[] reporte = generarReporte(filtro);

        // ── Construir cabeceras de descarga ───────────────────
        String contentType = FormatoReporte.XLSX.equals(formato)
                ? "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                : "application/pdf";

        String extension = FormatoReporte.XLSX.equals(formato) ? "xlsx" : "pdf";
        String filename  = "reporte-" + tipo.name().toLowerCase()
                + "-" + LocalDate.now() + "." + extension;

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\"")
                .body(reporte);
    }

    // ── Helper: delega en el servicio correcto ────────────────
    private byte[] generarReporte(ReportFilterDTO filtro) {
        if (TipoReporte.CLIMATICO.equals(filtro.getTipo())) {
            List<BeanLectura> datos = climateReadingService
                    .obtenerEntidadesPorRango(filtro.getInicio(), filtro.getFin());
            return FormatoReporte.XLSX.equals(filtro.getFormato())
                    ? excelReportService.generarReporteClimatico(datos, filtro)
                    : pdfReportService.generarReporteClimatico(datos, filtro);
        } else {
            List<BeanLecturaElectrica> datos = electricReadingService
                    .obtenerEntidadesPorRango(filtro.getInicio(), filtro.getFin());
            return FormatoReporte.XLSX.equals(filtro.getFormato())
                    ? excelReportService.generarReporteElectrico(datos, filtro)
                    : pdfReportService.generarReporteElectrico(datos, filtro);
        }
    }
}

