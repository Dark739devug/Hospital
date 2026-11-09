package com.example.hospital.medicamentos.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record MedicamentosDTO(
    @Schema(description = "Identificador del registro. Lo genera el servidor: no se envía al crear (se ignora).", example = "1")
    Long idMedicamento,
    @Schema(description = "Nombre comercial (único).", example = "Panadol")
    @NotBlank(message = "El nombre comercial es obligatorio")
    @Size(max = 150, message = "El nombre comercial no puede superar los 150 caracteres")
    String nombreComercial,
    @Schema(description = "Nombre genérico (principio activo).", example = "Paracetamol")
    @Size(max = 150, message = "El nombre genérico no puede superar los 150 caracteres")
    String nombreGenerico,
    @Schema(description = "Presentación del medicamento.", example = "Tabletas 500 mg, caja de 20")
    @Size(max = 100, message = "La presentación no puede superar los 100 caracteres")
    String presentacion,
    @Schema(description = "Laboratorio fabricante.", example = "GlaxoSmithKline")
    @Size(max = 100, message = "El laboratorio no puede superar los 100 caracteres")
    String laboratorio,
    // Solo se usa como stock inicial en POST; PUT lo ignora (cambia solo con PATCH /{id}/stock).
    @Schema(description = "Existencias. Solo se usa como stock inicial al crear; después cambia con PATCH /api/medicamentos/{id}/stock.", example = "100")
    @PositiveOrZero(message = "El stock no puede ser negativo")
    Integer stock,
    @Schema(description = "Existencias mínimas antes de considerarse bajo stock.", example = "20")
    @PositiveOrZero(message = "El stock mínimo no puede ser negativo")
    Integer stockMinimo,
    @Schema(description = "Precio de venta en quetzales (GTQ).", example = "15.50")
    @NotNull(message = "El precio es obligatorio")
    @DecimalMin(value = "0.00", inclusive = false, message = "El precio debe ser mayor a cero")
    @Digits(integer = 8, fraction = 2, message = "El precio admite hasta 8 enteros y 2 decimales")
    BigDecimal precio,
    @Schema(description = "Indica si se vende solo con receta.", example = "false")
    Boolean requiereReceta,
    @Schema(description = "Indica si el registro está activo. Si se omite al crear queda activo; false equivale a un borrado lógico.", example = "true")
    Boolean activo
) {}
