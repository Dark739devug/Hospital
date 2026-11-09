package com.example.hospital.asignacionenfermeros.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record AsignacionEnfermerosDTO(
    @Schema(description = "Identificador del registro. Lo genera el servidor: no se envía al crear (se ignora).", example = "1")
    Long idAsignacion,
    @Schema(description = "Id del enfermero (debe existir y estar activo).", example = "1")
    @NotNull(message = "El enfermero es obligatorio")
    Long idEnfermero,
    @Schema(description = "Id de la hospitalización.", example = "1")
    @NotNull(message = "La hospitalización es obligatoria")
    Long idHospitalizacion,
    @Schema(description = "Fecha de la asignación yyyy-MM-dd.", example = "2026-03-15")
    @NotNull(message = "La fecha de asignación es obligatoria")
    LocalDate fechaAsignacion,
    @Schema(description = "Id del turno (1 = Matutino, 2 = Vespertino, 3 = Nocturno).", example = "1")
    @NotNull(message = "El turno es obligatorio")
    Long idTurno,
    @Schema(description = "Indica si el registro está activo. Si se omite al crear queda activo; false equivale a un borrado lógico.", example = "true")
    Boolean activo
) {}
