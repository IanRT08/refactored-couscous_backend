package mx.edu.cenidet.estadias.excepciones;

public class DuplicateReadingException extends RuntimeException {

    public DuplicateReadingException(String message) {
        super(message);
    }

    public DuplicateReadingException(String message, Throwable cause) {
        super(message, cause);
    }

}
