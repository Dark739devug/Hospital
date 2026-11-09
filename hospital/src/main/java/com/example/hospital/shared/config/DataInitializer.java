package com.example.hospital.shared.config;

import com.example.hospital.rol.Rol;
import com.example.hospital.rol.RolRepository;
import com.example.hospital.usuarios.Usuarios;
import com.example.hospital.usuarios.UsuariosRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    // Sin el prefijo ROLE_ (Spring lo agrega internamente)
    private static final List<String> ROLES = List.of("ADMIN", "MEDICO", "ENFERMERO", "RECEPCIONISTA", "FARMACEUTICO",
            "CAJERO");

    private final RolRepository rolRepository;
    private final UsuariosRepository usuariosRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.username:admin}")
    private String adminUsername;

    @Value("${app.admin.password}")
    private String adminPassword;

    @Override
    public void run(String... args) {
        // 1. Crear roles si no existen
        for (String nombre : ROLES) {
            if (rolRepository.findByNombre(nombre).isEmpty()) {
                Rol rol = new Rol();
                rol.setNombre(nombre);
                rol.setDescripcion("Rol " + nombre);
                rol.setActivo(true);
                rolRepository.save(rol);
            }
        }

        // 2. Crear el usuario administrador inicial (solo la primera vez)
        Optional<Usuarios> adminOpt = usuariosRepository.findByNombreUsuario(adminUsername);
        Rol adminRol = rolRepository.findByNombre("ADMIN").orElseThrow(
                () -> new IllegalStateException("El rol ADMIN no pudo ser encontrado"));

        // Si el administrador ya existe no se toca: su contraseña se cambia desde la aplicación
        // y no debe volver al valor de ADMIN_PASSWORD en cada reinicio.
        if (adminOpt.isEmpty()) {
            Usuarios admin = new Usuarios();
            admin.setNombreUsuario(adminUsername);
            admin.setContrasenaHash(passwordEncoder.encode(adminPassword));
            admin.setEstado(true);
            admin.setUltimoAcceso(LocalDateTime.now());
            admin.setIdRol(adminRol.getIdRol());
            usuariosRepository.save(admin);
            System.out.println(">>> Usuario administrador inicial creado: " + adminUsername);
        }
    }
}
