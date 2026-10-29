package edu.eci.dosw.oficioya.service;

import java.util.List;
import java.util.Optional;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.eci.dosw.oficioya.dto.SolicitudRequest;
import edu.eci.dosw.oficioya.dto.SolicitudResponse;
import edu.eci.dosw.oficioya.entity.EstadoSolicitud;
import edu.eci.dosw.oficioya.entity.Rol;
import edu.eci.dosw.oficioya.entity.Solicitud;
import edu.eci.dosw.oficioya.entity.Usuario;
import edu.eci.dosw.oficioya.exception.EstadoInvalidoException;
import edu.eci.dosw.oficioya.exception.RecursoNoEncontradoException;
import edu.eci.dosw.oficioya.repository.SolicitudRepository;
import edu.eci.dosw.oficioya.repository.UsuarioRepository;

/**
 * Implementación base de solicitudes. LAB-41/LAB-42 de Juan Esteban debe
 * revisarla y refinarla; aquí se deja funcional para que el proyecto compile
 * y el contexto de Spring pueda arrancar.
 */
@Service
public class SolicitudServiceImpl implements SolicitudService {

    private final SolicitudRepository solicitudRepository;
    private final UsuarioRepository usuarioRepository;

    public SolicitudServiceImpl(SolicitudRepository solicitudRepository,
                                UsuarioRepository usuarioRepository) {
        this.solicitudRepository = solicitudRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    @Transactional
    public SolicitudResponse crear(String correo, SolicitudRequest request) {
        Usuario contratante = buscarUsuario(correo);
        if (contratante.getRol() != Rol.CONTRATANTE) {
            throw new AccessDeniedException("Solo un contratante puede crear solicitudes");
        }

        Usuario trabajador = usuarioRepository.findById(request.trabajadorId())
                .filter(usuario -> usuario.getRol() == Rol.TRABAJADOR)
                .orElseThrow(() -> new RecursoNoEncontradoException("Trabajador no encontrado"));

        Solicitud solicitud = new Solicitud();
        solicitud.setContratante(contratante);
        solicitud.setTrabajador(trabajador);
        solicitud.setDescripcion(request.descripcion());
        solicitud.setEstado(EstadoSolicitud.PENDIENTE);

        return toResponse(solicitudRepository.save(solicitud));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SolicitudResponse> listar(String correo) {
        Usuario usuario = buscarUsuario(correo);

        List<Solicitud> solicitudes;
        if (usuario.getRol() == Rol.CONTRATANTE) {
            solicitudes = solicitudRepository.findByContratanteId(usuario.getId());
        } else if (usuario.getRol() == Rol.TRABAJADOR) {
            solicitudes = solicitudRepository.findByTrabajadorId(usuario.getId());
        } else {
            throw new AccessDeniedException("Rol no autorizado para consultar solicitudes");
        }

        return solicitudes.stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SolicitudResponse obtener(String correo, Long id) {
        Usuario usuario = buscarUsuario(correo);
        return buscarPropia(usuario, id)
                .map(this::toResponse)
                .orElseThrow(() -> new RecursoNoEncontradoException("Solicitud no encontrada"));
    }

    @Override
    @Transactional
    public SolicitudResponse aceptar(String correo, Long id) {
        Usuario trabajador = buscarUsuario(correo);
        if (trabajador.getRol() != Rol.TRABAJADOR) {
            throw new AccessDeniedException("Solo el trabajador asignado puede aceptar");
        }

        Solicitud solicitud = solicitudRepository.findByIdAndTrabajadorId(id, trabajador.getId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Solicitud no encontrada"));

        if (solicitud.getEstado() != EstadoSolicitud.PENDIENTE) {
            throw new EstadoInvalidoException("La solicitud no está pendiente");
        }

        solicitud.setEstado(EstadoSolicitud.ACEPTADA);
        return toResponse(solicitudRepository.save(solicitud));
    }

    @Override
    @Transactional
    public SolicitudResponse rechazar(String correo, Long id) {
        Usuario trabajador = buscarUsuario(correo);
        if (trabajador.getRol() != Rol.TRABAJADOR) {
            throw new AccessDeniedException("Solo el trabajador asignado puede rechazar");
        }

        Solicitud solicitud = solicitudRepository.findByIdAndTrabajadorId(id, trabajador.getId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Solicitud no encontrada"));

        if (solicitud.getEstado() != EstadoSolicitud.PENDIENTE) {
            throw new EstadoInvalidoException("La solicitud no está pendiente");
        }

        solicitud.setEstado(EstadoSolicitud.RECHAZADA);
        return toResponse(solicitudRepository.save(solicitud));
    }

    @Override
    @Transactional
    public SolicitudResponse cancelar(String correo, Long id) {
        Usuario contratante = buscarUsuario(correo);
        if (contratante.getRol() != Rol.CONTRATANTE) {
            throw new AccessDeniedException("Solo el contratante puede cancelar");
        }

        Solicitud solicitud = solicitudRepository.findByIdAndContratanteId(id, contratante.getId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Solicitud no encontrada"));

        if (solicitud.getEstado() != EstadoSolicitud.PENDIENTE
                && solicitud.getEstado() != EstadoSolicitud.ACEPTADA) {
            throw new EstadoInvalidoException("La solicitud no se puede cancelar en su estado actual");
        }

        solicitud.setEstado(EstadoSolicitud.CANCELADA);
        return toResponse(solicitudRepository.save(solicitud));
    }

    private Usuario buscarUsuario(String correo) {
        return usuarioRepository.findByCorreo(correo.trim().toLowerCase())
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
    }

    private Optional<Solicitud> buscarPropia(Usuario usuario, Long id) {
        if (usuario.getRol() == Rol.CONTRATANTE) {
            return solicitudRepository.findByIdAndContratanteId(id, usuario.getId());
        }
        if (usuario.getRol() == Rol.TRABAJADOR) {
            return solicitudRepository.findByIdAndTrabajadorId(id, usuario.getId());
        }
        throw new AccessDeniedException("Rol no autorizado para consultar solicitudes");
    }

    private SolicitudResponse toResponse(Solicitud solicitud) {
        return new SolicitudResponse(
                solicitud.getId(),
                solicitud.getContratante().getId(),
                solicitud.getTrabajador().getId(),
                solicitud.getDescripcion(),
                solicitud.getEstado(),
                solicitud.getFechaCreacion()
        );
    }
}
