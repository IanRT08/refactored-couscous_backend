package mx.edu.cenidet.estadias.services.report;

import lombok.extern.slf4j.Slf4j;
import mx.edu.cenidet.estadias.modelos.lectura.BeanLectura;
import mx.edu.cenidet.estadias.modelos.lecturaElectrica.BeanLecturaElectrica;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CombinedDomainXYPlot;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.time.Minute;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.function.Function;

@Service
@Slf4j
public class ChartGeneratorService {

    private record VarConfig<T>(String clave, String etiqueta, String unidad,
                                 Color color, Function<T, Float> getter) {}

    // ── Climático ─────────────────────────────────────────────────────
    public byte[] generarGraficaClimatica(List<BeanLectura> datos, List<String> variables, String tipoGrafica) {
        List<VarConfig<BeanLectura>> configs = List.of(
            new VarConfig<>("TEMPERATURA", "Temperatura", "°C",    new Color(224, 80,  80),  BeanLectura::getTemperatura),
            new VarConfig<>("VIENTO",      "Viento",      "m/s",   new Color(23,  102, 130), BeanLectura::getViento),
            new VarConfig<>("HUMEDAD",     "Humedad",     "%",     new Color(27,  182, 165), BeanLectura::getHumedad),
            new VarConfig<>("RADIACION",   "Radiación",   "W/m²",  new Color(255, 140, 0),   BeanLectura::getRadiacion),
            new VarConfig<>("PRESION",     "Presión",     "hPa",   new Color(139, 92,  246), BeanLectura::getPresion)
        );
        List<VarConfig<BeanLectura>> activas = configs.stream()
            .filter(c -> variables == null || variables.isEmpty() || variables.contains(c.clave()))
            .toList();
        return renderizarClimatico(datos, activas, tipoGrafica);
    }

    // ── Eléctrico ─────────────────────────────────────────────────────
    public byte[] generarGraficaElectrica(List<BeanLecturaElectrica> datos, List<String> variables, String tipoGrafica) {
        List<VarConfig<BeanLecturaElectrica>> configs = List.of(
            new VarConfig<>("VOLTAJE",   "Voltaje",   "V",  new Color(23,  102, 130), BeanLecturaElectrica::getVoltaje),
            new VarConfig<>("CORRIENTE", "Corriente", "A",  new Color(224, 80,  80),  BeanLecturaElectrica::getCorriente),
            new VarConfig<>("POTENCIA",  "Potencia",  "W",  new Color(27,  182, 165), BeanLecturaElectrica::getPotencia),
            new VarConfig<>("VOC",       "Voc",       "V",  new Color(255, 140, 0),   BeanLecturaElectrica::getVoc),
            new VarConfig<>("ENERGIA",   "Energía",   "Wh", new Color(139, 92,  246), BeanLecturaElectrica::getEnergia)
        );
        List<VarConfig<BeanLecturaElectrica>> activas = configs.stream()
            .filter(c -> variables == null || variables.isEmpty() || variables.contains(c.clave()))
            .toList();
        return renderizarElectrico(datos, activas, tipoGrafica);
    }

    // ── Render genérico climático ──────────────────────────────────────
    private byte[] renderizarClimatico(List<BeanLectura> datos,
                                       List<VarConfig<BeanLectura>> activas, String tipoGrafica) {
        if (datos.isEmpty() || activas.isEmpty()) return new byte[0];
        boolean esBarra = "BARRA".equalsIgnoreCase(tipoGrafica);

        DateAxis domainAxis = new DateAxis("Fecha/Hora");
        domainAxis.setDateFormatOverride(new SimpleDateFormat("dd/MM HH:mm"));
        domainAxis.setLabelFont(new Font("SansSerif", Font.PLAIN, 10));
        domainAxis.setTickLabelFont(new Font("SansSerif", Font.PLAIN, 9));

        CombinedDomainXYPlot combined = new CombinedDomainXYPlot(domainAxis);
        combined.setGap(6.0);
        combined.setBackgroundPaint(Color.WHITE);
        combined.setDomainGridlinePaint(new Color(210, 220, 225));

        for (VarConfig<BeanLectura> vc : activas) {
            TimeSeries ts = new TimeSeries(vc.etiqueta() + " (" + vc.unidad() + ")");
            for (BeanLectura l : datos) {
                Float v = vc.getter().apply(l);
                if (v != null) ts.addOrUpdate(new Minute(toDate(l.getFechaLectura())), v.doubleValue());
            }
            if (ts.isEmpty()) continue;

            NumberAxis rangeAxis = new NumberAxis(vc.unidad());
            rangeAxis.setAutoRangeIncludesZero(false);
            rangeAxis.setLabelFont(new Font("SansSerif", Font.PLAIN, 9));
            rangeAxis.setTickLabelFont(new Font("SansSerif", Font.PLAIN, 8));

            XYPlot subplot = new XYPlot(new TimeSeriesCollection(ts), null, rangeAxis,
                    esBarra ? crearBarRenderer(vc.color()) : crearRenderer(vc.color(), datos.size()));
            estilizarSubplot(subplot, vc.color());
            combined.add(subplot, 1);
        }

        return toPng(new JFreeChart("", JFreeChart.DEFAULT_TITLE_FONT, combined, true),
                900, 190 * activas.size() + 30);
    }

