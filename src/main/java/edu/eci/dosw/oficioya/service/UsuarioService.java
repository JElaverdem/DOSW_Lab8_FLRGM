package edu.eci.dosw.oficioya.service;

import edu.eci.dosw.oficioya.dto.PerfilAdminResponse;
import edu.eci.dosw.oficioya.dto.PerfilPublicoResponse;
import edu.eci.dosw.oficioya.dto.UsuarioMeResponse;

import java.util.List;

public interface UsuarioService {

    UsuarioMeResponse me(String correo);

    List<PerfilPublicoResponse> listarTrabajadores();

    PerfilPublicoResponse obtenerTrabajador(Long id);

    List<PerfilAdminResponse> listarParaAdmin();
}
