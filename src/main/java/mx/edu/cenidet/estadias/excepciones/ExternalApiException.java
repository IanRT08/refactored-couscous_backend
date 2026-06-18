package mx.edu.cenidet.estadias.excepciones;

import lombok.Getter;

@Getter
public class ExternalApiException extends RuntimeException {

    private final String fuente;

    public ExternalApiException(String fuente, String message) {
        super(message);
        this.fuente = fuente;
    }

    public ExternalApiException(String fuente, String message, Throwable cause) {
        super(message, cause);
        this.fuente = fuente;
    }

}
