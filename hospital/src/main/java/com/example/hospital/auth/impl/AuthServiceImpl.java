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
import com.example.hospital.shared.exception.RecursoNoEncontradoException;
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
        String usernameLimpio = request.username() != null ? request.username().trim() : "";
        Usuarios usuario = usuariosRepository.findByNombreUsuario(usernameLimpio).orElse(null);

        Authentication auth;
        try {
            auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(usernameLimpio, request.password())
            );
        } catch (AuthenticationException e) {
            // Nunca se registra la contraseña: solo el usuario y el motivo
            log.warn("Inicio de sesión fallido para el usuario '{}': {}", usernameLimpio, e.getClass().getSimpleName());
            throw e;
        }

        UserDetails user = (UserDetails) auth.getPrincipal();

        // La contraseña temporal vence a las 24 horas. Se comprueba después de validar la contraseña
        // para no revelar a terceros qué cuentas tienen una contraseña temporal.
        if (usuario != null && Boolean.TRUE.equals(usuario.getDebeCambiarPassword()) && usuario.getFechaExpiracionPassword() != null
                && LocalDateTime.now().isAfter(usuario.getFechaExpiracionPassword())) {
            log.warn("Inicio de sesión rechazado: la contraseña temporal del usuario '{}' expiró el {}", usernameLimpio, usuario.getFechaExpiracionPassword());
            throw new ReglaNegocioException("La contraseña temporal ha expirado (validez máxima de 24 horas). Debe solicitar a un administrador que restablezca su acceso.");
        }
        // Registra el último acceso del usuario
        if (usuario != null) {
            usuario.setUltimoAcceso(LocalDateTime.now());
            usuariosRepository.save(usuario);
        }
        String token = jwtService.generar(user);
        boolean debeCambiar = usuario != null && Boolean.TRUE.equals(usuario.getDebeCambiarPassword());
        LocalDateTime expiracion = usuario != null ? usuario.getFechaExpiracionPassword() : null;

        log.info("Inicio de sesión del usuario '{}' con roles {} (debeCambiarPassword: {})", user.getUsername(), user.getAuthorities(), debeCambiar);
        return new LoginResponse(token, "Bearer", debeCambiar, expiracion);
    }

    @Override
    @Transactional
    public void cambiarPassword(String username, String passwordActual, String nuevaPassword) {
        if (username == null || username.isBlank()) {
            throw new ReglaNegocioException("El usuario es obligatorio.");
        }
        if (nuevaPassword == null || nuevaPassword.length() < 8) {
            throw new ReglaNegocioException("La nueva contraseña debe tener al menos 8 caracteres.");
        }

        Usuarios u = usuariosRepository.findByNombreUsuario(username.trim())
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado: " + username));

        if (!passwordEncoder.matches(passwordActual, u.getContrasenaHash())) {
            throw new ReglaNegocioException("La contraseña actual no es correcta.");
        }

        if (passwordActual.equals(nuevaPassword)) {
            throw new ReglaNegocioException("La nueva contraseña debe ser diferente a la contraseña actual.");
        }

        u.setContrasenaHash(passwordEncoder.encode(nuevaPassword));
        u.setDebeCambiarPassword(false);
        u.setFechaExpiracionPassword(null);
        usuariosRepository.save(u);
        log.info("Contraseña actualizada exitosamente para el usuario '{}'", username);
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
