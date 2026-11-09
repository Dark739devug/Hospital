package com.example.hospital.citas.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record CitasDTO(
    @Schema(description = "Identificador del registro. Lo genera el servidor: no se envía al crear (se ignora).", example = "1")
    Long idCita,
    @Schema(description = "Fecha de la cita (hoy o futura), formato yyyy-MM-dd.", example = "2026-12-15")
    @NotNull(message = "La fecha de la cita es obligatoria")
    LocalDate fecha,
    @Schema(description = "Hora de la cita, formato HH:mm:ss. El médico no puede tener otra cita activa a la misma fecha y hora.", example = "09:30:00")
    @NotNull(message = "La hora de la cita es obligatoria")
    LocalTime hora,
    @Schema(description = "Motivo de la consulta.", example = "Control de presión arterial")
    @Size(max = 255, message = "El motivo no puede superar 255 caracteres")
    String motivo,
    @Schema(description = "Fecha y hora de registro de la cita. La asigna el servidor.", example = "2026-03-15T10:30:00")
    LocalDateTime fechaCreacion,
    @Schema(description = "Id del estado de la cita (catálogo /api/estado-cita; 1 = Programada, 2 = Confirmada).", example = "1")
    @NotNull(message = "El estado de la cita es obligatorio")
    Long idEstado,
    @Schema(description = "Id del médico (debe existir y estar activo).", example = "1")
    @NotNull(message = "El médico es obligatorio")
    Long idMedico,
    @Schema(description = "Id del paciente (debe existir y estar activo).", example = "1")
    @NotNull(message = "El paciente es obligatorio")
    Long idPaciente,
    @Schema(description = "Indica si el registro está activo. Si se omite al crear queda activo; false equivale a un borrado lógico.", example = "true")
    Boolean activo
) {}
