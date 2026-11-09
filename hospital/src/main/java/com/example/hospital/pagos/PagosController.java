package com.example.hospital.pagos;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import com.example.hospital.pagos.dto.PagosDTO;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pagos")
@Tag(name = "Pagos", description = "Pagos aplicados a facturas y cuentas")
@ApiResponses({
        @ApiResponse(responseCode = "401", description = "Token ausente, inválido o expirado"),
        @ApiResponse(responseCode = "403", description = "El rol del usuario no tiene permiso")
})
public class PagosController {

    private final PagosService pagosService;

    public PagosController(PagosService pagosService) {
        this.pagosService = pagosService;
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'CAJERO')")
    @GetMapping
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Consulta exitosa")
    })
    @Operation(summary = "Listar pagos", description = "Devuelve todos los pagos registrados (incluidos los anulados, `activo=false`).\n\nRoles permitidos: ADMIN, CAJERO.")
    public List<PagosDTO> findAll() {
        return pagosService.findAll();
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'CAJERO')")
    @GetMapping("/{id}")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Consulta exitosa"),
            @ApiResponse(responseCode = "400", description = "Algún parámetro de la ruta o de la consulta tiene un formato inválido (por ejemplo un id no numérico o una fecha mal escrita)"),
            @ApiResponse(responseCode = "404", description = "No existe el pago indicado"),
            @ApiResponse(responseCode = "422", description = "Regla de negocio incumplida")
    })
    @Operation(summary = "Obtener pago por ID", description = "Devuelve el detalle de un pago.\n\nRoles permitidos: ADMIN, CAJERO.")
    public PagosDTO findById(@Parameter(description = "Id del registro en «Pagos» (debe existir).", example = "1") @PathVariable Long id) {
        return pagosService.findById(id);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'CAJERO')")
    @PostMapping
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Creado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos del cuerpo inválidos: ver `detalles` con el mensaje de cada campo"),
            @ApiResponse(responseCode = "422", description = "La factura está anulada: no admite pagos; Solo se registran pagos sobre facturas Pendientes (la factura está Pagada o Cancelada); El monto del pago debe ser mayor que 0; La fecha de pago no puede ser futura; El monto supera el saldo pendiente de la factura")
    })
    @Operation(summary = "Registrar pago", description = "Registra un pago sobre una factura con un método de pago. Reglas: el monto debe ser mayor que 0 y NO superar el saldo pendiente; la factura debe estar activa y Pendiente; el método de pago y la cuenta (opcional, del mismo paciente de la factura) deben existir y estar activos; la fecha no puede ser futura. Si el saldo queda en 0 la factura pasa automáticamente a Pagada.\n\nErrores: 400 (campos inválidos, con `detalles`), 422 (cualquier regla anterior).\n\nRoles permitidos: ADMIN, CAJERO.")
    public ResponseEntity<PagosDTO> crear(@Valid @RequestBody PagosDTO dto) {
        return new ResponseEntity<>(pagosService.crear(dto), HttpStatus.CREATED);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Actualizado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos del cuerpo inválidos: ver `detalles` con el mensaje de cada campo; Algún parámetro de la ruta o de la consulta tiene un formato inválido (por ejemplo un id no numérico o una fecha mal escrita)"),
            @ApiResponse(responseCode = "404", description = "No existe el pago indicado"),
            @ApiResponse(responseCode = "422", description = "El pago está anulado: no se puede modificar ni reactivar; No se puede cambiar la factura de un pago; No se puede cambiar el monto de un pago: anúlelo y registre uno nuevo; No se puede cambiar la cuenta de un pago; Para anular un pago use DELETE")
    })
    @Operation(summary = "Actualizar pago", description = "Corrige SOLO el método de pago y la referencia. Debe enviarse la misma factura y el mismo monto (y, si se envía, la misma cuenta); si se intenta cambiarlos, o modificar/reactivar un pago anulado, responde 422. Para anular use DELETE.\n\nErrores: 400, 404, 422.\n\nRoles permitidos: ADMIN.")
    public ResponseEntity<PagosDTO> actualizar(@Parameter(description = "Id del registro en «Pagos» (debe existir).", example = "1") @PathVariable Long id, @Valid @RequestBody PagosDTO dto) {
        return ResponseEntity.ok(pagosService.actualizar(dto, id));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Operación realizada sin contenido en la respuesta"),
            @ApiResponse(responseCode = "400", description = "Algún parámetro de la ruta o de la consulta tiene un formato inválido (por ejemplo un id no numérico o una fecha mal escrita)"),
            @ApiResponse(responseCode = "404", description = "No existe el pago indicado"),
            @ApiResponse(responseCode = "422", description = "El pago ya se encuentra anulado; La factura del pago no existe; No se puede anular un pago de una factura Cancelada")
    })
    @Operation(summary = "Anular pago", description = "Anula un pago (borrado lógico). Permitido con la factura Pendiente o Pagada (si estaba Pagada vuelve a Pendiente); con la factura Cancelada o con el pago ya anulado responde 422.\n\nErrores: 404, 422.\n\nRoles permitidos: ADMIN.")
    public ResponseEntity<Void> eliminar(@Parameter(description = "Id del registro en «Pagos» (debe existir).", example = "1") @PathVariable Long id) {
        pagosService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'CAJERO')")
    @GetMapping("/factura/{idFactura}")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Consulta exitosa"),
            @ApiResponse(responseCode = "400", description = "Algún parámetro de la ruta o de la consulta tiene un formato inválido (por ejemplo un id no numérico o una fecha mal escrita)"),
            @ApiResponse(responseCode = "404", description = "No existe la factura indicada")
    })
    @Operation(summary = "Pagos de una factura", description = "Devuelve los pagos aplicados a una factura. Con `soloActivos=true` oculta los anulados (por defecto se devuelven todos, para ver el historial). Responde 404 si la factura no existe.\n\nRoles permitidos: ADMIN, CAJERO.")
    public List<PagosDTO> findByFactura(@Parameter(description = "Id de la factura.", example = "1") @PathVariable Long idFactura,
                                        @Parameter(description = "Si es true devuelve solo los registros activos; por defecto false (todos).", example = "true") @RequestParam(required = false, defaultValue = "false") boolean soloActivos) {
        return pagosService.findByFactura(idFactura, soloActivos);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'CAJERO')")
    @GetMapping("/cuenta/{idCuenta}")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Consulta exitosa"),
            @ApiResponse(responseCode = "400", description = "Algún parámetro de la ruta o de la consulta tiene un formato inválido (por ejemplo un id no numérico o una fecha mal escrita)"),
            @ApiResponse(responseCode = "404", description = "No existe la cuenta indicada")
    })
    @Operation(summary = "Pagos de una cuenta", description = "Devuelve los pagos asociados a una cuenta hospitalaria. Con `soloActivos=true` oculta los anulados (por defecto se devuelven todos). Responde 404 si la cuenta no existe.\n\nRoles permitidos: ADMIN, CAJERO.")
    public List<PagosDTO> findByCuenta(@Parameter(description = "Id de la cuenta.", example = "1") @PathVariable Long idCuenta,
                                       @Parameter(description = "Si es true devuelve solo los registros activos; por defecto false (todos).", example = "true") @RequestParam(required = false, defaultValue = "false") boolean soloActivos) {
        return pagosService.findByCuenta(idCuenta, soloActivos);
    }
}
