package com.example.hospital.estadocita.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EstadoCitaDTO(
    @Schema(description = "Identificador del registro. Lo genera el servidor: no se envía al crear (se ignora).", example = "1")
    Long idEstado,
    @Schema(description = "Nombre del estado de cita (único).", example = "Reprogramada")
    @Size(max = 30, message = "El nombre no puede superar los 30 caracteres")
    String nombre,
    @Schema(description = "Descripción del estado.", example = "Cita movida a otra fecha")
    @NotBlank(message = "La descripción del estado es obligatoria")
    @Size(max = 200, message = "La descripción no puede superar los 200 caracteres")
    String descripcion,
    @Schema(description = "Indica si el registro está activo. Si se omite al crear queda activo; false equivale a un borrado lógico.", example = "true")
    Boolean activo
) {}
