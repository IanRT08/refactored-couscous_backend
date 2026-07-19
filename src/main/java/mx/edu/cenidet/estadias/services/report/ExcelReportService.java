package mx.edu.cenidet.estadias.services.report;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.edu.cenidet.estadias.dtos.reportes.ReportFilterDTO;
import mx.edu.cenidet.estadias.excepciones.ReportGenerationException;
import mx.edu.cenidet.estadias.modelos.lectura.BeanLectura;
import mx.edu.cenidet.estadias.modelos.lecturaElectrica.BeanLecturaElectrica;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExcelReportService {

    private static final String CENIDET_SIGLAS = "CENIDET";
    private static final String CENIDET_NOMBRE = "Centro Nacional de Investigación y Desarrollo Tecnológico";
    private static final String CENIDET_LUGAR  = "Cuernavaca, Morelos";
    private static final String CITACION =
        "Nota: Si utilizas estos datos en un trabajo académico o de investigación, incluye la siguiente " +
        "referencia: Estación AW-SHEF, Centro Nacional de Investigación y Desarrollo Tecnológico (CENIDET), " +
        "Cuernavaca, Morelos. Datos obtenidos del sistema de monitoreo AW-SHEF.";

    // Layout: rows 0-2 = header institucional, row 3 = blank, row 4 = tabla header,
    //         rows 5..N+4 = datos, blank, stats, blank, citación, blank, gráfica
    private static final int FILA_TABLA_HEADER = 4;
    private static final int FILA_DATOS_INICIO = 5;   // Excel 1-indexed

    private final ChartGeneratorService chartGeneratorService;

    // ── CLIMÁTICO ─────────────────────────────────────────────────────
    public byte[] generarReporteClimatico(List<BeanLectura> datos, ReportFilterDTO filtro) {
        try (XSSFWorkbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet hoja = wb.createSheet("Variables Climáticas");
            Estilos s = new Estilos(wb);

            record Col(String clave, String cabecera) {}
            List<Col> todasCols = List.of(
                new Col("TEMPERATURA", "Temperatura (°C)"),
                new Col("VIENTO",      "Viento (m/s)"),
                new Col("HUMEDAD",     "Humedad (%)"),
                new Col("RADIACION",   "Radiación (W/m²)"),
                new Col("PRESION",     "Presión (hPa)")
            );
            List<Col> cols = todasCols.stream()
                .filter(c -> filtro.incluirVariable(c.clave())).toList();
            int numCols = cols.size();
            int cenCol  = numCols + 1;

            // ── Header institucional (filas 0–2) ──────────────────────
            escribirHeader(hoja, s, "Reporte de Variables Climáticas",
                           filtro.getInicio() + " — " + filtro.getFin(),
                           null, cenCol);
            embedLogoInstitucional(wb, hoja, numCols, cenCol);

            // Fila 3: separador
            hoja.createRow(3);

            // Fila 4: encabezado tabla
            String[] cabs = cabeceras("Fecha/Hora", cols.stream().map(Col::cabecera).toList());
            crearFila(hoja, FILA_TABLA_HEADER, cabs, s.encDatos);

            // Filas 5..N+4: datos
            int fila = FILA_TABLA_HEADER + 1;
            for (BeanLectura l : datos) {
                Row row = hoja.createRow(fila);
                CellStyle ds = ((fila - (FILA_TABLA_HEADER + 1)) % 2 == 1) ? s.datoAlt : null;
                celda(row, 0, l.getFechaLectura().toString(), ds);
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
            int ultimaFila = fila; // = Excel 1-based row number of last data row

            hoja.createRow(fila++); // separador

            // Estadísticas
            fila = escribirEstadisticas(hoja, s, filtro, cabs, numCols, FILA_DATOS_INICIO, ultimaFila, fila);

            // Citación
            hoja.createRow(fila++);
            Row rowCita = hoja.createRow(fila++);
            celda(rowCita, 0, CITACION, s.citacion);

            // Autosize + impresión
            for (int i = 0; i <= cenCol; i++) hoja.autoSizeColumn(i);
            configurarImpresion(hoja);

            // Gráfica
            hoja.createRow(fila++);
            byte[] png = chartGeneratorService.generarGraficaClimatica(datos, filtro.getVariables());
            if (png.length > 0) embedImagen(wb, hoja, png, fila, numCols);

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

            Sheet hoja = wb.createSheet("Variables Eléctricas - " + filtro.getFuente());
            Estilos s = new Estilos(wb);

            record Col(String clave, String cabecera) {}
            List<Col> todasCols = List.of(
                new Col("VOLTAJE",   "Voltaje (V)"),
                new Col("CORRIENTE", "Corriente (A)"),
                new Col("POTENCIA",  "Potencia (W)"),
                new Col("VOC",       "Voc (V)"),
                new Col("ENERGIA",   "Energía (Wh)")
            );
            List<Col> cols = todasCols.stream()
                .filter(c -> filtro.incluirVariable(c.clave())).toList();
            int numCols = cols.size();
            int cenCol  = numCols + 1;

            escribirHeader(hoja, s, "Reporte de Variables Eléctricas · " + filtro.getFuente(),
                           filtro.getInicio() + " — " + filtro.getFin(),
                           "Fuente: " + filtro.getFuente(), cenCol);
            embedLogoInstitucional(wb, hoja, numCols, cenCol);

            hoja.createRow(3);

            String[] cabs = cabeceras("Fecha/Hora", cols.stream().map(Col::cabecera).toList());
            crearFila(hoja, FILA_TABLA_HEADER, cabs, s.encDatos);

            int fila = FILA_TABLA_HEADER + 1;
            for (BeanLecturaElectrica l : datos) {
                Row row = hoja.createRow(fila);
                CellStyle ds = ((fila - (FILA_TABLA_HEADER + 1)) % 2 == 1) ? s.datoAlt : null;
                celda(row, 0, l.getFechaLectura().toString(), ds);
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

            fila = escribirEstadisticas(hoja, s, filtro, cabs, numCols, FILA_DATOS_INICIO, ultimaFila, fila);

            hoja.createRow(fila++);
            Row rowCita = hoja.createRow(fila++);
            celda(rowCita, 0, CITACION, s.citacion);

            for (int i = 0; i <= cenCol; i++) hoja.autoSizeColumn(i);
            configurarImpresion(hoja);

            hoja.createRow(fila++);
            byte[] png = chartGeneratorService.generarGraficaElectrica(datos, filtro.getVariables());
            if (png.length > 0) embedImagen(wb, hoja, png, fila, numCols);

            wb.write(out);
            log.info("Excel eléctrico: {} filas, {} columnas", datos.size(), numCols);
            return out.toByteArray();

        } catch (IOException e) {
            throw new ReportGenerationException("Error generando reporte Excel eléctrico", e);
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────

    private void escribirHeader(Sheet hoja, Estilos s, String titulo, String periodo,
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

    private int escribirEstadisticas(Sheet hoja, Estilos s, ReportFilterDTO filtro,
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

    private void embedImagen(XSSFWorkbook wb, Sheet hoja, byte[] png, int filaInicio, int numCols) {
        int picIdx = wb.addPicture(png, Workbook.PICTURE_TYPE_PNG);
        Drawing<?> drawing = hoja.createDrawingPatriarch();
        ClientAnchor anchor = wb.getCreationHelper().createClientAnchor();
        anchor.setCol1(0); anchor.setRow1(filaInicio);
        anchor.setCol2(Math.min(numCols + 1, 8)); anchor.setRow2(filaInicio + 28);
        drawing.createPicture(anchor, picIdx);
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

    private void embedLogoInstitucional(XSSFWorkbook wb, Sheet hoja, int numCols, int cenCol) {
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
            log.debug("Logo CENIDET no incrustado en Excel, se usa texto: {}", e.getMessage());
        }
    }

    // ── Inner class para agrupar estilos ──────────────────────────────
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
            // Azul oscuro institucional
            byte[] azul   = {(byte)0x00, (byte)0x3B, (byte)0x8E};
            byte[] gris   = {(byte)0x55, (byte)0x55, (byte)0x55};
            byte[] blanco = {(byte)0xFF, (byte)0xFF, (byte)0xFF};
            byte[] azulCl = {(byte)0xE3, (byte)0xF2, (byte)0xFD};
            byte[] verde  = {(byte)0x1B, (byte)0x5E, (byte)0x20};
            byte[] citCol = {(byte)0x88, (byte)0x88, (byte)0x88};

            // Título
            titulo = wb.createCellStyle();
            XSSFFont ft = wb.createFont();
            ft.setBold(true); ft.setFontHeightInPoints((short) 14);
            ft.setColor(new XSSFColor(azul, null));
            titulo.setFont(ft);

            // CENIDET (derecha)
            cenidet = wb.createCellStyle();
            XSSFFont fc = wb.createFont();
            fc.setBold(true); fc.setItalic(true); fc.setFontHeightInPoints((short) 11);
            fc.setColor(new XSSFColor(azul, null));
            cenidet.setFont(fc);
            cenidet.setAlignment(HorizontalAlignment.RIGHT);

            // Periodo / subtítulo
            periodo = wb.createCellStyle();
            XSSFFont fp = wb.createFont();
            fp.setFontHeightInPoints((short) 9);
            fp.setColor(new XSSFColor(gris, null));
            periodo.setFont(fp);

            // Encabezado datos (fondo azul oscuro, texto blanco)
            encDatos = wb.createCellStyle();
            encDatos.setFillForegroundColor(new XSSFColor(azul, null));
            encDatos.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            encDatos.setBorderBottom(BorderStyle.THIN);
            XSSFFont fed = wb.createFont();
            fed.setBold(true); fed.setColor(new XSSFColor(blanco, null));
            encDatos.setFont(fed);

            // Fila dato alternada (azul muy claro)
            datoAlt = wb.createCellStyle();
            datoAlt.setFillForegroundColor(new XSSFColor(azulCl, null));
            datoAlt.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // Encabezado estadísticas (fondo verde oscuro, texto blanco)
            encStats = wb.createCellStyle();
            encStats.setFillForegroundColor(new XSSFColor(verde, null));
            encStats.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            XSSFFont fes = wb.createFont();
            fes.setBold(true); fes.setColor(new XSSFColor(blanco, null));
            encStats.setFont(fes);

            // Fila resumen (amarillo)
            resumen = wb.createCellStyle();
            Font fr = wb.createFont(); fr.setBold(true); resumen.setFont(fr);
            resumen.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
            resumen.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // Fila moda (verde claro)
            moda = wb.createCellStyle();
            Font fm = wb.createFont(); fm.setBold(true); moda.setFont(fm);
            moda.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
            moda.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // Citación (itálica, gris)
            citacion = wb.createCellStyle();
            XSSFFont fci = wb.createFont();
            fci.setItalic(true); fci.setFontHeightInPoints((short) 9);
            fci.setColor(new XSSFColor(citCol, null));
            citacion.setFont(fci);
            citacion.setWrapText(true);
        }
    }
}
