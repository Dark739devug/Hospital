package com.example.hospital.detallefactura.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record DetalleFacturaDTO(
    @Schema(description = "Identificador del registro. Lo genera el servidor: no se envía al crear (se ignora).", example = "1")
    Long idDetalle,
    @Schema(description = "Id de la factura (debe existir).", example = "1")
    @NotNull(message = "La factura es obligatoria")
    Long idFactura,
    @Schema(description = "Concepto cobrado en la línea.", example = "Consulta médica general")
    @NotBlank(message = "El concepto es obligatorio")
    @Size(max = 200, message = "El concepto no puede superar los 200 caracteres")
    String concepto,
    @Schema(description = "Cantidad (mayor a cero).", example = "1")
    @NotNull(message = "La cantidad es obligatoria")
    @Min(value = 1, message = "La cantidad debe ser mayor que cero")
    @Max(value = 100000, message = "La cantidad no puede superar 100000")
    Integer cantidad,
    @Schema(description = "Precio unitario en quetzales (GTQ), hasta 2 decimales.", example = "250.00")
    @NotNull(message = "El precio unitario es obligatorio")
    @PositiveOrZero(message = "El precio unitario no puede ser negativo")
    @Digits(integer = 8, fraction = 2, message = "El precio unitario admite hasta 8 enteros y 2 decimales")
    BigDecimal precioUnitario,
    @Schema(description = "Solo lectura: cantidad x precio unitario, lo calcula el servidor (se ignora lo que envíe el cliente).",
            accessMode = Schema.AccessMode.READ_ONLY)
    BigDecimal importe
) {}
