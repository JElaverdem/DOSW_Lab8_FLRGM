package edu.eci.dosw.oficioya.service;

import java.util.List;

import edu.eci.dosw.oficioya.dto.SolicitudRequest;
import edu.eci.dosw.oficioya.dto.SolicitudResponse;

/**
 * Contrato de solicitudes. La identidad siempre llega como correo autenticado.
 */
public interface SolicitudService {

    SolicitudResponse crear(String correo, SolicitudRequest request);

    List<SolicitudResponse> listar(String correo);

    SolicitudResponse obtener(String correo, Long id);

    SolicitudResponse aceptar(String correo, Long id);

    SolicitudResponse rechazar(String correo, Long id);

    SolicitudResponse cancelar(String correo, Long id);
}
