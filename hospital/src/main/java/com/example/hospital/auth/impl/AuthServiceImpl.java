package com.example.hospital.auth.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.transaction.annotation.Transactional;
import com.example.hospital.auth.AuthService;
import com.example.hospital.auth.dto.LoginRequest;
import com.example.hospital.auth.dto.LoginResponse;
import com.example.hospital.auth.dto.RegisterRequest;
import com.example.hospital.rol.Rol;
import com.example.hospital.rol.RolRepository;
import com.example.hospital.shared.exception.DuplicadoException;
import com.example.hospital.shared.exception.ReglaNegocioException;
import com.example.hospital.shared.security.JwtService;
import com.example.hospital.usuarios.Usuarios;
import com.example.hospital.usuarios.UsuariosRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UsuariosRepository usuariosRepository;
    private final PasswordEncoder passwordEncoder;
    private final RolRepository rolRepository;

    public AuthServiceImpl(AuthenticationManager authenticationManager,
                           JwtService jwtService,
                           UsuariosRepository usuariosRepository,
                           PasswordEncoder passwordEncoder,
                           RolRepository rolRepository) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.usuariosRepository = usuariosRepository;
        this.passwordEncoder = passwordEncoder;
        this.rolRepository = rolRepository;
    }

    @Override
    @Transactional
    public LoginResponse login(LoginRequest request) {
        Authentication auth;
        try {
            auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password())
            );
        } catch (AuthenticationException e) {
            // Nunca se registra la contraseña: solo el usuario y el motivo
            log.warn("Inicio de sesión fallido para el usuario '{}': {}", request.username(), e.getClass().getSimpleName());
            throw e;
        }

        UserDetails user = (UserDetails) auth.getPrincipal();
        // Registra el último acceso del usuario
        usuariosRepository.findByNombreUsuario(user.getUsername()).ifPresent(u -> {
            u.setUltimoAcceso(LocalDateTime.now());
            usuariosRepository.save(u);
        });
        String token = jwtService.generar(user);
        log.info("Inicio de sesión del usuario '{}' con roles {}", user.getUsername(), user.getAuthorities());
        return new LoginResponse(token, "Bearer");
    }

    @Override
    @Transactional
    public Long register(RegisterRequest request) {
        if (usuariosRepository.findByNombreUsuario(request.username()).isPresent()) {
            log.warn("Registro rechazado: el usuario '{}' ya existe", request.username());
            throw new DuplicadoException("El usuario ya existe");
        }

        String nombreRol = request.rol().trim().toUpperCase();
        Rol rol = rolRepository.findByNombre(nombreRol)
                .filter(r -> !Boolean.FALSE.equals(r.getActivo()))
                .orElseThrow(() -> new ReglaNegocioException("El rol '" + nombreRol + "' no existe o está inactivo"));

        Usuarios nuevoUsuario = new Usuarios();
        nuevoUsuario.setNombreUsuario(request.username());
        nuevoUsuario.setContrasenaHash(passwordEncoder.encode(request.password()));
        nuevoUsuario.setEstado(true);
        nuevoUsuario.setUltimoAcceso(LocalDateTime.now());
        nuevoUsuario.setIdRol(rol.getIdRol());

        Usuarios guardado = usuariosRepository.save(nuevoUsuario);
        Long idUsuario = guardado != null ? guardado.getIdUsuario() : null;
        log.info("Usuario '{}' registrado con el rol {} (id {})", request.username(), nombreRol, idUsuario);
        return idUsuario;
    }
}
