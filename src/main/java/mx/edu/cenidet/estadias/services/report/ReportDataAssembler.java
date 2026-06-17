package mx.edu.cenidet.estadias.services.report;


import mx.edu.cenidet.estadias.dtos.reportes.ReportFilterDTO;
import mx.edu.cenidet.estadias.modelos.lectura.BeanLectura;
import mx.edu.cenidet.estadias.modelos.lecturaElectrica.BeanLecturaElectrica;
import org.springframework.stereotype.Component;

import java.util.List;

// Construye el XML fuente que Apache FOP transforma en PDF.
// Usa concatenación de strings (sin JAXP/DOM) para mantener
// la dependencia mínima. El XML generado es consumido por
// las plantillas XSL-FO en resources/fop/templates/.
@Component
public class ReportDataAssembler {

    public String toXmlClimatico(List<BeanLectura> datos, ReportFilterDTO filtro) {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<reporte tipo=\"CLIMATICO\">\n");
        xml.append("  <periodo inicio=\"").append(filtro.getInicio())
                .append("\" fin=\"").append(filtro.getFin()).append("\"/>\n");
        xml.append("  <lecturas>\n");

        for (BeanLectura l : datos) {
            xml.append("    <lectura fecha=\"").append(l.getFechaLectura()).append("\">\n");
            xml.append("      <temperatura>").append(fmt(l.getTemperatura())).append("</temperatura>\n");
            xml.append("      <viento>").append(fmt(l.getViento())).append("</viento>\n");
            xml.append("      <humedad>").append(fmt(l.getHumedad())).append("</humedad>\n");
            xml.append("      <radiacion>").append(fmt(l.getRadiacion())).append("</radiacion>\n");
            xml.append("      <presion>").append(fmt(l.getPresion())).append("</presion>\n");
            xml.append("    </lectura>\n");
        }

        xml.append("  </lecturas>\n</reporte>");
        return xml.toString();
    }

    public String toXmlElectrico(List<BeanLecturaElectrica> datos, ReportFilterDTO filtro) {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<reporte tipo=\"ELECTRICO\">\n");
        xml.append("  <periodo inicio=\"").append(filtro.getInicio())
                .append("\" fin=\"").append(filtro.getFin()).append("\"/>\n");
        xml.append("  <lecturas>\n");

        for (BeanLecturaElectrica l : datos) {
            xml.append("    <lectura fecha=\"").append(l.getFechaLectura()).append("\">\n");
            xml.append("      <corriente>").append(fmt(l.getCorriente())).append("</corriente>\n");
            xml.append("      <voltaje>").append(fmt(l.getVoltaje())).append("</voltaje>\n");
            xml.append("      <potencia>").append(fmt(l.getPotencia())).append("</potencia>\n");
            xml.append("      <energia>").append(fmt(l.getEnergia())).append("</energia>\n");
            xml.append("    </lectura>\n");
        }

        xml.append("  </lecturas>\n</reporte>");
        return xml.toString();
    }

    private String fmt(Float v) {
        return v != null ? String.format("%.4f", v) : "";
    }
}

