package com.example.hospital.estadosfactura;

import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import com.example.hospital.estadosfactura.dto.EstadosFacturaDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/estados-factura")
@Tag(name = "Estados de factura", description = "Catálogo de estados de factura (Pendiente, Pagada, Cancelada)")
@ApiResponses({
        @ApiResponse(responseCode = "401", description = "Token ausente, inválido o expirado"),
        @ApiResponse(responseCode = "403", description = "El rol del usuario no tiene permiso")
})
public class EstadosFacturaController {

    private final EstadosFacturaService estadosFacturaService;

    public EstadosFacturaController(EstadosFacturaService estadosFacturaService) {
        this.estadosFacturaService = estadosFacturaService;
    }

    @GetMapping
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Consulta exitosa"),
            @ApiResponse(responseCode = "400", description = "Algún parámetro de la ruta o de la consulta tiene un formato inválido (por ejemplo un id no numérico o una fecha mal escrita)")
    })
    @Operation(summary = "Listar estados de factura", description = "Devuelve los estados de factura. Con `soloActivos=true` solo los activos.\n\nAcceso: cualquier usuario autenticado.")
    public List<EstadosFacturaDTO> findAll(
            @Parameter(description = "Si es true devuelve solo los registros activos; por defecto false (todos).", example = "true") @RequestParam(required = false, defaultValue = "false") boolean soloActivos) {
        if (soloActivos) {
            return estadosFacturaService.findActivos();
        }
        return estadosFacturaService.findAll();
    }

    @GetMapping("/{id}")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Consulta exitosa"),
            @ApiResponse(responseCode = "400", description = "Algún parámetro de la ruta o de la consulta tiene un formato inválido (por ejemplo un id no numérico o una fecha mal escrita)"),
            @ApiResponse(responseCode = "404", description = "No existe el estado de factura indicado")
    })
    @Operation(summary = "Obtener estado por ID", description = "Devuelve el detalle de un estado de factura.\n\nAcceso: cualquier usuario autenticado.")
    public EstadosFacturaDTO findById(@Parameter(description = "Id del registro en «Estados de factura» (debe existir).", example = "1") @PathVariable Long id) {
        return estadosFacturaService.findById(id);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Creado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos del cuerpo inválidos: ver `detalles` con el mensaje de cada campo"),
            @ApiResponse(responseCode = "409", description = "Ya existe un estado de factura con la descripción; Ya existe otro estado de factura con la descripción"),
            @ApiResponse(responseCode = "422", description = "La descripción del estado de factura es obligatoria; La descripción no puede exceder los 30 caracteres")
    })
    @Operation(summary = "Registrar estado de factura", description = "Agrega un nuevo estado al catálogo.\n\nRoles permitidos: ADMIN.")
    public ResponseEntity<EstadosFacturaDTO> save(@Valid @RequestBody EstadosFacturaDTO dto) {
        EstadosFacturaDTO creado = estadosFacturaService.crear(dto);
        return new ResponseEntity<>(creado, HttpStatus.CREATED);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Actualizado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos del cuerpo inválidos: ver `detalles` con el mensaje de cada campo; Algún parámetro de la ruta o de la consulta tiene un formato inválido (por ejemplo un id no numérico o una fecha mal escrita)"),
            @ApiResponse(responseCode = "404", description = "No existe el estado de factura indicado"),
            @ApiResponse(responseCode = "409", description = "Ya existe un estado de factura con la descripción; Ya existe otro estado de factura con la descripción"),
            @ApiResponse(responseCode = "422", description = "No se permite modificar la descripción de los estados base del sistema contable: Pendiente, Pagada, Cancelada; No se permite desactivar un estado base esencial del sistema contable; La descripción del estado de factura es obligatoria; La descripción no puede exceder los 30 caracteres")
    })
    @Operation(summary = "Actualizar estado de factura", description = "Modifica un estado de factura.\n\nRoles permitidos: ADMIN.")
    public ResponseEntity<EstadosFacturaDTO> update(
            @Parameter(description = "Id del registro en «Estados de factura» (debe existir).", example = "1") @PathVariable Long id,
            @Valid @RequestBody EstadosFacturaDTO dto) {
        EstadosFacturaDTO actualizado = estadosFacturaService.actualizar(dto, id);
        return ResponseEntity.ok(actualizado);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Operación realizada sin contenido en la respuesta"),
            @ApiResponse(responseCode = "400", description = "Algún parámetro de la ruta o de la consulta tiene un formato inválido (por ejemplo un id no numérico o una fecha mal escrita)"),
            @ApiResponse(responseCode = "404", description = "No existe el estado de factura indicado"),
            @ApiResponse(responseCode = "422", description = "No está permitido eliminar los estados base esenciales del sistema; No se puede eliminar el estado de factura porque existen facturas registradas con este estado")
    })
    @Operation(summary = "Eliminar estado de factura", description = "Desactiva un estado. No se permite si hay facturas que lo usan.\n\nRoles permitidos: ADMIN.")
    public ResponseEntity<Void> delete(@Parameter(description = "Id del registro en «Estados de factura» (debe existir).", example = "1") @PathVariable Long id) {
        estadosFacturaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
