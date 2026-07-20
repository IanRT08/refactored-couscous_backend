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
import java.util.Base64;
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
        byte[] png = chartGeneratorService.generarGraficaClimatica(datos, filtro.getVariables());
        String graficaBase64 = png.length > 0 ? Base64.getEncoder().encodeToString(png) : null;
        return transformarPdf(xml, "fop/templates/reporte-climatico.xsl", graficaBase64);
    }

    //Reporte PDF Eléctrico
    public byte[] generarReporteElectrico(List<BeanLecturaElectrica> datos, ReportFilterDTO filtro) {
        String xml = assembler.toXmlElectrico(datos, filtro);
        byte[] png = chartGeneratorService.generarGraficaElectrica(datos, filtro.getVariables());
        String graficaBase64 = png.length > 0 ? Base64.getEncoder().encodeToString(png) : null;
        return transformarPdf(xml, "fop/templates/reporte-electrico.xsl", graficaBase64);
    }

    //Motor de transformación XSL-FO a PDF
    private byte[] transformarPdf(String xmlDatos, String rutaPlantilla, String graficaBase64) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            Fop fop = fopFactory.newFop(MimeConstants.MIME_PDF, out);

            Source plantilla = new StreamSource(
                    new ClassPathResource(rutaPlantilla).getInputStream());
            Transformer transformer =
                    TransformerFactory.newInstance().newTransformer(plantilla);

            String logoBase64 = cargarLogoBase64();
            if (logoBase64 != null) {
                transformer.setParameter("logoBase64", logoBase64);
            }
            if (graficaBase64 != null) {
                transformer.setParameter("graficaBase64", graficaBase64);
            }

            Source src = new StreamSource(new StringReader(xmlDatos));
            Result res = new SAXResult(fop.getDefaultHandler());
            transformer.transform(src, res);

            log.info("Reporte PDF generado con plantilla: {}", rutaPlantilla);
            return out.toByteArray();

        } catch (Exception e) {
            throw new ReportGenerationException("Error generando PDF: " + e.getMessage(), e);
        }
    }

    private String cargarLogoBase64() {
        try {
            byte[] bytes = new ClassPathResource("fop/templates/Logo_cenidet.png")
                    .getInputStream().readAllBytes();
            return Base64.getEncoder().encodeToString(bytes);
        } catch (Exception e) {
            log.warn("Logo CENIDET no disponible, se usará texto como respaldo: {}", e.getMessage());
            return null;
        }
    }
}
