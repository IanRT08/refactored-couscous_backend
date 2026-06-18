package mx.edu.cenidet.estadias.services.report;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.edu.cenidet.estadias.dtos.reportes.ReportFilterDTO;
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
import java.util.List;

// Módulo 7 — Generación de reportes PDF con Apache FOP (fop-core 2.11).
// Transforma XML (generado por ReportDataAssembler) usando plantillas
// XSL-FO en resources/fop/templates/ → PDF vectorial con membrete UTEZ/CENIDET.
@Service
@RequiredArgsConstructor
@Slf4j
public class PdfReportService {

    // Bean configurado en FopConfig.java (ver abajo en este archivo)
    private final FopFactory         fopFactory;
    private final ReportDataAssembler assembler;

    // ── Reporte PDF Climático ─────────────────────────────────
    public byte[] generarReporteClimatico(List<BeanLectura> datos, ReportFilterDTO filtro) {
        String xml = assembler.toXmlClimatico(datos, filtro);
        return transformarPdf(xml, "fop/templates/reporte-climatico.xsl");
    }

    // ── Reporte PDF Eléctrico ─────────────────────────────────
    public byte[] generarReporteElectrico(List<BeanLecturaElectrica> datos, ReportFilterDTO filtro) {
        String xml = assembler.toXmlElectrico(datos, filtro);
        return transformarPdf(xml, "fop/templates/reporte-electrico.xsl");
    }

    // ── Motor de transformación XSL-FO → PDF ─────────────────
    private byte[] transformarPdf(String xmlDatos, String rutaPlantilla) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            // FOP procesa el resultado de la transformación XSLT
            Fop fop = fopFactory.newFop(MimeConstants.MIME_PDF, out);

            Source plantilla = new StreamSource(
                    new ClassPathResource(rutaPlantilla).getInputStream());
            Transformer transformer =
                    TransformerFactory.newInstance().newTransformer(plantilla);

            Source src = new StreamSource(new StringReader(xmlDatos));
            Result res = new SAXResult(fop.getDefaultHandler());

            transformer.transform(src, res);

            log.info("Reporte PDF generado con plantilla: {}", rutaPlantilla);
            return out.toByteArray();

        } catch (Exception e) {
            throw new ReportGenerationException("Error generando PDF: " + e.getMessage(), e);
        }
    }
}
