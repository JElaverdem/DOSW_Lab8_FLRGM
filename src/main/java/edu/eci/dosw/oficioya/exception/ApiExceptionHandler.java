package edu.eci.dosw.oficioya.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
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

    @ExceptionHandler(HttpMessageNotReadableException.class)   // p. ej. "rol": "JEFE"
    ResponseEntity<ApiError> cuerpoIlegible(HttpMessageNotReadableException ex, HttpServletRequest req) {
        return ResponseEntity.badRequest().body(
                ApiError.of(400, "Bad Request", "Cuerpo de la petición inválido", req.getRequestURI()));
    }

    @ExceptionHandler(CredencialesInvalidasException.class)
    ResponseEntity<ApiError> cuerpoIlegible(CredencialesInvalidasException ex, HttpServletRequest req) {
        return ResponseEntity.badRequest().body(
                ApiError.of(401, "Unauthorized", "Datos de sesión inválidos.", req.getRequestURI()));
    }
}