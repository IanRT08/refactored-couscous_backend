package mx.edu.cenidet.estadias.dtos.estadisticas;

import lombok.*;
import mx.edu.cenidet.estadias.modelos.lecturaElectrica.FuenteElectrica;

import java.time.LocalDateTime;

@Getter
@Builder
public class ElectricStatisticsDTO {

    private LocalDateTime inicio;
    private LocalDateTime fin;
    private FuenteElectrica fuente;

    //Voltaje (Fotovoltaico: Vload | Eólico: Voltaje)
    private Double promedioVoltaje;
    private Double maxVoltaje;
    private Double minVoltaje;
    private Double modaVoltaje;

    //Corriente (Fotovoltaico: iload | Eólico: Corriente)
    private Double promedioCorriente;
    private Double maxCorriente;
    private Double minCorriente;
    private Double modaCorriente;

    //Potencia (Fotovoltaico: Power | Eólico: Potencia)
    private Double promedioPotencia;
    private Double maxPotencia;
    private Double minPotencia;
    private Double modaPotencia;

    //Voc — solo Fotovoltaico (voltaje de circuito abierto); null en Eólico
    private Double promedioVoc;
    private Double maxVoc;
    private Double minVoc;
    private Double modaVoc;

    //Energía — no la reporta ThingSpeak, se calcula integrando potencia x tiempo
    private Double promedioEnergia;
    private Double maxEnergia;
    private Double minEnergia;
    private Double modaEnergia;
    //Suma de toda la energía generada en el periodo (la métrica que sí es
    //comparable/sumable entre Fotovoltaico y Eólico para la vista combinada)
    private Double energiaTotalPeriodo;

}
