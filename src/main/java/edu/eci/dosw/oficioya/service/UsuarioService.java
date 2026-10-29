package edu.eci.dosw.oficioya.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.eci.dosw.oficioya.dto.PerfilAdminResponse;
import edu.eci.dosw.oficioya.dto.PerfilPublicoResponse;
import edu.eci.dosw.oficioya.dto.UsuarioMeResponse;
import edu.eci.dosw.oficioya.entity.Rol;
import edu.eci.dosw.oficioya.exception.RecursoNoEncontradoException;
import edu.eci.dosw.oficioya.mapper.UsuarioMapper;
import edu.eci.dosw.oficioya.repository.UsuarioRepository;

/**
 * Profile service: own profile, public worker profiles and admin listing.
 */
@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final UsuarioMapper mapper;

    public UsuarioService(UsuarioRepository usuarioRepository, UsuarioMapper mapper) {
        this.usuarioRepository = usuarioRepository;
        this.mapper = mapper;
    }

    /**
     * Returns the profile of the authenticated user.
     */
    @Transactional(readOnly = true)
    public UsuarioMeResponse getCurrentUser(String email) {
        return usuarioRepository.findByCorreo(normalize(email))
                .map(mapper::toMe)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
    }

    /**
     * Lists every user with the worker role using the public DTO.
     */
    @Transactional(readOnly = true)
    public List<PerfilPublicoResponse> listWorkers() {
        return usuarioRepository.findByRol(Rol.TRABAJADOR).stream()
                .map(mapper::toPublico)
                .toList();
    }

    /**
     * Returns one worker by id, or 404 when it is missing or not a worker.
     */
    @Transactional(readOnly = true)
    public PerfilPublicoResponse getWorker(Long id) {
        return usuarioRepository.findById(id)
                .filter(usuario -> usuario.getRol() == Rol.TRABAJADOR)
                .map(mapper::toPublico)
                .orElseThrow(() -> new RecursoNoEncontradoException("Trabajador no encontrado"));
    }

    /**
     * Lists every user with contact data. Only for administrators.
     */
    @Transactional(readOnly = true)
    public List<PerfilAdminResponse> listForAdmin() {
        return usuarioRepository.findAll().stream()
                .map(mapper::toAdmin)
                .toList();
    }

    private String normalize(String email) {
        return email.trim().toLowerCase();
    }
}
