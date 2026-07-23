package mx.edu.cenidet.estadias.services.report;

import lombok.extern.slf4j.Slf4j;
import mx.edu.cenidet.estadias.dtos.reportes.ReportFilterDTO;
import mx.edu.cenidet.estadias.excepciones.ReportGenerationException;
import mx.edu.cenidet.estadias.modelos.lectura.BeanLectura;
import mx.edu.cenidet.estadias.modelos.lecturaElectrica.BeanLecturaElectrica;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xddf.usermodel.chart.*;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class ExcelReportService {

    private static final DateTimeFormatter FMT_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final String CENIDET_SIGLAS = "CENIDET";
    private static final String CENIDET_NOMBRE = "Centro Nacional de Investigación y Desarrollo Tecnológico";
    private static final String CENIDET_LUGAR  = "Cuernavaca, Morelos";
    private static final String CITACION =
        "Nota: Si utilizas estos datos en un trabajo académico o de investigación, incluye la siguiente " +
        "referencia: Estación AW-SHEF, Centro Nacional de Investigación y Desarrollo Tecnológico (CENIDET), " +
        "Cuernavaca, Morelos. Datos obtenidos del sistema de monitoreo AW-SHEF.";

    // Layout: filas 0-2 = header institucional, fila 3 = separador,
    //         fila 4 = encabezado tabla, filas 5..N = datos
    private static final int FILA_TABLA_HEADER = 4;
    private static final int FILA_DATOS_INICIO = 5;

    // ── CLIMÁTICO ─────────────────────────────────────────────────────
    public byte[] generarReporteClimatico(List<BeanLectura> datos, ReportFilterDTO filtro) {
        try (XSSFWorkbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            XSSFSheet hoja = wb.createSheet("Variables Climáticas");
            Estilos s = new Estilos(wb);

            record Col(String clave, String label, String unidad) {}
            List<Col> todasCols = List.of(
                new Col("TEMPERATURA", "Temperatura", "°C"),
                new Col("VIENTO",      "Viento",      "m/s"),
                new Col("HUMEDAD",     "Humedad",     "%"),
                new Col("RADIACION",   "Radiación",   "W/m²"),
                new Col("PRESION",     "Presión",     "hPa")
            );
            List<Col> cols = todasCols.stream()
                .filter(c -> filtro.incluirVariable(c.clave())).toList();
            int numCols = cols.size();
            int cenCol  = numCols + 1;

            String[] cabs = cabeceras("Fecha/Hora",
                cols.stream().map(c -> c.label() + " (" + c.unidad() + ")").toList());

            escribirHeader(hoja, s, "Reporte de Variables Climáticas",
                           filtro.getInicio() + " — " + filtro.getFin(), null, cenCol);
            embedLogo(wb, hoja, numCols, cenCol);
            hoja.createRow(3);
            crearFila(hoja, FILA_TABLA_HEADER, cabs, s.encDatos);

            int fila = FILA_DATOS_INICIO;
            for (BeanLectura l : datos) {
                Row row = hoja.createRow(fila);
                CellStyle ds = ((fila - FILA_DATOS_INICIO) % 2 == 1) ? s.datoAlt : null;
                celda(row, 0, l.getFechaLectura().format(FMT_FECHA), ds);
                int col = 1;
                for (Col c : cols) {
                    Float v = switch (c.clave()) {
                        case "TEMPERATURA" -> l.getTemperatura();
                        case "VIENTO"      -> l.getViento();
                        case "HUMEDAD"     -> l.getHumedad();
                        case "RADIACION"   -> l.getRadiacion();
                        case "PRESION"     -> l.getPresion();
                        default            -> null;
                    };
                    celdaFloat(row, col++, v, ds);
                }
                fila++;
            }
            int ultimaFila = fila;

            hoja.createRow(fila++);
            fila = escribirEstadisticas(hoja, s, filtro, cabs, numCols,
                                        FILA_DATOS_INICIO, ultimaFila, fila);
            hoja.createRow(fila++);
            Row rowCita = hoja.createRow(fila++);
            celda(rowCita, 0, CITACION, s.citacion);

            for (int i = 0; i <= cenCol; i++) hoja.autoSizeColumn(i);
            configurarImpresion(hoja);

            // Gráficas nativas (una por variable activa)
            hoja.createRow(fila++);
            List<ColChart> colsChart = new ArrayList<>();
            for (int i = 0; i < cols.size(); i++) {
                colsChart.add(new ColChart(cols.get(i).label(), cols.get(i).unidad(), i + 1));
            }
            agregarGraficasNativas(hoja, colsChart, FILA_DATOS_INICIO, ultimaFila - 1, fila);

            wb.write(out);
            log.info("Excel climático: {} filas, {} columnas", datos.size(), numCols);
            return out.toByteArray();

        } catch (IOException e) {
            throw new ReportGenerationException("Error generando reporte Excel climático", e);
        }
    }

    // ── ELÉCTRICO ─────────────────────────────────────────────────────
    public byte[] generarReporteElectrico(List<BeanLecturaElectrica> datos, ReportFilterDTO filtro) {
        try (XSSFWorkbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            XSSFSheet hoja = wb.createSheet("Variables Eléctricas - " + filtro.getFuente());
            Estilos s = new Estilos(wb);

            record Col(String clave, String label, String unidad) {}
            List<Col> todasCols = List.of(
                new Col("VOLTAJE",   "Voltaje",   "V"),
                new Col("CORRIENTE", "Corriente", "A"),
                new Col("POTENCIA",  "Potencia",  "W"),
                new Col("VOC",       "Voc",       "V"),
                new Col("ENERGIA",   "Energía",   "Wh")
            );
            List<Col> cols = todasCols.stream()
                .filter(c -> filtro.incluirVariable(c.clave())).toList();
            int numCols = cols.size();
            int cenCol  = numCols + 1;

            String[] cabs = cabeceras("Fecha/Hora",
                cols.stream().map(c -> c.label() + " (" + c.unidad() + ")").toList());

            escribirHeader(hoja, s, "Reporte de Variables Eléctricas · " + filtro.getFuente(),
                           filtro.getInicio() + " — " + filtro.getFin(),
                           "Fuente: " + filtro.getFuente(), cenCol);
            embedLogo(wb, hoja, numCols, cenCol);
            hoja.createRow(3);
            crearFila(hoja, FILA_TABLA_HEADER, cabs, s.encDatos);

            int fila = FILA_DATOS_INICIO;
            for (BeanLecturaElectrica l : datos) {
                Row row = hoja.createRow(fila);
                CellStyle ds = ((fila - FILA_DATOS_INICIO) % 2 == 1) ? s.datoAlt : null;
                celda(row, 0, l.getFechaLectura().format(FMT_FECHA), ds);
                int col = 1;
                for (Col c : cols) {
                    Float v = switch (c.clave()) {
                        case "VOLTAJE"   -> l.getVoltaje();
                        case "CORRIENTE" -> l.getCorriente();
                        case "POTENCIA"  -> l.getPotencia();
                        case "VOC"       -> l.getVoc();
                        case "ENERGIA"   -> l.getEnergia();
                        default          -> null;
                    };
                    celdaFloat(row, col++, v, ds);
                }
                fila++;
            }
            int ultimaFila = fila;

            hoja.createRow(fila++);
            fila = escribirEstadisticas(hoja, s, filtro, cabs, numCols,
                                        FILA_DATOS_INICIO, ultimaFila, fila);
            hoja.createRow(fila++);
            Row rowCita = hoja.createRow(fila++);
            celda(rowCita, 0, CITACION, s.citacion);

            for (int i = 0; i <= cenCol; i++) hoja.autoSizeColumn(i);
            configurarImpresion(hoja);

            hoja.createRow(fila++);
            List<ColChart> colsChart = new ArrayList<>();
            for (int i = 0; i < cols.size(); i++) {
                colsChart.add(new ColChart(cols.get(i).label(), cols.get(i).unidad(), i + 1));
            }
            agregarGraficasNativas(hoja, colsChart, FILA_DATOS_INICIO, ultimaFila - 1, fila);

            wb.write(out);
            log.info("Excel eléctrico: {} filas, {} columnas", datos.size(), numCols);
            return out.toByteArray();

        } catch (IOException e) {
            throw new ReportGenerationException("Error generando reporte Excel eléctrico", e);
        }
    }

    // ── Gráficas nativas XDDF ─────────────────────────────────────────
    private record ColChart(String label, String unidad, int colIndex) {}

    private void agregarGraficasNativas(XSSFSheet hoja, List<ColChart> cols,
                                         int filaInicioData, int filaFinData, int filaChart) {
        if (cols.isEmpty() || filaInicioData > filaFinData) return;

        XSSFDrawing drawing = (XSSFDrawing) hoja.createDrawingPatriarch();
        final int CHART_HEIGHT = 16;
        final int CHART_GAP    = 2;
        final int CHART_WIDTH  = 10;

        for (int i = 0; i < cols.size(); i++) {
            ColChart vc = cols.get(i);
            int startRow = filaChart + i * (CHART_HEIGHT + CHART_GAP);
            int endRow   = startRow + CHART_HEIGHT;

            XSSFClientAnchor anchor = drawing.createAnchor(0, 0, 0, 0, 0, startRow, CHART_WIDTH, endRow);
            XSSFChart chart = drawing.createChart(anchor);
            chart.setTitleText(vc.label() + " (" + vc.unidad() + ")");
            chart.setTitleOverlay(false);

            XDDFChartLegend legend = chart.getOrAddLegend();
            legend.setPosition(LegendPosition.BOTTOM);

            XDDFCategoryAxis bottomAxis = chart.createCategoryAxis(AxisPosition.BOTTOM);
            XDDFValueAxis leftAxis = chart.createValueAxis(AxisPosition.LEFT);
            leftAxis.setCrosses(AxisCrosses.AUTO_ZERO);

            XDDFDataSource<String> dates = XDDFDataSourcesFactory.fromStringCellRange(
                hoja, new CellRangeAddress(filaInicioData, filaFinData, 0, 0));
            XDDFNumericalDataSource<Double> values = XDDFDataSourcesFactory.fromNumericCellRange(
                hoja, new CellRangeAddress(filaInicioData, filaFinData, vc.colIndex(), vc.colIndex()));

            XDDFBarChartData barData = (XDDFBarChartData) chart.createData(
                ChartTypes.BAR, bottomAxis, leftAxis);
            barData.setBarDirection(BarDirection.COL);
            XDDFBarChartData.Series series = (XDDFBarChartData.Series) barData.addSeries(dates, values);
            series.setTitle(vc.label() + " (" + vc.unidad() + ")", null);

            chart.plot(barData);
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────

    private void escribirHeader(XSSFSheet hoja, Estilos s, String titulo, String periodo,
                                 Object fuenteExtra, int cenCol) {
        Row r0 = hoja.createRow(0);
        celda(r0, 0, titulo, s.titulo);
        celda(r0, cenCol, CENIDET_SIGLAS, s.cenidet);

        Row r1 = hoja.createRow(1);
        String per = (fuenteExtra != null) ? fuenteExtra + "  ·  Periodo: " + periodo : "Periodo: " + periodo;
        celda(r1, 0, per, s.periodo);
        celda(r1, cenCol, CENIDET_NOMBRE, s.periodo);

        Row r2 = hoja.createRow(2);
        celda(r2, cenCol, CENIDET_LUGAR, s.periodo);
    }

    private int escribirEstadisticas(XSSFSheet hoja, Estilos s, ReportFilterDTO filtro,
                                      String[] cabs, int numCols,
                                      int filaDataInicio, int ultimaFila, int fila) {
        List<String[]> resumen = buildResumen(filtro, 2, filaDataInicio, ultimaFila, numCols);
        if (resumen.isEmpty()) return fila;

        String[] statsHeader = new String[cabs.length];
        statsHeader[0] = "Estadística";
        System.arraycopy(cabs, 1, statsHeader, 1, cabs.length - 1);
        crearFila(hoja, fila++, statsHeader, s.encStats);

        for (String[] ent : resumen) {
            CellStyle est = "MODA".equals(ent[0]) ? s.moda : s.resumen;
            Row row = hoja.createRow(fila++);
            Cell etq = row.createCell(0);
            etq.setCellValue(ent[0]);
            etq.setCellStyle(est);
            for (int col = 1; col < ent.length; col++) {
                Cell cell = row.createCell(col);
                if (ent[col] != null) cell.setCellFormula(ent[col]);
                cell.setCellStyle(est);
            }
        }
        hoja.createRow(fila++);
        return fila;
    }

    private List<String[]> buildResumen(ReportFilterDTO filtro, int colInicio,
                                         int filaDataInicio, int ultimaFila, int numCols) {
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
                String col = CellReference.convertNumToColString(colInicio - 1 + i);
                entrada[i + 1] = sd.funcion() + "(" + col + filaDataInicio + ":" + col + ultimaFila + ")";
            }
            result.add(entrada);
        }
        return result;
    }

    private void configurarImpresion(Sheet hoja) {
        PrintSetup ps = hoja.getPrintSetup();
        ps.setFitWidth((short) 1);
        ps.setFitHeight((short) 0);
        hoja.setAutobreaks(true);
        hoja.setFitToPage(true);
    }

    private String[] cabeceras(String primero, List<String> resto) {
        String[] arr = new String[1 + resto.size()];
        arr[0] = primero;
        for (int i = 0; i < resto.size(); i++) arr[i + 1] = resto.get(i);
        return arr;
    }

    private void crearFila(Sheet hoja, int numFila, String[] valores, CellStyle estilo) {
        Row row = hoja.createRow(numFila);
        for (int i = 0; i < valores.length; i++) {
            Cell cell = row.createCell(i);
            cell.setCellValue(valores[i] != null ? valores[i] : "");
            if (estilo != null) cell.setCellStyle(estilo);
        }
    }

    private void celda(Row row, int col, String valor, CellStyle estilo) {
        Cell cell = row.createCell(col);
        cell.setCellValue(valor != null ? valor : "");
        if (estilo != null) cell.setCellStyle(estilo);
    }

    private void celdaFloat(Row row, int col, Float valor, CellStyle estilo) {
        Cell cell = row.createCell(col);
        if (valor != null) cell.setCellValue(valor);
        if (estilo != null) cell.setCellStyle(estilo);
    }

    private void embedLogo(XSSFWorkbook wb, Sheet hoja, int numCols, int cenCol) {
        try {
            byte[] logoBytes = new ClassPathResource("fop/templates/Logo_cenidet.png")
                    .getInputStream().readAllBytes();
            int picIdx = wb.addPicture(logoBytes, Workbook.PICTURE_TYPE_PNG);
            Drawing<?> drawing = hoja.createDrawingPatriarch();
            ClientAnchor anchor = wb.getCreationHelper().createClientAnchor();
            anchor.setCol1(Math.max(0, cenCol - 1)); anchor.setRow1(0);
            anchor.setCol2(cenCol + 1);              anchor.setRow2(3);
            drawing.createPicture(anchor, picIdx);
        } catch (Exception e) {
            log.debug("Logo CENIDET no incrustado en Excel: {}", e.getMessage());
        }
    }

    // ── Estilos ────────────────────────────────────────────────────────
    private static class Estilos {
        final XSSFCellStyle titulo;
        final XSSFCellStyle cenidet;
        final XSSFCellStyle periodo;
        final XSSFCellStyle encDatos;
        final XSSFCellStyle datoAlt;
        final XSSFCellStyle encStats;
        final CellStyle     resumen;
        final CellStyle     moda;
        final XSSFCellStyle citacion;

        Estilos(XSSFWorkbook wb) {
            byte[] azul   = {(byte)0x00, (byte)0x3B, (byte)0x8E};
            byte[] gris   = {(byte)0x55, (byte)0x55, (byte)0x55};
            byte[] blanco = {(byte)0xFF, (byte)0xFF, (byte)0xFF};
            byte[] azulCl = {(byte)0xE3, (byte)0xF2, (byte)0xFD};
            byte[] verde  = {(byte)0x1B, (byte)0x5E, (byte)0x20};
            byte[] citCol = {(byte)0x88, (byte)0x88, (byte)0x88};

            titulo = wb.createCellStyle();
            XSSFFont ft = wb.createFont();
            ft.setBold(true); ft.setFontHeightInPoints((short) 14);
            ft.setColor(new XSSFColor(azul, null));
            titulo.setFont(ft);

            cenidet = wb.createCellStyle();
            XSSFFont fc = wb.createFont();
            fc.setBold(true); fc.setItalic(true); fc.setFontHeightInPoints((short) 11);
            fc.setColor(new XSSFColor(azul, null));
            cenidet.setFont(fc);
            cenidet.setAlignment(HorizontalAlignment.RIGHT);

            periodo = wb.createCellStyle();
            XSSFFont fp = wb.createFont();
            fp.setFontHeightInPoints((short) 9);
            fp.setColor(new XSSFColor(gris, null));
            periodo.setFont(fp);

            encDatos = wb.createCellStyle();
            encDatos.setFillForegroundColor(new XSSFColor(azul, null));
            encDatos.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            encDatos.setBorderBottom(BorderStyle.THIN);
            XSSFFont fed = wb.createFont();
            fed.setBold(true); fed.setColor(new XSSFColor(blanco, null));
            encDatos.setFont(fed);

            datoAlt = wb.createCellStyle();
            datoAlt.setFillForegroundColor(new XSSFColor(azulCl, null));
            datoAlt.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            encStats = wb.createCellStyle();
            encStats.setFillForegroundColor(new XSSFColor(verde, null));
            encStats.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            XSSFFont fes = wb.createFont();
            fes.setBold(true); fes.setColor(new XSSFColor(blanco, null));
            encStats.setFont(fes);

            resumen = wb.createCellStyle();
            Font fr = wb.createFont(); fr.setBold(true); resumen.setFont(fr);
            resumen.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
            resumen.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            moda = wb.createCellStyle();
            Font fm = wb.createFont(); fm.setBold(true); moda.setFont(fm);
            moda.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
            moda.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            citacion = wb.createCellStyle();
            XSSFFont fci = wb.createFont();
            fci.setItalic(true); fci.setFontHeightInPoints((short) 9);
            fci.setColor(new XSSFColor(citCol, null));
            citacion.setFont(fci);
            citacion.setWrapText(true);
        }
    }
}
