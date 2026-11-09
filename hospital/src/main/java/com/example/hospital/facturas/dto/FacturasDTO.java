package com.example.hospital.facturas.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record FacturasDTO(
    @Schema(description = "Identificador del registro. Lo genera el servidor: no se envía al crear (se ignora).", example = "1")
    Long idFactura,
    @Schema(description = "Id del paciente (debe existir y estar activo).", example = "1")
    @NotNull(message = "El paciente es obligatorio")
    Long idPaciente,
    @Schema(description = "Id de la hospitalización.", example = "1")
    Long idHospitalizacion,
    @Schema(description = "Solo lectura: la asigna el servidor al emitir (hora actual); el cliente no la puede fijar ni cambiar.",
            accessMode = Schema.AccessMode.READ_ONLY)
    LocalDateTime fechaEmision,
    @Schema(description = "Solo lectura: suma de los importes de las líneas (la calcula el servidor; se ignora lo que envíe el cliente).",
            accessMode = Schema.AccessMode.READ_ONLY)
    BigDecimal subtotal,
    @Schema(description = "Solo lectura: subtotal x tasa de IVA configurada en el servidor (`app.facturacion.iva`, 0.12 por defecto).",
            accessMode = Schema.AccessMode.READ_ONLY)
    BigDecimal iva,
    @Schema(description = "Solo lectura: subtotal + IVA.", accessMode = Schema.AccessMode.READ_ONLY)
    BigDecimal total,
    @Schema(description = "Id del método de pago (1 = Efectivo, 2 = Tarjeta, 3 = Transferencia, 4 = Seguro).", example = "1")
    @NotNull(message = "El método de pago es obligatorio")
    Long idMetodoPago,
    @Schema(description = "Solo lectura: la factura nace Pendiente; el estado cambia solo con `PATCH /{id}/estado/{idEstado}` "
            + "o automáticamente al liquidarse con pagos.", accessMode = Schema.AccessMode.READ_ONLY)
    Long idEstadoFactura,
    @Schema(description = "Solo lectura: para anular use DELETE.", accessMode = Schema.AccessMode.READ_ONLY)
    Boolean activo
) {}
