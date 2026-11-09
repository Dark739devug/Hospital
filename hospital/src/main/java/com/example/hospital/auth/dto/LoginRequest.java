package com.example.hospital.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
    @Schema(description = "Nombre de usuario.", example = "admin")
    @NotBlank(message = "El usuario es obligatorio") String username,
    @Schema(description = "Contraseña del usuario.", example = "<contraseña>")
    @NotBlank(message = "La contraseña es obligatoria") String password
) {}
