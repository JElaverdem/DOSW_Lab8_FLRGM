package edu.eci.dosw.oficioya.controller;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import edu.eci.dosw.oficioya.entity.Rol;
import edu.eci.dosw.oficioya.entity.Usuario;
import edu.eci.dosw.oficioya.repository.SolicitudRepository;
import edu.eci.dosw.oficioya.repository.UsuarioRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PerfilesSeguridadTest {

    private static final String ADMIN = "admin@test.com";
    private static final String CONTRATANTE = "contratante@test.com";
    private static final String TRABAJADOR = "trabajador@test.com";
    private static final String TELEFONO_TRABAJADOR = "3002222222";

    @Autowired MockMvc mvc;
    @Autowired UsuarioRepository usuarios;
    @Autowired SolicitudRepository solicitudes;

    Usuario trabajador;

    @BeforeEach
    void sembrar() {
        solicitudes.deleteAll();
        usuarios.deleteAll();
        crear(ADMIN, "Administrador", Rol.ADMIN, "3000000000");
        crear(CONTRATANTE, "Carlos Contratante", Rol.CONTRATANTE, "3001111111");
        trabajador = crear(TRABAJADOR, "Pedro Gomez", Rol.TRABAJADOR, TELEFONO_TRABAJADOR);
    }

    @Test
    @DisplayName("PT-10: el perfil público de un trabajador no expone correo ni teléfono")
    @WithMockUser(username = CONTRATANTE, roles = "CONTRATANTE")
    void pt10_perfilPublicoSinContacto() throws Exception {
        mvc.perform(get("/api/v1/trabajadores/" + trabajador.getId()))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.nombre").value("Pedro Gomez"))
           .andExpect(jsonPath("$.correo").doesNotExist())
           .andExpect(jsonPath("$.telefono").doesNotExist());
    }

    @Test
    @DisplayName("PT-10: el listado de trabajadores no expone correo ni teléfono")
    @WithMockUser(username = CONTRATANTE, roles = "CONTRATANTE")
    void pt10_listadoSinContacto() throws Exception {
        mvc.perform(get("/api/v1/trabajadores"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.length()").value(1))
           .andExpect(jsonPath("$[0].nombre").value("Pedro Gomez"))
           .andExpect(jsonPath("$[0].correo").doesNotExist())
           .andExpect(jsonPath("$[0].telefono").doesNotExist());
    }

    @Test
    @DisplayName("PT-11: un CONTRATANTE no puede consultar /admin/usuarios (403)")
    @WithMockUser(username = CONTRATANTE, roles = "CONTRATANTE")
    void pt11_contratanteNoEsAdmin() throws Exception {
        mvc.perform(get("/api/v1/admin/usuarios"))
           .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PT-12: un ADMIN consulta /admin/usuarios y ve los datos de contacto (200)")
    @WithMockUser(username = ADMIN, roles = "ADMIN")
    void pt12_adminVeContacto() throws Exception {
        mvc.perform(get("/api/v1/admin/usuarios"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$[*].correo", hasItem(TRABAJADOR)))
           .andExpect(jsonPath("$[*].telefono", hasItem(TELEFONO_TRABAJADOR)));
    }

    private Usuario crear(String correo, String nombre, Rol rol, String telefono) {
        Usuario u = new Usuario();
        u.setNombre(nombre);
        u.setCorreo(correo);
        u.setTelefono(telefono);
        u.setPasswordHash("hash-de-prueba");
        u.setRol(rol);
        if (rol == Rol.TRABAJADOR) {
            u.setOficioPrincipal("Plomero");
            u.setZona("Chapinero");
        }
        return usuarios.save(u);
    }
}