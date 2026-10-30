package edu.eci.dosw.oficioya.mapper;

import org.springframework.stereotype.Component;

import edu.eci.dosw.oficioya.dto.PerfilAdminResponse;
import edu.eci.dosw.oficioya.dto.PerfilPublicoResponse;
import edu.eci.dosw.oficioya.dto.RegistroResponse;
import edu.eci.dosw.oficioya.dto.UsuarioMeResponse;
import edu.eci.dosw.oficioya.entity.Usuario;

@Component
public class UsuarioMapper {

    public RegistroResponse toResponse(Usuario usuario) {
        return new RegistroResponse(
                usuario.getId(),
                usuario.getNombre(),
                usuario.getCorreo(),
                usuario.getRol()
        );
    }

    public UsuarioMeResponse toMe(Usuario usuario) {
        return new UsuarioMeResponse(
                usuario.getId(),
                usuario.getNombre(),
                usuario.getCorreo(),
                usuario.getTelefono(),
                usuario.getRol(),
                usuario.getOficioPrincipal(),
                usuario.getZona()
        );
    }

    public PerfilPublicoResponse toPublico(Usuario usuario) {
        return new PerfilPublicoResponse(
                usuario.getId(),
                usuario.getNombre(),
                usuario.getOficioPrincipal(),
                usuario.getZona()
        );
    }

    public PerfilAdminResponse toAdmin(Usuario usuario) {
        return new PerfilAdminResponse(
                usuario.getId(),
                usuario.getNombre(),
                usuario.getCorreo(),
                usuario.getTelefono(),
                usuario.getRol()
        );
    }
}
