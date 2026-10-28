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
 * Esqueleto de los seis endpoints de solicitudes (LAB-31).
 *
 * Reglas del contrato:
 *  - La identidad sale SIEMPRE de Authentication.getName() (correo del token),
 *    nunca del body ni de la URL.
 *  - ADMIN no tiene acceso a ningún endpoint de /solicitudes (RS-08).
 *  - La propiedad (solicitud ajena -> 404) la resuelve SolicitudService, no el controller.
 *  - Sin try/catch: las excepciones llegan al @RestControllerAdvice.
 *  - Siempre DTOs, nunca entidades JPA.
 */
@RestController
@RequestMapping("/solicitudes")
public class SolicitudController {

    private final SolicitudService solicitudService;

    public SolicitudController(SolicitudService solicitudService) {
        this.solicitudService = solicitudService;
    }

    // 1. Crear: solo CONTRATANTE. El contratante sale del token. -> 201
    @PostMapping
    @PreAuthorize("hasRole('CONTRATANTE')")
    public ResponseEntity<SolicitudResponse> crear(
            @Valid @RequestBody SolicitudRequest request,
            Authentication auth) {
        SolicitudResponse response = solicitudService.crear(auth.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // 2. Listar las propias: CONTRATANTE o TRABAJADOR (ADMIN -> 403). -> 200
    @GetMapping
    @PreAuthorize("hasAnyRole('CONTRATANTE', 'TRABAJADOR')")
    public ResponseEntity<List<SolicitudResponse>> listar(Authentication auth) {
        return ResponseEntity.ok(solicitudService.listar(auth.getName()));
    }

    // 3. Obtener una: solo si participa; ajena o inexistente -> 404 (lo decide el service). -> 200
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CONTRATANTE', 'TRABAJADOR')")
    public ResponseEntity<SolicitudResponse> obtener(
            @PathVariable Long id,
            Authentication auth) {
        return ResponseEntity.ok(solicitudService.obtener(auth.getName(), id));
    }

    // 4. Aceptar: solo TRABAJADOR (asignado; no asignado -> 404, contratante -> 403). -> 200
    @PatchMapping("/{id}/aceptar")
    @PreAuthorize("hasRole('TRABAJADOR')")
    public ResponseEntity<SolicitudResponse> aceptar(
            @PathVariable Long id,
            Authentication auth) {
        return ResponseEntity.ok(solicitudService.aceptar(auth.getName(), id));
    }

    // 5. Rechazar: solo TRABAJADOR asignado, solo desde PENDIENTE (otro estado -> 409). -> 200
    @PatchMapping("/{id}/rechazar")
    @PreAuthorize("hasRole('TRABAJADOR')")
    public ResponseEntity<SolicitudResponse> rechazar(
            @PathVariable Long id,
            Authentication auth) {
        return ResponseEntity.ok(solicitudService.rechazar(auth.getName(), id));
    }

    // 6. Cancelar: solo CONTRATANTE autor, desde PENDIENTE o ACEPTADA (otro estado -> 409). -> 200
    @PatchMapping("/{id}/cancelar")
    @PreAuthorize("hasRole('CONTRATANTE')")
    public ResponseEntity<SolicitudResponse> cancelar(
            @PathVariable Long id,
            Authentication auth) {
        return ResponseEntity.ok(solicitudService.cancelar(auth.getName(), id));
    }
}
