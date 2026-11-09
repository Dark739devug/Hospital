package com.example.hospital.hospitalizaciones.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * Resumen mínimo de una hospitalización para elegirla en caja (facturas y cuentas).
 * NO incluye motivo de ingreso, diagnóstico, médico ni clínica.
 */
@Schema(description = "Resumen mínimo de una hospitalización (sin motivo ni diagnóstico)")
public record HospitalizacionResumenDTO(
        @Schema(description = "Identificador del registro. Lo genera el servidor: no se envía al crear (se ignora).", example = "1")
        Long idHospitalizacion,
        @Schema(description = "Fecha y hora de ingreso.", example = "2026-03-10T14:00:00")
        LocalDateTime fechaIngreso,
        @Schema(description = "Fecha y hora de alta; nulo si sigue hospitalizado.", example = "2026-03-14T11:00:00")
        LocalDateTime fechaAlta,
        @Schema(description = "Descripción del estado (Activa, Alta, ...)")
        String estado,
        @Schema(description = "Número de la habitación")
        String numeroHabitacion
) {}
