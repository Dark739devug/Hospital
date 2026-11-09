package com.example.hospital.emergencia.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

/**
 * fechaHoraAlta es solo de lectura: el alta se registra únicamente con PATCH /{id}/dar-alta
 * (en POST y PUT se ignora).
 */
public record EmergenciaDTO(
    @Schema(description = "Identificador del registro. Lo genera el servidor: no se envía al crear (se ignora).", example = "1")
    Long idEmergencia,
    @Schema(description = "Nombre provisional si el paciente no está identificado (se usa cuando no se envía idPaciente).", example = "Paciente no identificado 01")
    @Size(max = 150, message = "El nombre temporal no puede superar 150 caracteres")
    String nombreTemporal,
    @Schema(description = "Fecha y hora de ingreso (no futura). Si se omite, ahora.", example = "2026-03-15T22:10:00")
    LocalDateTime fechaHoraIngreso,
    @Schema(description = "Motivo del ingreso a emergencia.", example = "Dolor torácico intenso")
    @NotBlank(message = "El motivo de atención de la emergencia es obligatorio")
    String motivo,
    @Schema(description = "Signos vitales al ingreso en texto libre.", example = "PA 140/90, FC 98, T 36.8")
    String signosVitales,
    @Schema(description = "Fecha y hora de alta de emergencia. Se registra con PATCH /api/emergencia/{id}/dar-alta.", example = "2026-03-16T03:00:00")
    LocalDateTime fechaHoraAlta,
    @Schema(description = "Id del paciente (debe existir y estar activo).", example = "1")
    Long idPaciente,
    @Schema(description = "Id de la habitación (debe existir y estar activa).", example = "1")
    Long idHabitacion,
    @Schema(description = "Id del médico (debe existir y estar activo).", example = "1")
    Long idMedico,
    @Schema(description = "Id del nivel de triaje (1 = N1 ... 5 = N5; debe estar activo).", example = "3")
    @NotNull(message = "El nivel de triaje es obligatorio")
    Long idTriaje,
    @Schema(description = "Indica si el registro está activo. Si se omite al crear queda activo; false equivale a un borrado lógico.", example = "true")
    Boolean activo
) {}
