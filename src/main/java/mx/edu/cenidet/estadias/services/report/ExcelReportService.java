package mx.edu.cenidet.estadias.services.report;

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
import java.util.List;


@Service
@Slf4j
public class ExcelReportService {

    //Reporte Climático
    public byte[] generarReporteClimatico(List<BeanLectura> datos, ReportFilterDTO filtro) {
        try (Workbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet hoja = wb.createSheet("Datos Climáticos");
            CellStyle estiloEncabezado = crearEstiloEncabezado(wb);
            CellStyle estiloResumen    = crearEstiloResumen(wb);

            //Encabezados
            String[] cols = {"Fecha/Hora", "Temperatura (°C)", "Viento (m/s)",
                    "Humedad (%)", "Radiación (W/m²)", "Presión (hPa)"};
            crearFila(hoja, 0, cols, estiloEncabezado);

            //Datos
            int fila = 1;
            for (BeanLectura l : datos) {
                Row row = hoja.createRow(fila++);
                row.createCell(0).setCellValue(l.getFechaLectura().toString());
                setCeldaFloat(row, 1, l.getTemperatura());
                setCeldaFloat(row, 2, l.getViento());
                setCeldaFloat(row, 3, l.getHumedad());
                setCeldaFloat(row, 4, l.getRadiacion());
                setCeldaFloat(row, 5, l.getPresion());
            }

            //Filas de resumen con fórmulas nativas
            int ultimaFila = fila;
            agregarFilaResumen(hoja, fila++, "PROMEDIO", "AVERAGE", 2, ultimaFila,
                    estiloResumen);
            agregarFilaResumen(hoja, fila++, "MÁXIMO",   "MAX",     2, ultimaFila,
                    estiloResumen);
            agregarFilaResumen(hoja, fila,   "MÍNIMO",   "MIN",     2, ultimaFila,
                    estiloResumen);

            //Autoajustar columnas
            for (int i = 0; i < cols.length; i++) hoja.autoSizeColumn(i);

            wb.write(out);
            log.info("Reporte Excel climático generado: {} filas", datos.size());
            return out.toByteArray();

        } catch (IOException e) {
            throw new ReportGenerationException("Error generando reporte Excel climático", e);
        }
    }

    //Reporte Eléctrico
    public byte[] generarReporteElectrico(List<BeanLecturaElectrica> datos, ReportFilterDTO filtro) {
        try (Workbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet hoja = wb.createSheet("Datos Eléctricos - " + filtro.getFuente());
            CellStyle estiloEncabezado = crearEstiloEncabezado(wb);
            CellStyle estiloResumen    = crearEstiloResumen(wb);

            String[] cols = {"Fecha/Hora", "Voltaje (V)", "Corriente (A)",
                    "Potencia (W)", "Voc (V)", "Energía (Wh)"};
            crearFila(hoja, 0, cols, estiloEncabezado);

            int fila = 1;
            for (BeanLecturaElectrica l : datos) {
                Row row = hoja.createRow(fila++);
                row.createCell(0).setCellValue(l.getFechaLectura().toString());
                setCeldaFloat(row, 1, l.getVoltaje());
                setCeldaFloat(row, 2, l.getCorriente());
                setCeldaFloat(row, 3, l.getPotencia());
                setCeldaFloat(row, 4, l.getVoc());
                setCeldaFloat(row, 5, l.getEnergia());
            }

            int ultimaFila = fila;
            agregarFilaResumen(hoja, fila++, "PROMEDIO", "AVERAGE", 2, ultimaFila,
                    estiloResumen);
            agregarFilaResumen(hoja, fila++, "MÁXIMO",   "MAX",     2, ultimaFila,
                    estiloResumen);
            agregarFilaResumen(hoja, fila,   "MÍNIMO",   "MIN",     2, ultimaFila,
                    estiloResumen);

            for (int i = 0; i < cols.length; i++) hoja.autoSizeColumn(i);

            wb.write(out);
            log.info("Reporte Excel eléctrico generado: {} filas", datos.size());
            return out.toByteArray();

        } catch (IOException e) {
            throw new ReportGenerationException("Error generando reporte Excel eléctrico", e);
        }
    }

    private void crearFila(Sheet hoja, int numFila, String[] valores, CellStyle estilo) {
        Row row = hoja.createRow(numFila);
        for (int i = 0; i < valores.length; i++) {
            Cell cell = row.createCell(i);
            cell.setCellValue(valores[i]);
            cell.setCellStyle(estilo);
        }
    }

    private void setCeldaFloat(Row row, int col, Float valor) {
        Cell cell = row.createCell(col);
        if (valor != null) cell.setCellValue(valor);
    }

    //Agrega una fila de resumen con etiqueta en col 0 y fórmulas AVERAGE/MAX/MIN
    //en las columnas de datos (colInicio..colInicio + columnas)
    private void agregarFilaResumen(Sheet hoja, int numFila, String etiqueta,
                                    String funcion, int colInicio, int ultimaFilaDatos,
                                    CellStyle estilo) {
        Row row = hoja.createRow(numFila);
        Cell etiquetaCell = row.createCell(0);
        etiquetaCell.setCellValue(etiqueta);
        etiquetaCell.setCellStyle(estilo);

        for (int col = colInicio - 1; col < hoja.getRow(0).getLastCellNum(); col++) {
            String colLetra = CellReference.convertNumToColString(col);
            String rango = colLetra + "2:" + colLetra + ultimaFilaDatos;
            Cell cell = row.createCell(col);
            cell.setCellFormula(funcion + "(" + rango + ")");
            cell.setCellStyle(estilo);
        }
    }

    private CellStyle crearEstiloEncabezado(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        return style;
    }

    private CellStyle crearEstiloResumen(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }
}
