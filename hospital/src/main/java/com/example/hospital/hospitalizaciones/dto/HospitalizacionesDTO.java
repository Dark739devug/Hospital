package com.example.hospital.hospitalizaciones.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * idEstadoHosp, fechaAlta y activo son de solo lectura: el estado lo fija el servidor (Activa al
 * ingresar, Alta con POST /{id}/alta) y la fecha de ingreso no se edita con PUT.
 */
public record HospitalizacionesDTO(
    @Schema(description = "Identificador del registro. Lo genera el servidor: no se envía al crear (se ignora).", example = "1")
    Long idHospitalizacion,
    @Schema(description = "Id del paciente (debe existir y estar activo).", example = "1")
    @NotNull(message = "El paciente es obligatorio")
    Long idPaciente,
    @Schema(description = "Id de la habitación (debe existir y estar activa).", example = "1")
    @NotNull(message = "La habitación es obligatoria")
    Long idHabitacion,
    @Schema(description = "Id del médico responsable de la hospitalización (debe estar activo).", example = "1")
    @NotNull(message = "El médico responsable es obligatorio")
    Long idMedicoResponsable,
    @Schema(description = "Id de la clínica (debe existir y estar activa).", example = "1")
    Long idClinica,
    @Schema(description = "Fecha y hora de ingreso (no futura), yyyy-MM-ddTHH:mm:ss.", example = "2026-03-10T14:00:00")
    LocalDateTime fechaIngreso,
    @Schema(description = "Fecha y hora de alta. Se registra con POST /api/hospitalizaciones/{id}/alta.", example = "2026-03-14T11:00:00")
    LocalDateTime fechaAlta,
    @Schema(description = "Motivo del ingreso.", example = "Neumonía que requiere oxígeno y antibióticos")
    @NotBlank(message = "El motivo de ingreso es obligatorio")
    String motivoIngreso,
    @Schema(description = "Diagnóstico al ingreso.", example = "Neumonía adquirida en la comunidad")
    String diagnosticoIngreso,
    @Schema(description = "Id del estado de la hospitalización (1 = Activa, 2 = Alta, 3 = Traslado, 4 = Fallecimiento).", example = "1")
    Long idEstadoHosp,
    @Schema(description = "Indica si el registro está activo. Si se omite al crear queda activo; false equivale a un borrado lógico.", example = "true")
    Boolean activo
) {}
