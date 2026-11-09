package com.example.hospital.consultas.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Medicamento que se receta en un tratamiento de la consulta y se cobra en la factura de esa consulta. */
public record MedicamentoConsultaDTO(
    @Schema(description = "Tratamiento de un diagnóstico de la consulta.", example = "3")
    @NotNull(message = "El tratamiento es obligatorio")
    Long idTratamiento,
    @Schema(description = "Medicamento activo.", example = "5")
    @NotNull(message = "El medicamento es obligatorio")
    Long idMedicamento,
    @Schema(description = "Unidades que se cobran.", example = "2")
    @NotNull(message = "La cantidad es obligatoria")
    @Min(value = 1, message = "La cantidad debe ser mayor que cero")
    @Max(value = 1000, message = "La cantidad no puede superar 1000")
    Integer cantidad
) {}
