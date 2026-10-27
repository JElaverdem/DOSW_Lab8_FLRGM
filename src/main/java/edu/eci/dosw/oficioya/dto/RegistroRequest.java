package edu.eci.dosw.oficioya.dto;

import edu.eci.dosw.oficioya.entity.Rol;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegistroRequest(
        @NotBlank String nombre,
        @NotBlank @Email String correo,
        @NotBlank String telefono,
        @NotBlank @Size(min = 8) String password,
        @NotNull Rol rol,
        String oficioPrincipal,
        String zona) {
}
