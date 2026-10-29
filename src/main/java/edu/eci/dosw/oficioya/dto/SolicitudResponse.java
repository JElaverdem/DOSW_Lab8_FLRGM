package edu.eci.dosw.oficioya.dto;

import java.time.LocalDateTime;

import edu.eci.dosw.oficioya.entity.EstadoSolicitud;

public record SolicitudResponse(
        Long id,
        Long contratanteId,
        Long trabajadorId,
        String descripcion,
        EstadoSolicitud estado,
        LocalDateTime fechaCreacion) {
}
