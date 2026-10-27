package edu.eci.dosw.oficioya.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(@NotBlank String correo, @NotBlank String password) {
}
