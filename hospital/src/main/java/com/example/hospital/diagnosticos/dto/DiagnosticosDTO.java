package com.example.hospital.diagnosticos.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record DiagnosticosDTO(
    @Schema(description = "Identificador del registro. Lo genera el servidor: no se envía al crear (se ignora).", example = "1")
    Long idDiagnostico,
    @Schema(description = "Observaciones clínicas.", example = "Paciente con evolución favorable")
    String observaciones,
    @Schema(description = "Id del tipo de diagnóstico (1 = Presuntivo, 2 = Definitivo, 3 = Diferencial).", example = "2")
    @NotNull(message = "El tipo de diagnóstico es obligatorio")
    Long idTipoDiagnostico,
    @Schema(description = "Descripción del diagnóstico (máximo 255 caracteres).", example = "Gripe estacional")
    @NotBlank(message = "La descripción del diagnóstico es obligatoria")
    @Size(max = 255, message = "La descripción no puede superar 255 caracteres")
    String descripcion,
    @Schema(description = "Fecha del diagnóstico (no futura). Si se omite, hoy.", example = "2026-03-15")
    @PastOrPresent(message = "La fecha del diagnóstico no puede ser futura")
    LocalDate fecha,
    @Schema(description = "Indica si el registro está activo. Si se omite al crear queda activo; false equivale a un borrado lógico.", example = "true")
    Boolean activo
) {}
