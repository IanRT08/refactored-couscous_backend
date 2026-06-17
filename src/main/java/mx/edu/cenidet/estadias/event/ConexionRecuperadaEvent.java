package mx.edu.cenidet.estadias.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

// Publicado por SyncHealthService cuando un origen recupera
// la conectividad tras ≥3 fallos consecutivos.
// GapRecoveryService escucha este evento para iniciar la
// recuperación de datos perdidos durante la caída.
@Getter
public class ConexionRecuperadaEvent extends ApplicationEvent {

    // "AMBIENT_WEATHER" o "THINGSPEAK"
    private final String fuente;

    public ConexionRecuperadaEvent(Object source, String fuente) {
        super(source);
        this.fuente = fuente;
    }
}