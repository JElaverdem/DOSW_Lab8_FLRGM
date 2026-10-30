package edu.eci.dosw.oficioya.service;

import java.util.EnumSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

/**
 * Authentication service: registration and login.
 */
@Service
public class AuthService {

    private static final Set<Rol> PUBLIC_ROLES =
            EnumSet.of(Rol.TRABAJADOR, Rol.CONTRATANTE);

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final UsuarioMapper mapper;
    private final JwtService jwtService;
    private final long ttlMinutes;

    public AuthService(UsuarioRepository usuarioRepository,
                       PasswordEncoder passwordEncoder,
                       UsuarioMapper mapper,
                       JwtService jwtService,
                       @Value("${app.jwt.ttl-minutes}") long ttlMinutes) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.mapper = mapper;
        this.jwtService = jwtService;
        this.ttlMinutes = ttlMinutes;
    }

    /**
     * Registers a public user (worker or contractor).
     */
    @Transactional
    public RegistroResponse register(RegistroRequest request) {
        if (!PUBLIC_ROLES.contains(request.rol())) {
            throw new RolNoPermitidoException("Rol no permitido en el registro");
        }

        String email = request.correo().trim().toLowerCase();
        if (usuarioRepository.existsByCorreo(email)) {
            throw new CorreoDuplicadoException(email);
        }

        Usuario usuario = new Usuario();
        usuario.setNombre(request.nombre().trim());
        usuario.setCorreo(email);
        usuario.setTelefono(request.telefono().trim());
        usuario.setPasswordHash(passwordEncoder.encode(request.password()));
        usuario.setRol(request.rol());

        // Trade and zone only apply to workers.
        if (request.rol() == Rol.TRABAJADOR) {
            usuario.setOficioPrincipal(request.oficioPrincipal());
            usuario.setZona(request.zona());
        }

        return mapper.toResponse(usuarioRepository.save(usuario));
    }

    /**
     * Validates credentials and issues a bearer token.
     */
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        String email = request.correo().trim().toLowerCase();

        Usuario usuario = usuarioRepository.findByCorreo(email)
                .filter(u -> passwordEncoder.matches(request.password(), u.getPasswordHash()))
                .orElseThrow(CredencialesInvalidasException::new);

        String token = jwtService.generar(usuario.getCorreo(), usuario.getRol().name());
        return new LoginResponse("Bearer", token, ttlMinutes * 60);
    }
}
