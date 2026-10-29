package edu.eci.dosw.oficioya.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.eci.dosw.oficioya.dto.SolicitudRequest;
import edu.eci.dosw.oficioya.dto.SolicitudResponse;
import edu.eci.dosw.oficioya.service.SolicitudService;
import jakarta.validation.Valid;

/**
 * Skeleton of the six request endpoints (LAB-31).
 *
 * Contract rules:
 *  - Identity always comes from Authentication.getName() (token email),
 *    never from the body or the URL.
 *  - ADMIN has no access to any /solicitudes endpoint (RS-08).
 *  - Ownership (foreign request -> 404) is resolved by SolicitudService,
 *    not by the controller.
 *  - No try/catch: exceptions reach the @RestControllerAdvice.
 *  - Always DTOs, never JPA entities.
 */
@RestController
@RequestMapping("/solicitudes")
public class SolicitudController {

    private final SolicitudService solicitudService;

    public SolicitudController(SolicitudService solicitudService) {
        this.solicitudService = solicitudService;
    }

    // 1. Create: contractor only. The contractor comes from the token. -> 201
    @PostMapping
    @PreAuthorize("hasRole('CONTRATANTE')")
    public ResponseEntity<SolicitudResponse> create(
            @Valid @RequestBody SolicitudRequest request,
            Authentication auth) {
        SolicitudResponse response = solicitudService.create(auth.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // 2. List own requests: CONTRATANTE or TRABAJADOR (ADMIN -> 403). -> 200
    @GetMapping
    @PreAuthorize("hasAnyRole('CONTRATANTE', 'TRABAJADOR')")
    public ResponseEntity<List<SolicitudResponse>> list(Authentication auth) {
        return ResponseEntity.ok(solicitudService.list(auth.getName()));
    }

    // 3. Get one: only participants; foreign or missing -> 404 (decided by the service). -> 200
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CONTRATANTE', 'TRABAJADOR')")
    public ResponseEntity<SolicitudResponse> getById(
            @PathVariable Long id,
            Authentication auth) {
        return ResponseEntity.ok(solicitudService.getById(auth.getName(), id));
    }

    // 4. Accept: worker only (assigned; not assigned -> 404, contractor -> 403). -> 200
    @PatchMapping("/{id}/aceptar")
    @PreAuthorize("hasRole('TRABAJADOR')")
    public ResponseEntity<SolicitudResponse> accept(
            @PathVariable Long id,
            Authentication auth) {
        return ResponseEntity.ok(solicitudService.accept(auth.getName(), id));
    }

    // 5. Reject: assigned worker only, only from PENDIENTE (other state -> 409). -> 200
    @PatchMapping("/{id}/rechazar")
    @PreAuthorize("hasRole('TRABAJADOR')")
    public ResponseEntity<SolicitudResponse> reject(
            @PathVariable Long id,
            Authentication auth) {
        return ResponseEntity.ok(solicitudService.reject(auth.getName(), id));
    }

    // 6. Cancel: author contractor only, from PENDIENTE or ACEPTADA (other state -> 409). -> 200
    @PatchMapping("/{id}/cancelar")
    @PreAuthorize("hasRole('CONTRATANTE')")
    public ResponseEntity<SolicitudResponse> cancel(
            @PathVariable Long id,
            Authentication auth) {
        return ResponseEntity.ok(solicitudService.cancel(auth.getName(), id));
    }
}
