package com.example.hospital.estadoshospitalizacion.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EstadosHospitalizacionDTO(
    @Schema(description = "Identificador del registro. Lo genera el servidor: no se envía al crear (se ignora).", example = "1")
    Long idEstadoHosp,
    @Schema(description = "Nombre del estado de hospitalización (único, máximo 30 caracteres).", example = "En observación")
    @NotBlank(message = "La descripción es obligatoria")
    @Size(max = 30, message = "La descripción no puede superar los 30 caracteres")
    String descripcion,
    @Schema(description = "Indica si el registro está activo. Si se omite al crear queda activo; false equivale a un borrado lógico.", example = "true")
    Boolean activo
) {}
