package com.example.hospital.shared.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ErrorResponse> manejarRecursoNoEncontrado(
            RecursoNoEncontradoException ex, HttpServletRequest request) {
        log.debug("404 {} {}: {}", request.getMethod(), request.getRequestURI(), ex.getMessage());
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.NOT_FOUND.value())
                .error(HttpStatus.NOT_FOUND.getReasonPhrase())
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .build();
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(ReglaNegocioException.class)
    public ResponseEntity<ErrorResponse> manejarReglaNegocio(
            ReglaNegocioException ex, HttpServletRequest request) {
        log.warn("422 regla de negocio en {} {}: {}", request.getMethod(), request.getRequestURI(), ex.getMessage());
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.UNPROCESSABLE_CONTENT.value())
                .error(HttpStatus.UNPROCESSABLE_CONTENT.getReasonPhrase())
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .build();
        return new ResponseEntity<>(error, HttpStatus.UNPROCESSABLE_CONTENT);
    }

    @ExceptionHandler(DuplicadoException.class)
    public ResponseEntity<ErrorResponse> manejarDuplicado(
            DuplicadoException ex, HttpServletRequest request) {
        log.warn("409 conflicto en {} {}: {}", request.getMethod(), request.getRequestURI(), ex.getMessage());
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.CONFLICT.value())
                .error(HttpStatus.CONFLICT.getReasonPhrase())
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .build();
        return new ResponseEntity<>(error, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> manejarIllegalArgument(
            IllegalArgumentException ex, HttpServletRequest request) {
        log.debug("400 argumento inválido en {} {}: {}", request.getMethod(), request.getRequestURI(), ex.getMessage());
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .build();
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> manejarIllegalState(
            IllegalStateException ex, HttpServletRequest request) {
        log.warn("409 estado inválido en {} {}: {}", request.getMethod(), request.getRequestURI(), ex.getMessage());
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.CONFLICT.value())
                .error(HttpStatus.CONFLICT.getReasonPhrase())
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .build();
        return new ResponseEntity<>(error, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> manejarValidacion(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        log.debug("400 validación en {} {}: {} campo(s) con error", request.getMethod(), request.getRequestURI(), ex.getBindingResult().getFieldErrorCount());
        Map<String, String> detalles = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(err ->
                detalles.put(err.getField(), err.getDefaultMessage())
        );

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Validation Failed")
                .message("Error de validación en los campos enviados")
                .path(request.getRequestURI())
                .detalles(detalles)
                .build();
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    // @PreAuthorize lanza AccessDeniedException dentro del controller; sin este handler
    // lo capturaba Exception.class y se devolvía 500 en lugar de 403.
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> manejarAccesoDenegado(
            AccessDeniedException ex, HttpServletRequest request) {
        log.warn("403 acceso denegado a {} {} para el usuario {}", request.getMethod(), request.getRequestURI(), usuarioActual(request));
        return construir(HttpStatus.FORBIDDEN, "No tiene permisos para realizar esta acción", request, null);
    }

    // Login fallido (BadCredentialsException, DisabledException...) u otra falla de autenticación.
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> manejarAutenticacion(
            AuthenticationException ex, HttpServletRequest request) {
        String mensaje;
        if (ex instanceof BadCredentialsException) {
            mensaje = "Usuario o contraseña incorrectos";
        } else if (ex instanceof DisabledException) {
            mensaje = "El usuario se encuentra inactivo";
        } else {
            mensaje = "Token ausente, inválido o expirado";
        }
        return construir(HttpStatus.UNAUTHORIZED, mensaje, request, null);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> manejarCuerpoIlegible(
            HttpMessageNotReadableException ex, HttpServletRequest request) {
        log.debug("400 cuerpo ilegible en {} {}: {}", request.getMethod(), request.getRequestURI(), ex.getMostSpecificCause().getMessage());
        return construir(HttpStatus.BAD_REQUEST,
                "El cuerpo de la petición es inválido o tiene un formato incorrecto (JSON mal formado, fecha o valor no válido)",
                request, null);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> manejarTipoArgumento(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        Map<String, String> detalles = new HashMap<>();
        detalles.put(ex.getName(), "Valor inválido: '" + ex.getValue() + "'");
        return construir(HttpStatus.BAD_REQUEST, "Parámetro con formato inválido: " + ex.getName(), request, detalles);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> manejarParametroFaltante(
            MissingServletRequestParameterException ex, HttpServletRequest request) {
        Map<String, String> detalles = new HashMap<>();
        detalles.put(ex.getParameterName(), "Parámetro obligatorio");
        return construir(HttpStatus.BAD_REQUEST, "Falta el parámetro obligatorio: " + ex.getParameterName(), request, detalles);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> manejarRutaInexistente(
            NoResourceFoundException ex, HttpServletRequest request) {
        return construir(HttpStatus.NOT_FOUND, "El recurso solicitado no existe", request, null);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> manejarMetodoNoPermitido(
            HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        return construir(HttpStatus.METHOD_NOT_ALLOWED, "Método HTTP no permitido para esta ruta: " + ex.getMethod(), request, null);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> manejarTipoMedioNoSoportado(
            HttpMediaTypeNotSupportedException ex, HttpServletRequest request) {
        return construir(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Tipo de contenido no soportado; use application/json", request, null);
    }

    private ResponseEntity<ErrorResponse> construir(HttpStatus status, String mensaje,
            HttpServletRequest request, Map<String, String> detalles) {
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(mensaje)
                .path(request.getRequestURI())
                .detalles(detalles)
                .build();
        return new ResponseEntity<>(error, status);
    }

    // La base rechazó la operación (UNIQUE, FOREIGN KEY o CHECK). Normalmente las reglas del servicio lo evitan antes;
    // llega aquí en carreras (dos peticiones simultáneas) o si una regla falta en el servicio. Se responde 409 sin
    // exponer el SQL; el detalle (nombre de la restricción) queda en el log.
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> manejarIntegridadDatos(
            DataIntegrityViolationException ex, HttpServletRequest request) {
        String causa = ex.getMostSpecificCause().getMessage();
        log.warn("409 restricción de la base de datos en {} {} (usuario {}): {}",
                request.getMethod(), request.getRequestURI(), usuarioActual(request), causa);
        return construir(HttpStatus.CONFLICT, mensajeIntegridad(causa), request, null);
    }

    /** Mensaje para el usuario según el tipo de restricción violada (códigos SQLSTATE de PostgreSQL en el texto). */
    private static String mensajeIntegridad(String causa) {
        String texto = causa == null ? "" : causa.toLowerCase();
        if (texto.contains("duplicate key") || texto.contains("llave duplicada") || texto.contains("unique")
                || texto.contains("unicidad")) {
            return "Ya existe un registro con esos datos.";
        }
        if (texto.contains("foreign key") || texto.contains("llave foránea")) {
            return "La operación hace referencia a un registro que no existe o que todavía está en uso.";
        }
        if (texto.contains("check")) {
            return "Los datos no cumplen una regla de la base de datos (por ejemplo, un monto o saldo negativo).";
        }
        return "La operación entra en conflicto con los datos existentes.";
    }

    // Error no previsto: se registra completo (con la traza) y al cliente se le devuelve un mensaje genérico,
    // sin detalles internos (SQL, nombres de clases...).
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> manejarExcepcionGeneral(
            Exception ex, HttpServletRequest request) {
        log.error("500 error no controlado en {} {} (usuario {})",
                request.getMethod(), request.getRequestURI(), usuarioActual(request), ex);
        return construir(HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocurrió un error interno en el servidor. Si persiste, contacte al administrador.", request, null);
    }

    private static String usuarioActual(HttpServletRequest request) {
        return request.getUserPrincipal() != null ? request.getUserPrincipal().getName() : "anónimo";
    }
}
