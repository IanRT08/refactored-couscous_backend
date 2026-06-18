package mx.edu.cenidet.estadias.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class ConexionRecuperadaEvent extends ApplicationEvent {

    //AMBIENT_WEATHER o THINGSPEAK
    private final String fuente;

    public ConexionRecuperadaEvent(Object source, String fuente) {
        super(source);
        this.fuente = fuente;
    }
}