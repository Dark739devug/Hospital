package com.example.hospital.medicoespecialidades.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record MedicoEspecialidadesDTO(
    @Schema(description = "Id de la especialidad (debe existir y estar activa).", example = "1")
    @NotNull(message = "La especialidad es obligatoria")
    Long idEspecialidad,
    @Schema(description = "Id del médico (debe existir y estar activo).", example = "1")
    @NotNull(message = "El médico es obligatorio")
    Long idMedico
) {}
