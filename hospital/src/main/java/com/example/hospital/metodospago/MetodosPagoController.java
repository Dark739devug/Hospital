package com.example.hospital.metodospago;

import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import com.example.hospital.metodospago.dto.MetodosPagoDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/metodos-pago")
@Tag(name = "Métodos de pago", description = "Catálogo de métodos de pago (efectivo, tarjeta, transferencia, seguro)")
@ApiResponses({
        @ApiResponse(responseCode = "401", description = "Token ausente, inválido o expirado"),
        @ApiResponse(responseCode = "403", description = "El rol del usuario no tiene permiso")
})
public class MetodosPagoController {

    private final MetodosPagoService metodosPagoService;

    public MetodosPagoController(MetodosPagoService metodosPagoService) {
        this.metodosPagoService = metodosPagoService;
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'CAJERO', 'RECEPCIONISTA')")
    @GetMapping
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Consulta exitosa")
    })
    @Operation(summary = "Listar métodos de pago", description = "Devuelve los métodos de pago disponibles.\n\nRoles permitidos: ADMIN, CAJERO, RECEPCIONISTA.")
    public List<MetodosPagoDTO> findAll() {
        return metodosPagoService.findAll();
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'CAJERO', 'RECEPCIONISTA')")
    @GetMapping("/{id}")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Consulta exitosa"),
            @ApiResponse(responseCode = "400", description = "Algún parámetro de la ruta o de la consulta tiene un formato inválido (por ejemplo un id no numérico o una fecha mal escrita)"),
            @ApiResponse(responseCode = "404", description = "No existe el método de pago indicado"),
            @ApiResponse(responseCode = "422", description = "Regla de negocio incumplida")
    })
    @Operation(summary = "Obtener método de pago por ID", description = "Devuelve el detalle de un método de pago.\n\nRoles permitidos: ADMIN, CAJERO, RECEPCIONISTA.")
    public MetodosPagoDTO findById(@Parameter(description = "Id del registro en «Métodos de pago» (debe existir).", example = "1") @PathVariable Long id) {
        return metodosPagoService.findById(id);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Creado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos del cuerpo inválidos: ver `detalles` con el mensaje de cada campo"),
            @ApiResponse(responseCode = "409", description = "Ya existe un método de pago con esa descripción"),
            @ApiResponse(responseCode = "422", description = "La descripción del método de pago es obligatoria; La descripción no puede exceder 50 caracteres")
    })
    @Operation(summary = "Registrar método de pago", description = "Agrega un nuevo método de pago al catálogo.\n\nRoles permitidos: ADMIN.")
    public ResponseEntity<MetodosPagoDTO> crear(@Valid @RequestBody MetodosPagoDTO dto) {
        return new ResponseEntity<>(metodosPagoService.crear(dto), HttpStatus.CREATED);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Actualizado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos del cuerpo inválidos: ver `detalles` con el mensaje de cada campo; Algún parámetro de la ruta o de la consulta tiene un formato inválido (por ejemplo un id no numérico o una fecha mal escrita)"),
            @ApiResponse(responseCode = "404", description = "No existe el método de pago indicado"),
            @ApiResponse(responseCode = "409", description = "Ya existe un método de pago con esa descripción"),
            @ApiResponse(responseCode = "422", description = "La descripción del método de pago es obligatoria; La descripción no puede exceder 50 caracteres")
    })
    @Operation(summary = "Actualizar método de pago", description = "Modifica un método de pago.\n\nRoles permitidos: ADMIN.")
    public ResponseEntity<MetodosPagoDTO> actualizar(@Parameter(description = "Id del registro en «Métodos de pago» (debe existir).", example = "1") @PathVariable Long id, @Valid @RequestBody MetodosPagoDTO dto) {
        return ResponseEntity.ok(metodosPagoService.actualizar(dto, id));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Operación realizada sin contenido en la respuesta"),
            @ApiResponse(responseCode = "400", description = "Algún parámetro de la ruta o de la consulta tiene un formato inválido (por ejemplo un id no numérico o una fecha mal escrita)"),
            @ApiResponse(responseCode = "404", description = "No existe el método de pago indicado"),
            @ApiResponse(responseCode = "409", description = "No se puede eliminar el método de pago porque lo usan facturas o pagos"),
            @ApiResponse(responseCode = "422", description = "Regla de negocio incumplida")
    })
    @Operation(summary = "Eliminar método de pago", description = "Desactiva un método de pago. No se permite si hay facturas o pagos que lo usan.\n\nRoles permitidos: ADMIN.")
    public ResponseEntity<Void> eliminar(@Parameter(description = "Id del registro en «Métodos de pago» (debe existir).", example = "1") @PathVariable Long id) {
        metodosPagoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
