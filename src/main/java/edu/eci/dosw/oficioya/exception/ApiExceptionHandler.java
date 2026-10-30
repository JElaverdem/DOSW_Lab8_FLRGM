package edu.eci.dosw.oficioya.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import edu.eci.dosw.oficioya.dto.ApiError;
import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(CorreoDuplicadoException.class)
    ResponseEntity<ApiError> duplicado(CorreoDuplicadoException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                ApiError.of(409, "Conflict", "El correo ya está registrado", req.getRequestURI()));
    }

    @ExceptionHandler(RolNoPermitidoException.class)
    ResponseEntity<ApiError> rolNoPermitido(RolNoPermitidoException ex, HttpServletRequest req) {
        return ResponseEntity.badRequest().body(
                ApiError.of(400, "Bad Request", ex.getMessage(), req.getRequestURI()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> datosInvalidos(MethodArgumentNotValidException ex, HttpServletRequest req) {
        return ResponseEntity.badRequest().body(
                ApiError.of(400, "Bad Request", "Datos inválidos", req.getRequestURI()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiError> cuerpoIlegible(HttpMessageNotReadableException ex, HttpServletRequest req) {
        return ResponseEntity.badRequest().body(
                ApiError.of(400, "Bad Request", "Cuerpo de la petición inválido", req.getRequestURI()));
    }

    @ExceptionHandler(CredencialesInvalidasException.class)
    ResponseEntity<ApiError> credencialesInvalidas(CredencialesInvalidasException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                ApiError.of(401, "Unauthorized", "Credenciales inválidas", req.getRequestURI()));
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    ResponseEntity<ApiError> noEncontrado(RecursoNoEncontradoException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ApiError.of(404, "Not Found", ex.getMessage(), req.getRequestURI()));
    }

    @ExceptionHandler(EstadoInvalidoException.class)
    ResponseEntity<ApiError> estadoInvalido(EstadoInvalidoException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                ApiError.of(409, "Conflict", ex.getMessage(), req.getRequestURI()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ApiError> accesoDenegado(AccessDeniedException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                ApiError.of(403, "Forbidden", "No tiene permisos para realizar esta acción", req.getRequestURI()));
    }
}
