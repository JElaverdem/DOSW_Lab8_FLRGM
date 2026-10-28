package edu.eci.dosw.oficioya.service;

import java.util.EnumSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import edu.eci.dosw.oficioya.dto.LoginRequest;
import edu.eci.dosw.oficioya.dto.LoginResponse;
import edu.eci.dosw.oficioya.dto.RegistroRequest;
import edu.eci.dosw.oficioya.dto.RegistroResponse;
import edu.eci.dosw.oficioya.entity.Rol;
import edu.eci.dosw.oficioya.entity.Usuario;
import edu.eci.dosw.oficioya.exception.CorreoDuplicadoException;
import edu.eci.dosw.oficioya.exception.CredencialesInvalidasException;
import edu.eci.dosw.oficioya.exception.RolNoPermitidoException;
import edu.eci.dosw.oficioya.mapper.UsuarioMapper;
import edu.eci.dosw.oficioya.repository.UsuarioRepository;
import edu.eci.dosw.oficioya.security.JwtService;

@Service 
public class AuthService {
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final UsuarioMapper mapper;
    private final JwtService jwtService;
    private final Long ttlMinutes;

    private static final Set<Rol> ROLES_PUBLICOS =
        EnumSet.of(Rol.TRABAJADOR, Rol.CONTRATANTE);

    public AuthService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder, UsuarioMapper usuarioMapper, JwtService jwtService,@Value("${app.jwt.ttl-minutes}") long ttlMinutes){
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.mapper = usuarioMapper;
        this.jwtService = jwtService;
        this.ttlMinutes = ttlMinutes;
    }

    public RegistroResponse registrar(RegistroRequest req) {
    if (!ROLES_PUBLICOS.contains(req.rol())) {
        throw new RolNoPermitidoException("Rol no permitido en el registro");   // 400
    }
    String correo = req.correo().trim().toLowerCase();
    if (usuarioRepository.existsByCorreo(correo)) {
        throw new CorreoDuplicadoException(correo);                            // 409
    }
    Usuario u = new Usuario();
    u.setNombre(req.nombre());
    u.setCorreo(correo);
    u.setPasswordHash(passwordEncoder.encode(req.password()));
    u.setRol(req.rol());                         // el cliente propone; el servidor solo acepta roles públicos
    return mapper.toResponse(usuarioRepository.save(u));
}

    public LoginResponse login(LoginRequest req) {
    Usuario u = usuarioRepository.findByCorreo(req.correo().trim().toLowerCase())
            .filter(x -> passwordEncoder.matches(req.password(), x.getPasswordHash()))
            .orElseThrow(CredencialesInvalidasException::new);   // mismo error si falla el correo o la clave
    String token = jwtService.generar(u.getCorreo(), u.getRol().name());
    return new LoginResponse("Bearer", token, ttlMinutes * 60);
}
}
