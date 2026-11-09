package com.example.hospital.turnos.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TurnosDTO(
    @Schema(description = "Identificador del registro. Lo genera el servidor: no se envía al crear (se ignora).", example = "1")
    Long idTurno,
    @Schema(description = "Nombre del turno (único).", example = "Matutino")
    @NotBlank(message = "La descripción del turno es obligatoria")
    @Size(max = 30, message = "La descripción no puede superar los 30 caracteres")
    String descripcion,
    @Schema(description = "Hora de inicio HH:mm:ss.", example = "06:00:00")
    @NotNull(message = "La hora de inicio es obligatoria")
    java.time.LocalTime horaInicio,
    @Schema(description = "Hora de fin HH:mm:ss.", example = "14:00:00")
    @NotNull(message = "La hora de fin es obligatoria")
    java.time.LocalTime horaFin,
    @Schema(description = "Indica si el registro está activo. Si se omite al crear queda activo; false equivale a un borrado lógico.", example = "true")
    Boolean activo
) {}
