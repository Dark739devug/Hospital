package com.example.hospital.diagnosticoconsulta.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record DiagnosticoConsultaDTO(
    @Schema(description = "Id del diagnóstico (debe existir y estar activo).", example = "1")
    @NotNull(message = "El diagnóstico es obligatorio")
    Long idDiagnostico,
    @Schema(description = "Id de la consulta médica (debe existir y estar activa).", example = "1")
    @NotNull(message = "La consulta es obligatoria")
    Long idConsulta
) {}
