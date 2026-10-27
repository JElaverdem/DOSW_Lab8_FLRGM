package edu.eci.dosw.oficioya.dto;

import edu.eci.dosw.oficioya.entity.Rol;

public record PerfilAdminResponse(
        Long id,
        String nombre,
        String correo,
        String telefono,
        Rol rol) {
}
