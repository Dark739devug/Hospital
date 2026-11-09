package com.example.hospital.tiposhabitacion.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TiposHabitacionDTO(
    @Schema(description = "Identificador del registro. Lo genera el servidor: no se envía al crear (se ignora).", example = "1")
    Long idTipoHabitacion,
    @Schema(description = "Nombre del tipo de habitación (único, máximo 50 caracteres).", example = "Suite")
    @NotBlank(message = "La descripción es obligatoria")
    @Size(max = 50, message = "La descripción no puede superar los 50 caracteres")
    String descripcion,
    @Schema(description = "Costo por día en quetzales (GTQ), mayor a cero.", example = "3500.00")
    @NotNull(message = "El costo por día es obligatorio")
    @DecimalMin(value = "0.00", inclusive = false, message = "El costo por día debe ser mayor a cero")
    @Digits(integer = 8, fraction = 2, message = "El costo por día admite hasta 8 enteros y 2 decimales")
    java.math.BigDecimal costoDia,
    @Schema(description = "Indica si el registro está activo. Si se omite al crear queda activo; false equivale a un borrado lógico.", example = "true")
    Boolean activo
) {}
