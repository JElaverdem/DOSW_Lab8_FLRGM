package edu.eci.dosw.oficioya.mapper;

import edu.eci.dosw.oficioya.entity.Usuario;
import edu.eci.dosw.oficioya.dto.RegistroResponse;
import org.springframework.stereotype.Component;

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
}