package mx.edu.cenidet.estadias.services.report;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.edu.cenidet.estadias.dtos.reportes.ReportFilterDTO;
import mx.edu.cenidet.estadias.excepciones.ReportGenerationException;
import mx.edu.cenidet.estadias.modelos.lectura.BeanLectura;
import mx.edu.cenidet.estadias.modelos.lecturaElectrica.BeanLecturaElectrica;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExcelReportService {

    private final ChartGeneratorService chartGeneratorService;

    // ── CLIMÁTICO ─────────────────────────────────────────────────────
    public byte[] generarReporteClimatico(List<BeanLectura> datos, ReportFilterDTO filtro) {
        try (Workbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet hoja = wb.createSheet("Datos Climáticos");
            CellStyle estiloEnc  = crearEstiloEncabezado(wb);
            CellStyle estiloRes  = crearEstiloResumen(wb);
            CellStyle estiloModa = crearEstiloModa(wb);

            record ColClima(String clave, String cabecera) {}
            List<ColClima> todasCols = List.of(
                new ColClima("TEMPERATURA", "Temperatura (°C)"),
                new ColClima("VIENTO",      "Viento (m/s)"),
                new ColClima("HUMEDAD",     "Humedad (%)"),
                new ColClima("RADIACION",   "Radiación (W/m²)"),
                new ColClima("PRESION",     "Presión (hPa)")
            );
            List<ColClima> cols = todasCols.stream()
                .filter(c -> filtro.incluirVariable(c.clave()))
                .toList();

            String[] cabeceras = new String[1 + cols.size()];
            cabeceras[0] = "Fecha/Hora";
            for (int i = 0; i < cols.size(); i++) cabeceras[i + 1] = cols.get(i).cabecera();
            crearFila(hoja, 0, cabeceras, estiloEnc);

            int fila = 1;
            for (BeanLectura l : datos) {
                Row row = hoja.createRow(fila++);
                row.createCell(0).setCellValue(l.getFechaLectura().toString());
                int col = 1;
                for (ColClima c : cols) {
                    Float v = switch (c.clave()) {
                        case "TEMPERATURA" -> l.getTemperatura();
                        case "VIENTO"      -> l.getViento();
                        case "HUMEDAD"     -> l.getHumedad();
                        case "RADIACION"   -> l.getRadiacion();
                        case "PRESION"     -> l.getPresion();
                        default            -> null;
                    };
                    setCeldaFloat(row, col++, v);
                }
            }

            int ultimaFila = fila;
            List<String[]> resumen = buildResumen(filtro, 2, ultimaFila, cols.size());
            for (String[] entrada : resumen) {
                CellStyle estilo = "MODA".equals(entrada[0]) ? estiloModa : estiloRes;
                Row row = hoja.createRow(fila++);
                Cell etq = row.createCell(0);
                etq.setCellValue(entrada[0]);
                etq.setCellStyle(estilo);
                for (int col = 1; col < entrada.length; col++) {
                    Cell cell = row.createCell(col);
                    if (entrada[col] != null) cell.setCellFormula(entrada[col]);
                    cell.setCellStyle(estilo);
                }
            }

            for (int i = 0; i <= cols.size(); i++) hoja.autoSizeColumn(i);

            byte[] graficaPng = chartGeneratorService.generarGraficaClimatica(datos, filtro.getVariables());
            if (graficaPng.length > 0) embedImagen(wb, hoja, graficaPng, fila + 2, cols.size());

            wb.write(out);
            log.info("Reporte Excel climático: {} filas, {} columnas", datos.size(), cols.size());
            return out.toByteArray();

        } catch (IOException e) {
            throw new ReportGenerationException("Error generando reporte Excel climático", e);
        }
    }

    // ── ELÉCTRICO ─────────────────────────────────────────────────────
    public byte[] generarReporteElectrico(List<BeanLecturaElectrica> datos, ReportFilterDTO filtro) {
        try (Workbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet hoja = wb.createSheet("Datos Eléctricos - " + filtro.getFuente());
            CellStyle estiloEnc  = crearEstiloEncabezado(wb);
            CellStyle estiloRes  = crearEstiloResumen(wb);
            CellStyle estiloModa = crearEstiloModa(wb);

            record ColElec(String clave, String cabecera) {}
            List<ColElec> todasCols = List.of(
                new ColElec("VOLTAJE",   "Voltaje (V)"),
                new ColElec("CORRIENTE", "Corriente (A)"),
                new ColElec("POTENCIA",  "Potencia (W)"),
                new ColElec("VOC",       "Voc (V)"),
                new ColElec("ENERGIA",   "Energía (Wh)")
            );
            List<ColElec> cols = todasCols.stream()
                .filter(c -> filtro.incluirVariable(c.clave()))
                .toList();

            String[] cabeceras = new String[1 + cols.size()];
            cabeceras[0] = "Fecha/Hora";
            for (int i = 0; i < cols.size(); i++) cabeceras[i + 1] = cols.get(i).cabecera();
            crearFila(hoja, 0, cabeceras, estiloEnc);

            int fila = 1;
            for (BeanLecturaElectrica l : datos) {
                Row row = hoja.createRow(fila++);
                row.createCell(0).setCellValue(l.getFechaLectura().toString());
                int col = 1;
                for (ColElec c : cols) {
                    Float v = switch (c.clave()) {
                        case "VOLTAJE"   -> l.getVoltaje();
                        case "CORRIENTE" -> l.getCorriente();
                        case "POTENCIA"  -> l.getPotencia();
                        case "VOC"       -> l.getVoc();
                        case "ENERGIA"   -> l.getEnergia();
                        default          -> null;
                    };
                    setCeldaFloat(row, col++, v);
                }
            }

            int ultimaFila = fila;
            List<String[]> resumen = buildResumen(filtro, 2, ultimaFila, cols.size());
            for (String[] entrada : resumen) {
                CellStyle estilo = "MODA".equals(entrada[0]) ? estiloModa : estiloRes;
                Row row = hoja.createRow(fila++);
                Cell etq = row.createCell(0);
                etq.setCellValue(entrada[0]);
                etq.setCellStyle(estilo);
                for (int col = 1; col < entrada.length; col++) {
                    Cell cell = row.createCell(col);
                    if (entrada[col] != null) cell.setCellFormula(entrada[col]);
                    cell.setCellStyle(estilo);
                }
            }

            for (int i = 0; i <= cols.size(); i++) hoja.autoSizeColumn(i);

            byte[] graficaPng = chartGeneratorService.generarGraficaElectrica(datos, filtro.getVariables());
            if (graficaPng.length > 0) embedImagen(wb, hoja, graficaPng, fila + 2, cols.size());

            wb.write(out);
            log.info("Reporte Excel eléctrico: {} filas, {} columnas", datos.size(), cols.size());
            return out.toByteArray();

        } catch (IOException e) {
            throw new ReportGenerationException("Error generando reporte Excel eléctrico", e);
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────

    private List<String[]> buildResumen(ReportFilterDTO filtro, int colInicio,
                                        int ultimaFila, int numCols) {
        List<String[]> result = new ArrayList<>();
        record StatDef(String clave, String etiqueta, String funcion) {}
        List<StatDef> defs = List.of(
            new StatDef("PROMEDIO", "PROMEDIO", "AVERAGE"),
            new StatDef("MAXIMO",   "MÁXIMO",   "MAX"),
            new StatDef("MINIMO",   "MÍNIMO",   "MIN"),
            new StatDef("MODA",     "MODA",     "MODE")
        );
        for (StatDef sd : defs) {
            if (!filtro.incluirEstadistica(sd.clave())) continue;
            String[] entrada = new String[1 + numCols];
            entrada[0] = sd.etiqueta();
            for (int i = 0; i < numCols; i++) {
                String colLetra = CellReference.convertNumToColString(colInicio - 1 + i);
                entrada[i + 1] = sd.funcion() + "(" + colLetra + "2:" + colLetra + ultimaFila + ")";
            }
            result.add(entrada);
        }
        return result;
    }

    private void embedImagen(Workbook wb, Sheet hoja, byte[] png, int filaInicio, int numCols) {
        int picIdx = wb.addPicture(png, Workbook.PICTURE_TYPE_PNG);
        Drawing<?> drawing = hoja.createDrawingPatriarch();
        ClientAnchor anchor = wb.getCreationHelper().createClientAnchor();
        anchor.setCol1(0); anchor.setRow1(filaInicio);
        anchor.setCol2(Math.min(numCols + 1, 8)); anchor.setRow2(filaInicio + 25);
        drawing.createPicture(anchor, picIdx);
    }

    private void crearFila(Sheet hoja, int numFila, String[] valores, CellStyle estilo) {
        Row row = hoja.createRow(numFila);
        for (int i = 0; i < valores.length; i++) {
            Cell cell = row.createCell(i);
            cell.setCellValue(valores[i] != null ? valores[i] : "");
            if (estilo != null) cell.setCellStyle(estilo);
        }
    }

    private void setCeldaFloat(Row row, int col, Float valor) {
        Cell cell = row.createCell(col);
        if (valor != null) cell.setCellValue(valor);
    }

    private CellStyle crearEstiloEncabezado(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        Font f = wb.createFont(); f.setBold(true); s.setFont(f);
        s.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setBorderBottom(BorderStyle.THIN);
        return s;
    }

    private CellStyle crearEstiloResumen(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        Font f = wb.createFont(); f.setBold(true); s.setFont(f);
        s.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return s;
    }

    private CellStyle crearEstiloModa(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        Font f = wb.createFont(); f.setBold(true); s.setFont(f);
        s.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return s;
    }
}
