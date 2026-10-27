package edu.eci.dosw.oficioya.exception;

public class CorreoDuplicadoException extends RuntimeException {
    public CorreoDuplicadoException(String correo) {
        super("El correo ya está registrado");
    }
}