package com.example.hospital.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record LoginResponse(
    @Schema(description = "Token JWT para el encabezado Authorization: Bearer.", example = "eyJhbGciOiJIUzI1NiJ9...")
    String token,
    @Schema(description = "Tipo de token; siempre Bearer.", example = "Bearer")
    String type,
    @Schema(description = "Indica si la contraseña es temporal y debe cambiarse.", example = "false")
    Boolean debeCambiarPassword,
    @Schema(description = "Fecha y hora límite de expiración de la contraseña temporal.", example = "2026-11-10T10:30:00")
    LocalDateTime fechaExpiracionPassword
) {
    public LoginResponse(String token, String type) {
        this(token, type, false, null);
    }
}
