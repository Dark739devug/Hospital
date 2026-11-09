package com.example.hospital.tratamientomedicamento.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record TratamientoMedicamentoDTO(
    @Schema(description = "Id del medicamento (debe existir y estar activo).", example = "1")
    @NotNull(message = "El medicamento es obligatorio")
    Long idMedicamento,
    @Schema(description = "Id del tratamiento (debe existir y estar activo).", example = "1")
    @NotNull(message = "El tratamiento es obligatorio")
    Long idTratamiento
) {}
