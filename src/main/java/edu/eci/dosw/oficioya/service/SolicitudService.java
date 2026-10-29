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
 * Base request service. LAB-41/LAB-42 (Juan Esteban) must review and refine it;
 * it is left functional so the project compiles and the Spring context starts.
 */
@Service
public class SolicitudService {

    private final SolicitudRepository solicitudRepository;
    private final UsuarioRepository usuarioRepository;

    public SolicitudService(SolicitudRepository solicitudRepository,
                            UsuarioRepository usuarioRepository) {
        this.solicitudRepository = solicitudRepository;
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * Creates a request. The contractor always comes from the token.
     */
    @Transactional
    public SolicitudResponse create(String email, SolicitudRequest request) {
        Usuario contratante = findUser(email);
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

    /**
     * Lists the requests where the authenticated user participates.
     */
    @Transactional(readOnly = true)
    public List<SolicitudResponse> list(String email) {
        Usuario usuario = findUser(email);

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

    /**
     * Returns one request only when the authenticated user participates in it.
     */
    @Transactional(readOnly = true)
    public SolicitudResponse getById(String email, Long id) {
        Usuario usuario = findUser(email);
        return findOwn(usuario, id)
                .map(this::toResponse)
                .orElseThrow(() -> new RecursoNoEncontradoException("Solicitud no encontrada"));
    }

    /**
     * Accepts a pending request. Only the assigned worker can do it.
     */
    @Transactional
    public SolicitudResponse accept(String email, Long id) {
        Usuario trabajador = findUser(email);
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

    /**
     * Rejects a pending request. Only the assigned worker can do it.
     */
    @Transactional
    public SolicitudResponse reject(String email, Long id) {
        Usuario trabajador = findUser(email);
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

    /**
     * Cancels a pending or accepted request. Only the author can do it.
     */
    @Transactional
    public SolicitudResponse cancel(String email, Long id) {
        Usuario contratante = findUser(email);
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

    private Usuario findUser(String email) {
        return usuarioRepository.findByCorreo(email.trim().toLowerCase())
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
    }

    private Optional<Solicitud> findOwn(Usuario usuario, Long id) {
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
