package com.example.hospital.metodospago.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MetodosPagoDTO(
    @Schema(description = "Identificador del registro. Lo genera el servidor: no se envía al crear (se ignora).", example = "1")
    Long idMetodoPago,
    @Schema(description = "Nombre del método de pago (único).", example = "Cheque")
    @NotBlank(message = "La descripción es obligatoria")
    @Size(max = 50, message = "La descripción no puede superar los 50 caracteres")
    String descripcion,
    @Schema(description = "Indica si el registro está activo. Si se omite al crear queda activo; false equivale a un borrado lógico.", example = "true")
    Boolean activo
) {}
