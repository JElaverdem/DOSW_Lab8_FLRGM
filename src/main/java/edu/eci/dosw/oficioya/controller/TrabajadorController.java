package edu.eci.dosw.oficioya.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.eci.dosw.oficioya.dto.PerfilPublicoResponse;
import edu.eci.dosw.oficioya.service.UsuarioService;

@RestController
@RequestMapping("/api/v1/trabajadores")
public class TrabajadorController {

    private final UsuarioService usuarioService;

    public TrabajadorController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public List<PerfilPublicoResponse> listar() {
        return usuarioService.listarTrabajadores();
    }

    @GetMapping("/{id}")
    public PerfilPublicoResponse obtener(@PathVariable Long id) {
        return usuarioService.obtenerTrabajador(id);
    }
}