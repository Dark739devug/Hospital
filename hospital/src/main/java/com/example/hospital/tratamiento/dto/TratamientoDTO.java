package com.example.hospital.tratamiento.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record TratamientoDTO(
    @Schema(description = "Identificador del registro. Lo genera el servidor: no se envía al crear (se ignora).", example = "1")
    Long idTratamiento,
    @Schema(description = "Descripción del tratamiento (máximo 255 caracteres).", example = "Reposo e hidratación")
    @NotBlank(message = "La descripción del tratamiento es obligatoria")
    @Size(max = 255, message = "La descripción no puede superar 255 caracteres")
    String descripcion,
    @Schema(description = "Duración en días (mayor a cero).", example = "7")
    @Positive(message = "La duración en días debe ser mayor a cero")
    Integer duracionDias,
    @Schema(description = "Indicaciones para el paciente.", example = "Tomar abundante líquido y reposar")
    String indicaciones,
    @Schema(description = "Fecha de inicio yyyy-MM-dd. Si se omite, hoy.", example = "2026-03-15")
    LocalDate fechaInicio,
    @Schema(description = "Fecha de fin yyyy-MM-dd (no anterior a la de inicio).", example = "2026-03-22")
    LocalDate fechaFin,
    @Schema(description = "Id del diagnóstico (debe existir y estar activo).", example = "1")
    @NotNull(message = "El diagnóstico asociado es obligatorio")
    Long idDiagnostico,
    @Schema(description = "Indica si el registro está activo. Si se omite al crear queda activo; false equivale a un borrado lógico.", example = "true")
    Boolean activo
) {}
