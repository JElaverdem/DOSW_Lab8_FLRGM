package edu.eci.dosw.oficioya.controller;
 
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertEquals;
 
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc; // Boot 4
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
 
import edu.eci.dosw.oficioya.entity.EstadoSolicitud;
import edu.eci.dosw.oficioya.entity.Rol;
import edu.eci.dosw.oficioya.entity.Solicitud;
import edu.eci.dosw.oficioya.entity.Usuario;
import edu.eci.dosw.oficioya.repository.SolicitudRepository;
import edu.eci.dosw.oficioya.repository.UsuarioRepository;
 
/**
 * LAB-49 (PT-14 a PT-16) y LAB-51 (PT-13 y PT-17).
 * La identidad de @WithMockUser es el correo del usuario sembrado en H2,
 * igual que Authentication.getName() en producción.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SolicitudSeguridadTest {
 
    private static final String C1 = "contratante1@test.com";
    private static final String C2 = "contratante2@test.com";
    private static final String T1 = "trabajador1@test.com";
    private static final String T2 = "trabajador2@test.com";
 
    @Autowired MockMvc mvc;
    @Autowired UsuarioRepository usuarios;
    @Autowired SolicitudRepository solicitudes;
 
    Usuario contratante1, contratante2, trabajador1, trabajador2;
 
    @BeforeEach
    void sembrar() {
        solicitudes.deleteAll();
        usuarios.deleteAll();
        contratante1 = usuario(C1, Rol.CONTRATANTE);
        contratante2 = usuario(C2, Rol.CONTRATANTE);
        trabajador1 = usuario(T1, Rol.TRABAJADOR);
        trabajador2 = usuario(T2, Rol.TRABAJADOR);
    }
 
    // ---------------------------------------------------------------- PT-13
    @Test
    @DisplayName("PT-13: un TRABAJADOR no puede crear solicitudes (403)")
    @WithMockUser(username = T1, roles = "TRABAJADOR")
    void pt13_trabajadorNoCreaSolicitud() throws Exception {
        String body = """
            {"trabajadorId": %d, "descripcion": "Arreglar una fuga"}
            """.formatted(trabajador2.getId());
 
        mvc.perform(post("/api/v1/solicitudes").with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(body))
           .andExpect(status().isForbidden());
 
        assertEquals(0, solicitudes.count());
    }
 
    // ---------------------------------------------------------------- PT-14
    @Test
    @DisplayName("PT-14: el CONTRATANTE solo lista sus propias solicitudes")
    @WithMockUser(username = C1, roles = "CONTRATANTE")
    void pt14_contratanteSoloListaLasPropias() throws Exception {
        solicitud(contratante1, trabajador1);
        solicitud(contratante1, trabajador2);
        Solicitud ajena = solicitud(contratante2, trabajador1);
 
        mvc.perform(get("/api/v1/solicitudes"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.length()").value(2))
           .andExpect(jsonPath("$[?(@.id == " + ajena.getId() + ")]").isEmpty());
    }
 
    // ---------------------------------------------------------------- PT-15
    @Test
    @DisplayName("PT-15a: consultar una solicitud ajena da 404")
    @WithMockUser(username = C1, roles = "CONTRATANTE")
    void pt15_consultarAjenaDa404() throws Exception {
        Solicitud ajena = solicitud(contratante2, trabajador1);
 
        mvc.perform(get("/api/v1/solicitudes/{id}", ajena.getId()))
           .andExpect(status().isNotFound());
    }
 
    @Test
    @DisplayName("PT-15b: cancelar una solicitud ajena da 404 y no cambia la base")
    @WithMockUser(username = C1, roles = "CONTRATANTE")
    void pt15_cancelarAjenaDa404SinCambios() throws Exception {
        Solicitud ajena = solicitud(contratante2, trabajador1);
 
        mvc.perform(patch("/api/v1/solicitudes/{id}/cancelar", ajena.getId()).with(csrf()))
           .andExpect(status().isNotFound());
 
        assertEquals(EstadoSolicitud.PENDIENTE, estadoEnBase(ajena));
    }
 
    // ---------------------------------------------------------------- PT-16
    @Test
    @DisplayName("PT-16a: aceptar con un trabajador no asignado da 404")
    @WithMockUser(username = T2, roles = "TRABAJADOR")
    void pt16_aceptarConTrabajadorNoAsignadoDa404() throws Exception {
        Solicitud s = solicitud(contratante1, trabajador1); // asignada a T1, no a T2
 
        mvc.perform(patch("/api/v1/solicitudes/{id}/aceptar", s.getId()).with(csrf()))
           .andExpect(status().isNotFound());
 
        assertEquals(EstadoSolicitud.PENDIENTE, estadoEnBase(s));
    }
 
    @Test
    @DisplayName("PT-16b: aceptar con un CONTRATANTE da 403")
    @WithMockUser(username = C1, roles = "CONTRATANTE")
    void pt16_aceptarConContratanteDa403() throws Exception {
        Solicitud s = solicitud(contratante1, trabajador1);
 
        mvc.perform(patch("/api/v1/solicitudes/{id}/aceptar", s.getId()).with(csrf()))
           .andExpect(status().isForbidden());
 
        assertEquals(EstadoSolicitud.PENDIENTE, estadoEnBase(s));
    }
 
    // ---------------------------------------------------------------- PT-17
    @Test
    @DisplayName("PT-17: un ADMIN no puede consultar /solicitudes (403)")
    @WithMockUser(username = "admin@test.com", roles = "ADMIN")
    void pt17_adminNoConsultaSolicitudes() throws Exception {
        mvc.perform(get("/api/v1/solicitudes"))
           .andExpect(status().isForbidden());
    }
 
    // ------------------------------------------------------------- auxiliares
    private Usuario usuario(String correo, Rol rol) {
        Usuario u = new Usuario();
        u.setNombre(correo);
        u.setCorreo(correo);
        u.setTelefono("3000000000");
        u.setPasswordHash("hash-de-prueba");
        u.setRol(rol);
        return usuarios.save(u);
    }
 
    private Solicitud solicitud(Usuario contratante, Usuario trabajador) {
        Solicitud s = new Solicitud();
        s.setContratante(contratante);
        s.setTrabajador(trabajador);
        s.setDescripcion("Trabajo de prueba");
        s.setEstado(EstadoSolicitud.PENDIENTE);
        return solicitudes.save(s);
    }
 
    private EstadoSolicitud estadoEnBase(Solicitud s) {
        return solicitudes.findById(s.getId()).orElseThrow().getEstado();
    }
}
