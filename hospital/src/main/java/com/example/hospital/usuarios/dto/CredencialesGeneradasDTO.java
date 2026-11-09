package com.example.hospital.usuarios.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * Credenciales iniciales generadas automáticamente al registrar personal clínico (Médicos o Enfermeros).
 * La contraseña en texto claro solo se expone una vez en la respuesta de creación.
 */
public record CredencialesGeneradasDTO(
    @Schema(description = "Nombre de usuario asignado.", example = "dr.cmendez")
    String username,

    @Schema(description = "Contraseña temporal autogenerada (solo se muestra esta vez).", example = "Med#8xK2!9")
    String passwordTemporal,

    @Schema(description = "Fecha y hora límite de validez de la contraseña temporal (24 horas).", example = "2026-11-10T11:00:00")
    LocalDateTime fechaExpiracion,

    @Schema(description = "Indica si el usuario debe cambiar su contraseña.", example = "true")
    Boolean debeCambiarPassword,

    @Schema(description = "Rol asignado al usuario.", example = "MEDICO")
    String rol,

    @Schema(description = "Nombre completo del profesional.", example = "Dr. Carlos Alberto Méndez")
    String nombreCompleto
) {}

