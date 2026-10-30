package edu.eci.dosw.oficioya.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import org.springframework.test.web.servlet.MvcResult;

import com.jayway.jsonpath.JsonPath;

import edu.eci.dosw.oficioya.entity.Usuario;
import edu.eci.dosw.oficioya.repository.SolicitudRepository;
import edu.eci.dosw.oficioya.repository.UsuarioRepository;
import edu.eci.dosw.oficioya.security.JwtService;
import io.jsonwebtoken.Claims;

/**
 * PT-01 a PT-06: registro, almacenamiento de contraseñas y login.
 *
 * Las rutas /api/v1/auth/** son públicas, así que estas pruebas no usan
 * @WithMockUser: ejercitan el flujo real de registro y login.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthSeguridadTest {

    private static final String CORREO = "pedro@example.com";
    private static final String PASSWORD = "ContrasenaDemo123!";

    @Autowired MockMvc mvc;
    @Autowired UsuarioRepository usuarios;
    @Autowired SolicitudRepository solicitudes;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JwtService jwtService;

    @BeforeEach
    void limpiar() {
        solicitudes.deleteAll();
        usuarios.deleteAll();
    }

    @Test
    @DisplayName("PT-01: registro válido responde 201 sin contraseña ni hash")
    void pt01_registroValido() throws Exception {
        mvc.perform(post("/api/v1/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registro(CORREO.toUpperCase(), PASSWORD, "TRABAJADOR")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.nombre").value("Pedro Gomez"))
                .andExpect(jsonPath("$.correo").value(CORREO))
                .andExpect(jsonPath("$.rol").value("TRABAJADOR"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    @DisplayName("PT-02: registro con correo repetido responde 409")
    void pt02_correoRepetido() throws Exception {
        registrar(CORREO, PASSWORD, "TRABAJADOR");

        mvc.perform(post("/api/v1/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registro(CORREO.toUpperCase(), PASSWORD, "CONTRATANTE")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.mensaje").value("El correo ya está registrado"));
    }

    @Test
    @DisplayName("PT-03: registro con rol ADMIN responde 400")
    void pt03_rolAdmin() throws Exception {
        mvc.perform(post("/api/v1/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registro(CORREO, PASSWORD, "ADMIN")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.mensaje").value("Rol no permitido en el registro"));
    }

    @Test
    @DisplayName("PT-03: registro con contraseña corta responde 400")
    void pt03_passwordCorta() throws Exception {
        mvc.perform(post("/api/v1/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registro(CORREO, "corta12", "TRABAJADOR")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("PT-04: la contraseña se guarda como hash BCrypt distinto del texto")
    void pt04_passwordHasheada() throws Exception {
        registrar(CORREO, PASSWORD, "TRABAJADOR");

        Usuario guardado = usuarios.findByCorreo(CORREO).orElseThrow();

        assertNotEquals(PASSWORD, guardado.getPasswordHash());
        assertTrue(guardado.getPasswordHash().startsWith("$2"),
                "el hash debe ser BCrypt ($2a$ / $2b$ / $2y$)");
        assertTrue(passwordEncoder.matches(PASSWORD, guardado.getPasswordHash()),
                "el hash debe validar contra la contraseña original");
    }

    @Test
    @DisplayName("PT-05: login correcto responde 200 con un JWT válido")
    void pt05_loginCorrecto() throws Exception {
        registrar(CORREO, PASSWORD, "TRABAJADOR");

        MvcResult result = mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(login(CORREO, PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.expiraEn").value(1800))
                .andReturn();

        String token = JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
        Claims claims = jwtService.validar(token);

        assertEquals(CORREO, claims.getSubject());
        assertEquals("TRABAJADOR", claims.get("role", String.class));
        assertEquals("oficioya", claims.getIssuer());
    }

    @Test
    @DisplayName("PT-06: login fallido responde 401 con el mismo mensaje en ambos casos")
    void pt06_loginFallidoMismoMensaje() throws Exception {
        registrar(CORREO, PASSWORD, "TRABAJADOR");

        MvcResult claveMala = mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(login(CORREO, "OtraClave123!")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.accessToken").doesNotExist())
                .andReturn();

        MvcResult correoInexistente = mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(login("nadie@example.com", PASSWORD)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.accessToken").doesNotExist())
                .andReturn();

        String mensajeClaveMala =
                JsonPath.read(claveMala.getResponse().getContentAsString(), "$.mensaje");
        String mensajeCorreoInexistente =
                JsonPath.read(correoInexistente.getResponse().getContentAsString(), "$.mensaje");

        assertEquals(mensajeClaveMala, mensajeCorreoInexistente,
                "el mensaje no debe revelar si el correo existe");
        assertFalse(mensajeClaveMala.toLowerCase().contains("existe"));
    }

    private String registro(String correo, String password, String rol) {
        return """
                {
                  "nombre": "Pedro Gomez",
                  "correo": "%s",
                  "telefono": "3001234567",
                  "password": "%s",
                  "rol": "%s",
                  "oficioPrincipal": "Plomero",
                  "zona": "Chapinero"
                }
                """.formatted(correo, password, rol);
    }

    private String login(String correo, String password) {
        return """
                {"correo": "%s", "password": "%s"}
                """.formatted(correo, password);
    }

    private void registrar(String correo, String password, String rol) throws Exception {
        mvc.perform(post("/api/v1/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registro(correo, password, rol)))
                .andExpect(status().isCreated());
    }
}
