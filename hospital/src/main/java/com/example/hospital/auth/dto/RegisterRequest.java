package com.example.hospital.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Datos para registrar un usuario. `rol` es el nombre del rol (ADMIN, MEDICO, ENFERMERO,
 * RECEPCIONISTA, FARMACEUTICO, CAJERO). Es obligatorio: no hay rol por defecto.
 * Política de contraseña: mínimo 8 caracteres (máximo 100).
 */
public record RegisterRequest(
    @Schema(description = "Nombre de usuario nuevo (máximo 50 caracteres, único).", example = "recepcion01")
    @NotBlank(message = "El usuario es obligatorio")
    @Size(max = 50, message = "El usuario no puede superar los 50 caracteres") String username,
    @Schema(description = "Contraseña de 8 a 100 caracteres.", example = "<contraseña de 8 o más caracteres>")
    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 8, max = 100, message = "La contraseña debe tener entre 8 y 100 caracteres") String password,
    @Schema(description = "Nombre del rol: ADMIN, MEDICO, ENFERMERO, RECEPCIONISTA, FARMACEUTICO o CAJERO.", example = "RECEPCIONISTA")
    @NotBlank(message = "El rol es obligatorio") String rol
) {}
