package com.example.hospital.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CambiarPasswordRequest(
    @Schema(description = "Contraseña actual del usuario", example = "Temp#8x2K!9")
    @NotBlank(message = "La contraseña actual es obligatoria")
    String passwordActual,

    @Schema(description = "Nueva contraseña (mínimo 8 caracteres)", example = "ClaveSegura2026*")
    @NotBlank(message = "La nueva contraseña es obligatoria")
    @Size(min = 8, max = 100, message = "La nueva contraseña debe tener entre 8 y 100 caracteres")
    String nuevaPassword
) {}

