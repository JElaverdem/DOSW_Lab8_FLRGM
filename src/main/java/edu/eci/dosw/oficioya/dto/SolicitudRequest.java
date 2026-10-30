package edu.eci.dosw.oficioya.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SolicitudRequest(
        @NotNull Long trabajadorId,
        @NotBlank String descripcion) {
}
