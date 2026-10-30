package edu.eci.dosw.oficioya.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import edu.eci.dosw.oficioya.entity.Rol;
import edu.eci.dosw.oficioya.entity.Usuario;
import edu.eci.dosw.oficioya.repository.SolicitudRepository;
import edu.eci.dosw.oficioya.repository.UsuarioRepository;
import edu.eci.dosw.oficioya.security.JwtService;

/**
 * PT-07 a PT-09: el filtro JWT rechaza peticiones sin token, con token alterado
 * y con token vencido.
 *
 * Los tres casos deben responder 401 con el cuerpo ApiError que escribe
 * JsonAuthenticationEntryPoint, nunca 403 ni 500.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecuritySeguridadTest {

    private static final String CORREO = "ana@example.com";
    private static final String RUTA_PROTEGIDA = "/api/v1/usuarios/me";

    @Autowired MockMvc mvc;
    @Autowired UsuarioRepository usuarios;
    @Autowired SolicitudRepository solicitudes;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JwtService jwtService;

    @BeforeEach
    void setUp() {
        solicitudes.deleteAll();
        usuarios.deleteAll();

        Usuario usuario = new Usuario();
        usuario.setNombre("Ana Torres");
        usuario.setCorreo(CORREO);
        usuario.setTelefono("3001234567");
        usuario.setPasswordHash(passwordEncoder.encode("ContrasenaDemo123!"));
        usuario.setRol(Rol.TRABAJADOR);
        usuario.setOficioPrincipal("Electricista");
        usuario.setZona("Usaquen");
        usuarios.save(usuario);
    }

    @Test
    @DisplayName("PT-07: sin token responde 401 con ApiError")
    void pt07_sinToken() throws Exception {
        mvc.perform(get(RUTA_PROTEGIDA))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.mensaje").value("Token inválido o expirado"))
                .andExpect(jsonPath("$.ruta").value(RUTA_PROTEGIDA))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("PT-08: token alterado en un carácter responde 401")
    void pt08_tokenAlterado() throws Exception {
        String token = jwtService.generar(CORREO, "TRABAJADOR");

        // Línea base: el token intacto sí entra al recurso.
        mvc.perform(get(RUTA_PROTEGIDA).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correo").value(CORREO));

        mvc.perform(get(RUTA_PROTEGIDA).header("Authorization", "Bearer " + alterar(token)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.ruta").value(RUTA_PROTEGIDA));
    }

    @Test
    @DisplayName("PT-09: token vencido responde 401")
    void pt09_tokenVencido() throws Exception {
        String vencido = jwtService.generar(CORREO, "TRABAJADOR", -30);

        mvc.perform(get(RUTA_PROTEGIDA).header("Authorization", "Bearer " + vencido))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.mensaje").value("Token inválido o expirado"))
                .andExpect(jsonPath("$.ruta").value(RUTA_PROTEGIDA));
    }

    /** Changes one character inside the signature, keeping the token well formed. */
    private String alterar(String token) {
        int firma = token.lastIndexOf('.') + 1;
        int posicion = firma + (token.length() - firma) / 2;
        char original = token.charAt(posicion);
        char nuevo = original == 'A' ? 'B' : 'A';
        return token.substring(0, posicion) + nuevo + token.substring(posicion + 1);
    }
}
