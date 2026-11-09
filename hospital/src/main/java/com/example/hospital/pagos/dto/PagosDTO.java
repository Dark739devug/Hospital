package com.example.hospital.pagos.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PagosDTO(
    @Schema(description = "Identificador del registro. Lo genera el servidor: no se envía al crear (se ignora).", example = "1")
    Long idPago,
    @Schema(description = "Id de la factura (debe existir).", example = "1")
    @NotNull(message = "La factura es obligatoria")
    Long idFactura,
    @Schema(description = "Opcional: cuenta hospitalaria del MISMO paciente de la factura.")
    Long idCuenta,
    @Schema(description = "Id del método de pago (1 = Efectivo, 2 = Tarjeta, 3 = Transferencia, 4 = Seguro).", example = "1")
    @NotNull(message = "El método de pago es obligatorio")
    Long idMetodoPago,
    @Schema(description = "Monto pagado en quetzales (GTQ); mayor a cero y sin superar el saldo pendiente de la factura.", example = "250.00")
    @NotNull(message = "El monto es obligatorio")
    @DecimalMin(value = "0.00", inclusive = false, message = "El monto debe ser mayor que cero")
    @Digits(integer = 8, fraction = 2, message = "El monto admite hasta 8 enteros y 2 decimales")
    BigDecimal monto,
    @Schema(description = "Opcional al registrar (por defecto, ahora); no puede ser futura. En PUT se ignora.")
    LocalDateTime fechaPago,
    @Schema(description = "Número de boleta, autorización o referencia del pago.", example = "BOL-458796")
    @Size(max = 100, message = "La referencia no puede superar los 100 caracteres")
    String referencia,
    @Schema(description = "Al registrar se ignora (el pago nace activo); para anular use DELETE. En PUT no puede reactivar un pago anulado.")
    Boolean activo
) {}
