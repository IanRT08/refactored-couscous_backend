package mx.edu.cenidet.estadias.util;

import mx.edu.cenidet.estadias.modelos.lecturaElectrica.BeanLecturaElectrica;

import java.time.Duration;

//Ni el canal Fotovoltaico ni el Eólico de ThingSpeak reportan energía: se
//estima integrando la potencia en el tiempo entre lecturas consecutivas de
//la MISMA fuente (regla del trapecio), expresada en Wh.
public final class EnergiaCalculator {

    private EnergiaCalculator() {
    }

    public static Float calcularIncremento(BeanLecturaElectrica anterior, BeanLecturaElectrica actual) {
        if (anterior == null || anterior.getPotencia() == null || actual.getPotencia() == null) {
            return null;
        }
        double horas = Duration.between(anterior.getFechaLectura(), actual.getFechaLectura()).toSeconds() / 3600.0;
        if (horas <= 0) {
            return null;
        }
        double potenciaPromedio = (anterior.getPotencia() + actual.getPotencia()) / 2.0;
        return (float) (potenciaPromedio * horas);
    }
}
