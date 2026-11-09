package com.example.hospital.detallefactura;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import com.example.hospital.detallefactura.dto.DetalleFacturaDTO;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/detalle-factura")
@Tag(name = "Detalle de factura", description = "Líneas o conceptos cobrados dentro de una factura")
@ApiResponses({
        @ApiResponse(responseCode = "401", description = "Token ausente, inválido o expirado"),
        @ApiResponse(responseCode = "403", description = "El rol del usuario no tiene permiso")
})
public class DetalleFacturaController {

    private final DetalleFacturaService detalleFacturaService;

    public DetalleFacturaController(DetalleFacturaService detalleFacturaService) {
        this.detalleFacturaService = detalleFacturaService;
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'CAJERO', 'RECEPCIONISTA')")
    @GetMapping
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Consulta exitosa"),
            @ApiResponse(responseCode = "400", description = "Algún parámetro de la ruta o de la consulta tiene un formato inválido (por ejemplo un id no numérico o una fecha mal escrita)"),
            @ApiResponse(responseCode = "422", description = "El ID de la factura es requerido")
    })
    @Operation(summary = "Listar líneas de factura", description = "Devuelve las líneas de detalle. Con `idFactura` devuelve solo las de esa factura.\n\nRoles permitidos: ADMIN, CAJERO, RECEPCIONISTA.")
    public List<DetalleFacturaDTO> findAll(@Parameter(description = "Id de la factura.", example = "1") @RequestParam(required = false) Long idFactura) {
        if (idFactura != null) {
            return detalleFacturaService.findByFactura(idFactura);
        }
        return detalleFacturaService.findAll();
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'CAJERO', 'RECEPCIONISTA')")
    @GetMapping("/{id}")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Consulta exitosa"),
            @ApiResponse(responseCode = "400", description = "Algún parámetro de la ruta o de la consulta tiene un formato inválido (por ejemplo un id no numérico o una fecha mal escrita)"),
            @ApiResponse(responseCode = "404", description = "No existe el detalle de factura indicado")
    })
    @Operation(summary = "Obtener línea por ID", description = "Devuelve una línea de detalle (concepto, cantidad, precio unitario e importe).\n\nRoles permitidos: ADMIN, CAJERO, RECEPCIONISTA.")
    public DetalleFacturaDTO findById(@Parameter(description = "Id del registro en «Detalle de factura» (debe existir).", example = "1") @PathVariable Long id) {
        return detalleFacturaService.findById(id);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'CAJERO', 'RECEPCIONISTA')")
    @PostMapping
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Creado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos del cuerpo inválidos: ver `detalles` con el mensaje de cada campo"),
            @ApiResponse(responseCode = "422", description = "La factura indicada no existe; El subtotal de la factura supera el máximo permitido de 99,999,999.99; El total de la factura supera el máximo permitido de 99,999,999.99; El cambio dejaría el total de la factura por debajo de lo ya pagado : anule primero los pagos necesarios; La factura seleccionada se encuentra anulada")
    })
    @Operation(summary = "Agregar línea a factura", description = "Agrega un concepto cobrado a una factura. El importe se calcula como cantidad × precio unitario (se ignora el que envíe el cliente) y el servidor recalcula el subtotal, el IVA (tasa `app.facturacion.iva`) y el total de la factura. Solo en facturas activas y Pendientes.\n\nErrores: 400 (campos inválidos, con `detalles`), 422 (factura inexistente, anulada, Pagada o Cancelada; el total quedaría por debajo de lo pagado; importe fuera de rango).\n\nRoles permitidos: ADMIN, CAJERO, RECEPCIONISTA.")
    public ResponseEntity<DetalleFacturaDTO> save(@Valid @RequestBody DetalleFacturaDTO dto) {
        DetalleFacturaDTO creado = detalleFacturaService.crear(dto);
        return new ResponseEntity<>(creado, HttpStatus.CREATED);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'CAJERO')")
    @PutMapping("/{id}")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Actualizado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos del cuerpo inválidos: ver `detalles` con el mensaje de cada campo; Algún parámetro de la ruta o de la consulta tiene un formato inválido (por ejemplo un id no numérico o una fecha mal escrita)"),
            @ApiResponse(responseCode = "404", description = "No existe el detalle de factura indicado"),
            @ApiResponse(responseCode = "422", description = "No se puede mover una línea a otra factura: elimínela y agréguela en la factura destino; La factura asociada al detalle no existe; El subtotal de la factura supera el máximo permitido de 99,999,999.99; El total de la factura supera el máximo permitido de 99,999,999.99; El cambio dejaría el total de la factura por debajo de lo ya pagado : anule primero los pagos necesarios")
    })
    @Operation(summary = "Actualizar línea", description = "Modifica concepto, cantidad o precio de una línea y recalcula los totales de la factura. La línea NO se puede mover a otra factura: `idFactura` debe ser el mismo (422 si no). Solo en facturas activas y Pendientes.\n\nErrores: 400, 404, 422.\n\nRoles permitidos: ADMIN, CAJERO.")
    public ResponseEntity<DetalleFacturaDTO> update(
            @Parameter(description = "Id del registro en «Detalle de factura» (debe existir).", example = "1") @PathVariable Long id,
            @Valid @RequestBody DetalleFacturaDTO dto) {
        DetalleFacturaDTO actualizado = detalleFacturaService.actualizar(dto, id);
        return ResponseEntity.ok(actualizado);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Operación realizada sin contenido en la respuesta"),
            @ApiResponse(responseCode = "400", description = "Algún parámetro de la ruta o de la consulta tiene un formato inválido (por ejemplo un id no numérico o una fecha mal escrita)"),
            @ApiResponse(responseCode = "404", description = "No existe el detalle de factura indicado"),
            @ApiResponse(responseCode = "422", description = "La factura asociada no existe; El subtotal de la factura supera el máximo permitido de 99,999,999.99; El total de la factura supera el máximo permitido de 99,999,999.99; El cambio dejaría el total de la factura por debajo de lo ya pagado : anule primero los pagos necesarios; La factura seleccionada se encuentra anulada")
    })
    @Operation(summary = "Eliminar línea", description = "Quita una línea de la factura y recalcula los totales. Solo en facturas activas y Pendientes (422 en caso contrario).\n\nRoles permitidos: ADMIN.")
    public ResponseEntity<Void> delete(@Parameter(description = "Id del registro en «Detalle de factura» (debe existir).", example = "1") @PathVariable Long id) {
        detalleFacturaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
