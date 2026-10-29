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

@Service
public class UsuarioServiceImplementation implements UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final UsuarioMapper mapper;

    public UsuarioServiceImplementation(UsuarioRepository usuarioRepository, UsuarioMapper mapper) {
        this.usuarioRepository = usuarioRepository;
        this.mapper = mapper;
    }

    @Override
    @Transactional(readOnly = true)
    public UsuarioMeResponse me(String correo) {
        return usuarioRepository.findByCorreo(normalizar(correo))
                .map(mapper::toMe)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PerfilPublicoResponse> listarTrabajadores() {
        return usuarioRepository.findByRol(Rol.TRABAJADOR).stream()
                .map(mapper::toPublico)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PerfilPublicoResponse obtenerTrabajador(Long id) {
        return usuarioRepository.findById(id)
                .filter(usuario -> usuario.getRol() == Rol.TRABAJADOR)
                .map(mapper::toPublico)
                .orElseThrow(() -> new RecursoNoEncontradoException("Trabajador no encontrado"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PerfilAdminResponse> listarParaAdmin() {
        return usuarioRepository.findAll().stream()
                .map(mapper::toAdmin)
                .toList();
    }

    private String normalizar(String correo) {
        return correo.trim().toLowerCase();
    }
}
