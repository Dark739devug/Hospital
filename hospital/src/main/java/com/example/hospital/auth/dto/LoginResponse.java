package com.example.hospital.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
public record LoginResponse(
    @Schema(description = "Token JWT para el encabezado Authorization: Bearer.", example = "eyJhbGciOiJIUzI1NiJ9...")
    String token,
    @Schema(description = "Tipo de token; siempre Bearer.", example = "Bearer")
    String type
) {}
