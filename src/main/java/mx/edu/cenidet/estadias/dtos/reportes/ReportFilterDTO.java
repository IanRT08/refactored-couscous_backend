package mx.edu.cenidet.estadias.dtos.reportes;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import mx.edu.cenidet.estadias.modelos.lecturaElectrica.FuenteElectrica;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReportFilterDTO {

    @NotNull(message = "La fecha de inicio es obligatoria")
    private LocalDateTime inicio;

    @NotNull(message = "La fecha de fin es obligatoria")
    private LocalDateTime fin;

    @NotNull(message = "El tipo de reporte es obligatorio")
    private TipoReporte tipo;

    @NotNull(message = "El formato es obligatorio")
    private FormatoReporte formato;

    // Obligatorio solo cuando tipo = ELECTRICO
    private FuenteElectrica fuente;

    // Variables a incluir. null/vacío = todas.
    // CLIMATICO: TEMPERATURA, VIENTO, HUMEDAD, RADIACION, PRESION
    // ELECTRICO: VOLTAJE, CORRIENTE, POTENCIA, VOC, ENERGIA
    private List<String> variables;

    // Filas de estadísticas a incluir. null/vacío = PROMEDIO, MAXIMO, MINIMO.
    // Valores válidos: PROMEDIO, MAXIMO, MINIMO, MODA
    private List<String> estadisticas;

    // Tipo de gráfica para PDF: LINEA (default) o BARRA
    private String tipoGrafica = "LINEA";

    // Constructor de compatibilidad (sin los nuevos campos opcionales)
    public ReportFilterDTO(LocalDateTime inicio, LocalDateTime fin,
                           TipoReporte tipo, FormatoReporte formato,
                           FuenteElectrica fuente) {
        this.inicio = inicio;
        this.fin = fin;
        this.tipo = tipo;
        this.formato = formato;
        this.fuente = fuente;
    }

    public boolean incluirVariable(String nombre) {
        return variables == null || variables.isEmpty() || variables.contains(nombre);
    }

    public boolean incluirEstadistica(String nombre) {
        if (estadisticas == null || estadisticas.isEmpty()) {
            return !nombre.equals("MODA"); // default: PROMEDIO, MAXIMO, MINIMO
        }
        return estadisticas.contains(nombre);
    }

    @AssertTrue(message = "La fecha de inicio debe ser anterior a la fecha fin")
    @JsonIgnore
    public boolean isRangoValido() {
        return inicio != null && fin != null && inicio.isBefore(fin);
    }
}
