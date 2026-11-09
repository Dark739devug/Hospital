package com.example.hospital.auth;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import com.example.hospital.auth.dto.CambiarPasswordRequest;
import com.example.hospital.auth.dto.LoginRequest;
import com.example.hospital.auth.dto.LoginResponse;
import com.example.hospital.auth.dto.RegisterRequest;
import com.example.hospital.usuarios.Usuarios;
import com.example.hospital.usuarios.UsuariosRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticación", description = "Inicio de sesión, registro de usuarios y datos del usuario actual")
public class AuthController {

    private final AuthService authService;
    private final UsuariosRepository usuariosRepository;

    public AuthController(AuthService authService, UsuariosRepository usuariosRepository) {
        this.authService = authService;
        this.usuariosRepository = usuariosRepository;
    }

    @PostMapping("/login")
    @Operation(summary = "Iniciar sesión", description = "Recibe usuario y contraseña y, si son válidos, devuelve un token JWT. Ese token se envía en el encabezado `Authorization: Bearer <token>` en el resto de peticiones (en Swagger, botón **Authorize**). Responde 400 si faltan campos y 401 (con el formato de error estándar) si las credenciales son inválidas o el usuario está inactivo.\n\nAcceso: público (no requiere token).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token JWT generado"),
            @ApiResponse(responseCode = "400", description = "Faltan usuario o contraseña"),
            @ApiResponse(responseCode = "401", description = "Credenciales inválidas")
    })
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/register")
    @Operation(summary = "Registrar usuario", description = "Crea un nuevo usuario de acceso al sistema con contraseña cifrada. El campo `rol` (nombre del rol) es obligatorio. Política de contraseña: mínimo 8 caracteres. Devuelve `message` e `idUsuario`. Responde 400 si faltan datos o la contraseña es corta (con `detalles`), 409 si el usuario ya existe y 422 si el rol no existe o está inactivo.\n\nRoles permitidos: ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Usuario creado"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos o rol ausente"),
            @ApiResponse(responseCode = "409", description = "El usuario ya existe"),
            @ApiResponse(responseCode = "422", description = "Rol inexistente o inactivo")
    })
    public ResponseEntity<Map<String, Object>> register(@Valid @RequestBody RegisterRequest request) {
        Long idUsuario = authService.register(request);
        Map<String, Object> cuerpo = new java.util.LinkedHashMap<>();
        cuerpo.put("message", "Usuario '" + request.username() + "' creado exitosamente.");
        cuerpo.put("idUsuario", idUsuario);
        return ResponseEntity.status(HttpStatus.CREATED).body(cuerpo);
    }

    @PostMapping("/cambiar-password")
    @Operation(summary = "Cambiar contraseña del usuario actual", description = "Permite al usuario autenticado cambiar su contraseña ingresando la actual y la nueva. Al cambiarla, si era temporal, deja de serlo y se elimina la fecha de vencimiento.\n\nAcceso: cualquier usuario autenticado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Contraseña cambiada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos (la contraseña debe tener al menos 8 caracteres)"),
            @ApiResponse(responseCode = "401", description = "Token ausente o inválido"),
            @ApiResponse(responseCode = "422", description = "La contraseña actual es incorrecta o igual a la anterior")
    })
    public ResponseEntity<Map<String, String>> cambiarPassword(
            @Valid @RequestBody CambiarPasswordRequest request,
            Authentication authentication) {
        authService.cambiarPassword(authentication.getName(), request.passwordActual(), request.nuevaPassword());
        return ResponseEntity.ok(Map.of("message", "Contraseña cambiada exitosamente."));
    }

    @GetMapping("/me")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Nombre de usuario y roles del token"),
            @ApiResponse(responseCode = "401", description = "Token ausente, inválido o expirado")
    })
    @Operation(summary = "Usuario actual", description = "Devuelve el nombre de usuario y los roles del token con el que se hace la petición. Útil para que el frontend sepa quién inició sesión y qué menús mostrar.\n\nAcceso: cualquier usuario autenticado.")
    public ResponseEntity<Map<String, Object>> me(Authentication authentication) {
        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        Usuarios usuario = usuariosRepository.findByNombreUsuario(authentication.getName()).orElse(null);
        boolean debeCambiar = usuario != null && Boolean.TRUE.equals(usuario.getDebeCambiarPassword());
        java.time.LocalDateTime expiracion = usuario != null ? usuario.getFechaExpiracionPassword() : null;

        Map<String, Object> resp = new java.util.LinkedHashMap<>();
        resp.put("username", authentication.getName());
        resp.put("roles", roles);
        resp.put("debeCambiarPassword", debeCambiar);
        resp.put("fechaExpiracionPassword", expiracion);
        return ResponseEntity.ok(resp);
    }
}
