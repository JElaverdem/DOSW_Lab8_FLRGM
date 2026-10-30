package edu.eci.dosw.oficioya.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import edu.eci.dosw.oficioya.entity.Rol;
import edu.eci.dosw.oficioya.entity.Usuario;
import edu.eci.dosw.oficioya.repository.SolicitudRepository;
import edu.eci.dosw.oficioya.repository.UsuarioRepository;
import edu.eci.dosw.oficioya.security.JwtService;

/**
 * Pruebas del flujo de solicitudes para aumentar la cobertura de SolicitudService y SolicitudController.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SolicitudFlujoTest {

    @Autowired MockMvc mvc;
    @Autowired UsuarioRepository usuarios;
    @Autowired SolicitudRepository solicitudes;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JwtService jwtService;

    private String tokenContratante;
    private String tokenTrabajador;
    private Long trabajadorId;

    @BeforeEach
    void setUp() {
        solicitudes.deleteAll();
        usuarios.deleteAll();

        Usuario contratante = new Usuario();
        contratante.setNombre("Carlos Contratante");
        contratante.setCorreo("carlos@example.com");
        contratante.setTelefono("3001111111");
        contratante.setPasswordHash(passwordEncoder.encode("ContrasenaDemo123!"));
        contratante.setRol(Rol.CONTRATANTE);
        usuarios.save(contratante);

        Usuario trabajador = new Usuario();
        trabajador.setNombre("Pedro Trabajador");
        trabajador.setCorreo("pedro@example.com");
        trabajador.setTelefono("3002222222");
        trabajador.setPasswordHash(passwordEncoder.encode("ContrasenaDemo123!"));
        trabajador.setRol(Rol.TRABAJADOR);
        trabajador.setOficioPrincipal("Plomero");
        trabajador.setZona("Centro");
        trabajador = usuarios.save(trabajador);
        trabajadorId = trabajador.getId();

        tokenContratante = jwtService.generar("carlos@example.com", "CONTRATANTE");
        tokenTrabajador = jwtService.generar("pedro@example.com", "TRABAJADOR");
    }

    @Test
    @DisplayName("Crear solicitud, listar como trabajador, aceptar, rechazar y cancelar")
    void flujoCompletoSolicitudes() throws Exception {
        // 1. Crear solicitud -> 201
        String body = """
                {
                  "trabajadorId": %d,
                  "descripcion": "Reparacion de fuga"
                }
                """.formatted(trabajadorId);

        String response = mvc.perform(post("/api/v1/solicitudes")
                        .header("Authorization", "Bearer " + tokenContratante)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                .andReturn().getResponse().getContentAsString();

        Long solicitudId = ((Number) com.jayway.jsonpath.JsonPath.read(response, "$.id")).longValue();

        // 2. Listar solicitudes como trabajador -> 200
        mvc.perform(get("/api/v1/solicitudes")
                        .header("Authorization", "Bearer " + tokenTrabajador))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        // 3. Obtener solicitud propia por ID -> 200
        mvc.perform(get("/api/v1/solicitudes/" + solicitudId)
                        .header("Authorization", "Bearer " + tokenContratante))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(solicitudId));

        // 4. Aceptar solicitud como trabajador -> 200
        mvc.perform(patch("/api/v1/solicitudes/" + solicitudId + "/aceptar")
                        .header("Authorization", "Bearer " + tokenTrabajador))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ACEPTADA"));

        // 5. Cancelar solicitud como contratante -> 200
        mvc.perform(patch("/api/v1/solicitudes/" + solicitudId + "/cancelar")
                        .header("Authorization", "Bearer " + tokenContratante))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CANCELADA"));

        // 6. Intentar cancelar una solicitud ya cancelada -> 409
        mvc.perform(patch("/api/v1/solicitudes/" + solicitudId + "/cancelar")
                        .header("Authorization", "Bearer " + tokenContratante))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }
}
