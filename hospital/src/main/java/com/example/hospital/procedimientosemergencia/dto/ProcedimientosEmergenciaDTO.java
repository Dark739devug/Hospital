package com.example.hospital.procedimientosemergencia.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public record ProcedimientosEmergenciaDTO(
    @Schema(description = "Identificador del registro. Lo genera el servidor: no se envía al crear (se ignora).", example = "1")
    Long idProcedimiento,
    @Schema(description = "Id del ingreso a emergencia.", example = "1")
    @NotNull(message = "La emergencia es obligatoria")
    Long idEmergencia,
    @Schema(description = "Descripción del procedimiento (máximo 200 caracteres).", example = "Sutura de herida en antebrazo")
    @NotBlank(message = "La descripción del procedimiento es obligatoria")
    @Size(max = 200, message = "La descripción no puede superar 200 caracteres")
    String descripcion,
    @Schema(description = "Fecha y hora del procedimiento. Si se omite, ahora.", example = "2026-03-15T22:45:00")
    LocalDateTime fechaHora,
    @Schema(description = "Id del médico (debe existir y estar activo).", example = "1")
    @NotNull(message = "El médico es obligatorio")
    Long idMedico,
    @Schema(description = "Observaciones del procedimiento.", example = "Sin complicaciones")
    String observaciones,
    @Schema(description = "Indica si el registro está activo. Si se omite al crear queda activo; false equivale a un borrado lógico.", example = "true")
    Boolean activo
) {}
