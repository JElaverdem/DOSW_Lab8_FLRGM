package edu.eci.dosw.oficioya.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

import edu.eci.dosw.oficioya.entity.Rol;
import edu.eci.dosw.oficioya.entity.Usuario;
import edu.eci.dosw.oficioya.repository.UsuarioRepository;

@Configuration
@Profile("dev")
public class AdminSeeder {

    @Bean
    CommandLineRunner crearAdmin(UsuarioRepository repository,
                                 PasswordEncoder encoder,
                                 @Value("${ADMIN_CORREO}") String correo,
                                 @Value("${ADMIN_PASSWORD}") String password) {
        return args -> {
            String normalizado = correo.trim().toLowerCase();
            if (repository.existsByCorreo(normalizado)) {
                return;
            }
            Usuario admin = new Usuario();
            admin.setNombre("Administrador");
            admin.setCorreo(normalizado);
            admin.setTelefono("3000000000");
            admin.setPasswordHash(encoder.encode(password));
            admin.setRol(Rol.ADMIN);
            repository.save(admin);
        };
    }
}