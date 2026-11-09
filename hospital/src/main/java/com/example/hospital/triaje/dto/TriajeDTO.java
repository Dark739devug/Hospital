package com.example.hospital.triaje.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TriajeDTO(
    @Schema(description = "Identificador del registro. Lo genera el servidor: no se envía al crear (se ignora).", example = "1")
    Long idTriaje,
    @Schema(description = "Nivel de triaje (único, máximo 50 caracteres).", example = "N6")
    @NotBlank(message = "El nivel de emergencia es obligatorio")
    @Size(max = 50, message = "El nivel de emergencia no puede superar 50 caracteres")
    String nivelDeEmergencia,
    @Schema(description = "Descripción del nivel.", example = "Seguimiento")
    @Size(max = 255, message = "La descripción no puede superar 255 caracteres")
    String descripcion,
    @Schema(description = "Indica si el registro está activo. Si se omite al crear queda activo; false equivale a un borrado lógico.", example = "true")
    Boolean activo
) {}
