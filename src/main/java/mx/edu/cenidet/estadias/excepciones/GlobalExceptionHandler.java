package mx.edu.cenidet.estadias.excepciones;

import lombok.extern.slf4j.Slf4j;
import mx.edu.cenidet.estadias.dtos.comunes.ApiResponseDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

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

    //Parámetro de ruta/query con tipo incorrecto (ej. texto donde se espera un Long): Error 400
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponseDTO<?>> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex) {

        log.debug("Parámetro con tipo inválido: {}", ex.getMessage());
        return ResponseEntity
                .badRequest()
                .body(ApiResponseDTO.error(
                        "El parámetro '" + ex.getName() + "' tiene un formato inválido."));
    }

    //Falta un @RequestParam obligatorio: Error 400
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponseDTO<?>> handleMissingParam(
            MissingServletRequestParameterException ex) {

        log.debug("Parámetro obligatorio faltante: {}", ex.getMessage());
        return ResponseEntity
                .badRequest()
                .body(ApiResponseDTO.error(
                        "Falta el parámetro obligatorio '" + ex.getParameterName() + "'."));
    }

    //Body de la petición ilegible (JSON mal formado, vacío, etc.): Error 400
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponseDTO<?>> handleNotReadable(
            HttpMessageNotReadableException ex) {

        log.debug("Body de la solicitud no legible: {}", ex.getMessage());
        return ResponseEntity
                .badRequest()
                .body(ApiResponseDTO.error(
                        "El cuerpo de la solicitud no es válido o está mal formado."));
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
