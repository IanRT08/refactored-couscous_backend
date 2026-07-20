package mx.edu.cenidet.estadias.services.report;

import mx.edu.cenidet.estadias.dtos.reportes.ReportFilterDTO;
import mx.edu.cenidet.estadias.modelos.lectura.BeanLectura;
import mx.edu.cenidet.estadias.modelos.lecturaElectrica.BeanLecturaElectrica;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ReportDataAssembler {

    // ── CLIMÁTICO ──────────────────────────────────────────────────────
    public String toXmlClimatico(List<BeanLectura> datos, ReportFilterDTO filtro) {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<reporte tipo=\"CLIMATICO\">\n");
        xml.append("  <periodo inicio=\"").append(filtro.getInicio())
                .append("\" fin=\"").append(filtro.getFin()).append("\"/>\n");

        String varsStr = (filtro.getVariables() != null && !filtro.getVariables().isEmpty())
                ? String.join(",", filtro.getVariables())
                : "TEMPERATURA,VIENTO,HUMEDAD,RADIACION,PRESION";
        xml.append("  <variables>").append(varsStr).append("</variables>\n");

        appendResumenClimatico(xml, datos, filtro);

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

    private void appendResumenClimatico(StringBuilder xml, List<BeanLectura> datos, ReportFilterDTO filtro) {
        if (datos.isEmpty()) return;
        record VE(String campo, Function<BeanLectura, Float> getter) {}
        List<VE> extractors = List.of(
            new VE("temperatura", BeanLectura::getTemperatura),
            new VE("viento",      BeanLectura::getViento),
            new VE("humedad",     BeanLectura::getHumedad),
            new VE("radiacion",   BeanLectura::getRadiacion),
            new VE("presion",     BeanLectura::getPresion)
        );
        appendResumen(xml, extractors.stream().map(e -> Map.entry(e.campo(),
            datos.stream().map(e.getter()).filter(Objects::nonNull).toList()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new)),
            filtro);
    }

    // ── ELÉCTRICO ──────────────────────────────────────────────────────
    public String toXmlElectrico(List<BeanLecturaElectrica> datos, ReportFilterDTO filtro) {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<reporte tipo=\"ELECTRICO\">\n");
        xml.append("  <periodo inicio=\"").append(filtro.getInicio())
                .append("\" fin=\"").append(filtro.getFin())
                .append("\" fuente=\"").append(filtro.getFuente()).append("\"/>\n");

        String varsStr = (filtro.getVariables() != null && !filtro.getVariables().isEmpty())
                ? String.join(",", filtro.getVariables())
                : "VOLTAJE,CORRIENTE,POTENCIA,VOC,ENERGIA";
        xml.append("  <variables>").append(varsStr).append("</variables>\n");

        appendResumenElectrico(xml, datos, filtro);

        xml.append("  <lecturas>\n");
        for (BeanLecturaElectrica l : datos) {
            xml.append("    <lectura fecha=\"").append(l.getFechaLectura()).append("\">\n");
            xml.append("      <voltaje>").append(fmt(l.getVoltaje())).append("</voltaje>\n");
            xml.append("      <corriente>").append(fmt(l.getCorriente())).append("</corriente>\n");
            xml.append("      <potencia>").append(fmt(l.getPotencia())).append("</potencia>\n");
            xml.append("      <voc>").append(fmt(l.getVoc())).append("</voc>\n");
            xml.append("      <energia>").append(fmt(l.getEnergia())).append("</energia>\n");
            xml.append("    </lectura>\n");
        }
        xml.append("  </lecturas>\n</reporte>");
        return xml.toString();
    }

    private void appendResumenElectrico(StringBuilder xml, List<BeanLecturaElectrica> datos, ReportFilterDTO filtro) {
        if (datos.isEmpty()) return;
        record VE(String campo, Function<BeanLecturaElectrica, Float> getter) {}
        List<VE> extractors = List.of(
            new VE("voltaje",   BeanLecturaElectrica::getVoltaje),
            new VE("corriente", BeanLecturaElectrica::getCorriente),
            new VE("potencia",  BeanLecturaElectrica::getPotencia),
            new VE("voc",       BeanLecturaElectrica::getVoc),
            new VE("energia",   BeanLecturaElectrica::getEnergia)
        );
        appendResumen(xml, extractors.stream().map(e -> Map.entry(e.campo(),
            datos.stream().map(e.getter()).filter(Objects::nonNull).toList()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new)),
            filtro);
    }

    // ── Lógica de resumen compartida ───────────────────────────────────
    private void appendResumen(StringBuilder xml, Map<String, List<Float>> valoresPorCampo, ReportFilterDTO filtro) {
        record StatSpec(String tipo, String etiqueta) {}
        List<StatSpec> stats = List.of(
            new StatSpec("PROMEDIO", "Promedio"),
            new StatSpec("MAXIMO",   "Máximo"),
            new StatSpec("MINIMO",   "Mínimo"),
            new StatSpec("MODA",     "Moda")
        );
        boolean hayAlgunoActivo = stats.stream().anyMatch(s -> filtro.incluirEstadistica(s.tipo()));
        if (!hayAlgunoActivo) return;

        xml.append("  <resumen>\n");
        for (StatSpec st : stats) {
            if (!filtro.incluirEstadistica(st.tipo())) continue;
            xml.append("    <fila tipo=\"").append(st.tipo())
               .append("\" etiqueta=\"").append(st.etiqueta()).append("\">\n");
            for (Map.Entry<String, List<Float>> entry : valoresPorCampo.entrySet()) {
                Float resultado = calcularStat(st.tipo(), entry.getValue());
                xml.append("      <").append(entry.getKey()).append(">")
                   .append(fmt(resultado))
                   .append("</").append(entry.getKey()).append(">\n");
            }
            xml.append("    </fila>\n");
        }
        xml.append("  </resumen>\n");
    }

    private Float calcularStat(String tipo, List<Float> vals) {
        if (vals == null || vals.isEmpty()) return null;
        return switch (tipo) {
            case "PROMEDIO" -> (float)(vals.stream().mapToDouble(Float::doubleValue).sum() / vals.size());
            case "MAXIMO"   -> vals.stream().max(Float::compareTo).orElse(null);
            case "MINIMO"   -> vals.stream().min(Float::compareTo).orElse(null);
            case "MODA"     -> vals.stream()
                .collect(Collectors.groupingBy(
                    v -> BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP).floatValue(),
                    Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
            default -> null;
        };
    }

    private String fmt(Float v) {
        return v != null ? String.format("%.4f", v) : "";
    }
}
