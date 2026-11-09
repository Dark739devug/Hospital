package com.example.hospital.usuarios.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.groups.Default;

import java.time.LocalDateTime;

/**
 * Usuario del sistema. `password` es SOLO de entrada (WRITE_ONLY): nunca aparece en las respuestas.
 * Al crear (grupo {@link Crear}) son obligatorios el usuario, la contraseña y el rol; al actualizar solo se modifica lo que se envía.
 */
public record UsuariosDTO(
    @Schema(description = "Identificador del registro. Lo genera el servidor: no se envía al crear (se ignora).", example = "1")
    Long idUsuario,
    @Schema(description = "Nombre de usuario (único).", example = "farmacia01")
    @NotBlank(message = "El nombre de usuario es obligatorio", groups = Crear.class)
    @Size(max = 50, message = "El nombre de usuario no puede superar los 50 caracteres", groups = {Default.class, Crear.class})
    String nombreUsuario,
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Schema(description = "Contraseña (solo de entrada; mínimo 8 caracteres). Se guarda cifrada y nunca se devuelve.", example = "<contraseña de 8 o más caracteres>", accessMode = Schema.AccessMode.WRITE_ONLY)
    @NotBlank(message = "La contraseña es obligatoria", groups = Crear.class)
    @Size(min = 8, max = 100, message = "La contraseña debe tener entre 8 y 100 caracteres", groups = {Default.class, Crear.class})
    String password,
    @Schema(description = "Indica si el usuario está activo; un usuario inactivo no puede iniciar sesión.", example = "true")
    Boolean estado,
    @Schema(description = "Último inicio de sesión. Lo registra el servidor.", example = "2026-03-15T08:00:00")
    LocalDateTime ultimoAcceso,
    @Schema(description = "Id del rol del usuario (catálogo /api/rol).", example = "1")
    @NotNull(message = "El rol es obligatorio", groups = Crear.class)
    Long idRol
) {
    /** Grupo de validación de la creación. */
    public interface Crear {}

    public static UsuariosDTO sinPassword(Long idUsuario, String nombreUsuario, Boolean estado, LocalDateTime ultimoAcceso, Long idRol) {
        return new UsuariosDTO(idUsuario, nombreUsuario, null, estado, ultimoAcceso, idRol);
    }
}