    // ── Render genérico eléctrico ──────────────────────────────────────
    private byte[] renderizarElectrico(List<BeanLecturaElectrica> datos,
                                       List<VarConfig<BeanLecturaElectrica>> activas, String tipoGrafica) {
        if (datos.isEmpty() || activas.isEmpty()) return new byte[0];
        boolean esBarra = "BARRA".equalsIgnoreCase(tipoGrafica);

        DateAxis domainAxis = new DateAxis("Fecha/Hora");
        domainAxis.setDateFormatOverride(new SimpleDateFormat("dd/MM HH:mm"));
        domainAxis.setLabelFont(new Font("SansSerif", Font.PLAIN, 10));
        domainAxis.setTickLabelFont(new Font("SansSerif", Font.PLAIN, 9));

        CombinedDomainXYPlot combined = new CombinedDomainXYPlot(domainAxis);
        combined.setGap(6.0);
        combined.setBackgroundPaint(Color.WHITE);

        for (VarConfig<BeanLecturaElectrica> vc : activas) {
            TimeSeries ts = new TimeSeries(vc.etiqueta() + " (" + vc.unidad() + ")");
            for (BeanLecturaElectrica l : datos) {
                Float v = vc.getter().apply(l);
                if (v != null) ts.addOrUpdate(new Minute(toDate(l.getFechaLectura())), v.doubleValue());
            }
            if (ts.isEmpty()) continue;

            NumberAxis rangeAxis = new NumberAxis(vc.unidad());
            rangeAxis.setAutoRangeIncludesZero(false);
            rangeAxis.setLabelFont(new Font("SansSerif", Font.PLAIN, 9));
            rangeAxis.setTickLabelFont(new Font("SansSerif", Font.PLAIN, 8));

            XYPlot subplot = new XYPlot(new TimeSeriesCollection(ts), null, rangeAxis,
                    esBarra ? crearBarRenderer(vc.color()) : crearRenderer(vc.color(), datos.size()));
            estilizarSubplot(subplot, vc.color());
            combined.add(subplot, 1);
        }

        return toPng(new JFreeChart("", JFreeChart.DEFAULT_TITLE_FONT, combined, true),
                900, 190 * activas.size() + 30);
    }

    // ── Helpers ────────────────────────────────────────────────────────
    private XYLineAndShapeRenderer crearRenderer(Color color, int numPuntos) {
        boolean mostrarPuntos = numPuntos <= 60;
        XYLineAndShapeRenderer r = new XYLineAndShapeRenderer(true, mostrarPuntos);
        r.setSeriesPaint(0, color);
        r.setSeriesStroke(0, new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        if (mostrarPuntos) r.setSeriesShape(0, new Ellipse2D.Double(-2, -2, 4, 4));
        return r;
    }

    private XYBarRenderer crearBarRenderer(Color color) {
        XYBarRenderer r = new XYBarRenderer(0.15);
        r.setSeriesPaint(0, color);
        r.setShadowVisible(false);
        r.setDrawBarOutline(false);
        r.setUseYInterval(false);
        return r;
    }

    private void estilizarSubplot(XYPlot plot, Color acento) {
        plot.setBackgroundPaint(new Color(252, 254, 255));
        plot.setDomainGridlinePaint(new Color(220, 230, 235));
        plot.setRangeGridlinePaint(new Color(220, 230, 235));
        plot.setOutlinePaint(new Color(200, 215, 220));
    }

    private byte[] toPng(JFreeChart chart, int width, int height) {
        chart.setBackgroundPaint(Color.WHITE);
        chart.getLegend().setItemFont(new Font("SansSerif", Font.PLAIN, 9));
        chart.getLegend().setBackgroundPaint(new Color(250, 252, 253));
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ChartUtils.writeChartAsPNG(out, chart, width, height);
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Error generando gráfica PNG", e);
            return new byte[0];
        }
    }

    private static final ZoneId ZONA_CENIDET = ZoneId.of("America/Mexico_City");

    private Date toDate(java.time.LocalDateTime ldt) {
        return Date.from(ldt.atZone(ZONA_CENIDET).toInstant());
    }
}
