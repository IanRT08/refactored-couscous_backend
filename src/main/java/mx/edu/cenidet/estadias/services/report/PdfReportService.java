package mx.edu.cenidet.estadias.services.report;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.edu.cenidet.estadias.dtos.reportes.ReportFilterDTO;
import mx.edu.cenidet.estadias.excepciones.ReportGenerationException;
import mx.edu.cenidet.estadias.modelos.lectura.BeanLectura;
import mx.edu.cenidet.estadias.modelos.lecturaElectrica.BeanLecturaElectrica;
import org.apache.fop.apps.Fop;
import org.apache.fop.apps.FopFactory;
import org.apache.xmlgraphics.util.MimeConstants;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import javax.xml.transform.*;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PdfReportService {

    private final FopFactory           fopFactory;
    private final ReportDataAssembler  assembler;
    private final ChartGeneratorService chartGeneratorService;

    //Reporte PDF Climático
    public byte[] generarReporteClimatico(List<BeanLectura> datos, ReportFilterDTO filtro) {
        String xml = assembler.toXmlClimatico(datos, filtro);
        byte[] png = chartGeneratorService.generarGraficaClimatica(datos, filtro.getVariables(), filtro.getTipoGrafica());
        return transformarPdf(xml, "fop/templates/reporte-climatico.xsl", png.length > 0 ? png : null);
    }

    //Reporte PDF Eléctrico
    public byte[] generarReporteElectrico(List<BeanLecturaElectrica> datos, ReportFilterDTO filtro) {
        String xml = assembler.toXmlElectrico(datos, filtro);
        byte[] png = chartGeneratorService.generarGraficaElectrica(datos, filtro.getVariables(), filtro.getTipoGrafica());
        return transformarPdf(xml, "fop/templates/reporte-electrico.xsl", png.length > 0 ? png : null);
    }

    //Motor de transformación XSL-FO a PDF
    private byte[] transformarPdf(String xmlDatos, String rutaPlantilla, byte[] graficaPng) {
        Path tempLogo  = null;
        Path tempChart = null;
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            Fop fop = fopFactory.newFop(MimeConstants.MIME_PDF, out);

            Source plantilla = new StreamSource(
                    new ClassPathResource(rutaPlantilla).getInputStream());
            Transformer transformer =
                    TransformerFactory.newInstance().newTransformer(plantilla);

            // Logo → archivo temporal para que FOP lo cargue como file://
            byte[] logoBytes = cargarLogoBytes();
            if (logoBytes != null) {
                tempLogo = Files.createTempFile("fop-logo-", ".png");
                Files.write(tempLogo, logoBytes);
                transformer.setParameter("logoUri", tempLogo.toUri().toString());
            }

            // Gráfica → archivo temporal
            if (graficaPng != null) {
                tempChart = Files.createTempFile("fop-chart-", ".png");
                Files.write(tempChart, graficaPng);
                transformer.setParameter("graficaUri", tempChart.toUri().toString());
            }

            Source src = new StreamSource(new StringReader(xmlDatos));
            Result res = new SAXResult(fop.getDefaultHandler());
            transformer.transform(src, res);

            log.info("Reporte PDF generado con plantilla: {}", rutaPlantilla);
            return out.toByteArray();

        } catch (Exception e) {
            throw new ReportGenerationException("Error generando PDF: " + e.getMessage(), e);
        } finally {
            borrarTempFile(tempLogo);
            borrarTempFile(tempChart);
        }
    }

    private byte[] cargarLogoBytes() {
        try {
            return new ClassPathResource("fop/templates/Logo_cenidet.png")
                    .getInputStream().readAllBytes();
        } catch (Exception e) {
            log.warn("Logo CENIDET no disponible, se usará texto como respaldo: {}", e.getMessage());
            return null;
        }
    }

    private void borrarTempFile(Path path) {
        if (path != null) {
            try { Files.deleteIfExists(path); } catch (Exception ignored) {}
        }
    }
}
