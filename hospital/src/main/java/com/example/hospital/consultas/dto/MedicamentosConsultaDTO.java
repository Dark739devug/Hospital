package com.example.hospital.consultas.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;

/** Medicamentos cobrados en la factura de una consulta. Sin factura todavía: idFactura null y lista vacía. */
public record MedicamentosConsultaDTO(
    @Schema(description = "Factura de la consulta (null si aún no tiene).", example = "12")
    Long idFactura,
    @Schema(description = "Estado de la factura: Pendiente, Pagada o Cancelada.", example = "Pendiente")
    String estadoFactura,
    @Schema(description = "Subtotal de la factura (todas sus líneas).", example = "291.00")
    BigDecimal subtotal,
    @Schema(description = "Total de la factura con IVA.", example = "325.92")
    BigDecimal total,
    List<Item> medicamentos
) {
    public record Item(
        Long idDetalle,
        Long idTratamiento,
        Long idMedicamento,
        String concepto,
        Integer cantidad,
        BigDecimal precioUnitario,
        BigDecimal importe
    ) {}
}
