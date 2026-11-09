package com.example.hospital.alergias.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AlergiasDTO(
    @Schema(description = "Identificador del registro. Lo genera el servidor: no se envía al crear (se ignora).", example = "1")
    Long idAlergias,
    @Schema(description = "Nombre de la alergia (único).", example = "Penicilina")
    @NotBlank(message = "El nombre de la alergia es obligatorio")
    @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
    String nombre,
    @Schema(description = "Detalle de la alergia.", example = "Reacción alérgica a la penicilina y derivados")
    @Size(max = 255, message = "La descripción no puede superar los 255 caracteres")
    String descripcion,
    @Schema(description = "Indica si el registro está activo. Si se omite al crear queda activo; false equivale a un borrado lógico.", example = "true")
    Boolean activo
) {}
