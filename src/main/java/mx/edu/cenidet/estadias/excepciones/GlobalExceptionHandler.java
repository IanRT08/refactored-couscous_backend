package mx.edu.cenidet.estadias.excepciones;

import lombok.extern.slf4j.Slf4j;
import mx.edu.cenidet.estadias.dtos.comunes.ApiResponseDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    //Validacion de campos: Error 400
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponseDTO<?>> handleValidation(
            MethodArgumentNotValidException ex) {

        String mensajes = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(" | "));

        log.debug("Validación fallida: {}", mensajes);
        return ResponseEntity
                .badRequest()
                .body(ApiResponseDTO.error("Datos inválidos: " + mensajes));
    }

    //Reglas de negocio violadas: Error 400
    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ApiResponseDTO<?>> handleBusinessRule(
            BusinessRuleException ex) {

        log.debug("Error, validacion violada: {}", ex.getMessage());
        return ResponseEntity
                .badRequest()
                .body(ApiResponseDTO.error(ex.getMessage()));
    }

    //Credenciales invalidas: Error 401
    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiResponseDTO<?>> handleInvalidCredentials(
            InvalidCredentialsException ex) {

        log.debug("Credenciales inválidas: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponseDTO.error(ex.getMessage()));
    }

    //Error 404
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponseDTO<?>> handleNotFound(
            ResourceNotFoundException ex) {

        log.debug("Recurso no encontrado: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponseDTO.error(ex.getMessage()));
    }

    //Lectura duplicada
    @ExceptionHandler(DuplicateReadingException.class)
    public ResponseEntity<ApiResponseDTO<?>> handleDuplicate(
            DuplicateReadingException ex) {

        log.warn("Lectura duplicada ignorada: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponseDTO.error(ex.getMessage()));
    }

    //Error al generar reporte: Error 500
    @ExceptionHandler(ReportGenerationException.class)
    public ResponseEntity<ApiResponseDTO<?>> handleReport(
            ReportGenerationException ex) {

        log.error("Error generando reporte [{}]: {}", ex.getMessage(),
                ex.getCause() != null ? ex.getCause().getMessage() : "sin causa");
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponseDTO.error(
                        "Error al generar el reporte. Intenta de nuevo o contacta al administrador."));
    }

    //API externa no disponible: Error 503
    @ExceptionHandler(ExternalApiException.class)
    public ResponseEntity<ApiResponseDTO<?>> handleExternalApi(
            ExternalApiException ex) {

        log.error("Error de API externa [{}]: {}", ex.getFuente(), ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponseDTO.error(
                        "Servicio externo no disponible. "
                                + "Los datos mostrados pueden no ser los más recientes."));
    }

    //Error generico
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponseDTO<?>> handleGeneral(Exception ex) {
        log.error("Error no controlado [{}]: {}", ex.getClass().getSimpleName(),
                ex.getMessage(), ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponseDTO.error(
                        "Error interno del servidor. Por favor intenta más tarde."));
    }


}
