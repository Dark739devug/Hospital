package com.example.hospital.estadocita;

import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import com.example.hospital.estadocita.dto.EstadoCitaDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/estado-cita")
@Tag(name = "Estados de cita", description = "Catálogo de estados por los que pasa una cita")
@ApiResponses({
        @ApiResponse(responseCode = "401", description = "Token ausente, inválido o expirado"),
        @ApiResponse(responseCode = "403", description = "El rol del usuario no tiene permiso")
})
public class EstadoCitaController {

    private final EstadoCitaService estadoCitaService;

    public EstadoCitaController(EstadoCitaService estadoCitaService) {
        this.estadoCitaService = estadoCitaService;
    }

    @GetMapping
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Consulta exitosa")
    })
    @Operation(summary = "Listar estados de cita", description = "Devuelve los estados posibles de una cita (Programada, Confirmada, Atendida, Cancelada, etc.).\n\nAcceso: cualquier usuario autenticado.")
    public List<EstadoCitaDTO> findAll() {
        return estadoCitaService.findAll();
    }

    @GetMapping("/{id}")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Consulta exitosa"),
            @ApiResponse(responseCode = "400", description = "Algún parámetro de la ruta o de la consulta tiene un formato inválido (por ejemplo un id no numérico o una fecha mal escrita)"),
            @ApiResponse(responseCode = "404", description = "No existe el estado de cita indicado")
    })
    @Operation(summary = "Obtener estado por ID", description = "Devuelve el detalle de un estado de cita.\n\nAcceso: cualquier usuario autenticado.")
    public EstadoCitaDTO findById(@Parameter(description = "Id del registro en «Estados de cita» (debe existir).", example = "1") @PathVariable Long id) {
        return estadoCitaService.findById(id);
    }

    // Endpoint auxiliar para verificar si una transición entre estados es válida
    @GetMapping("/validar-transicion")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Consulta exitosa")
    })
    @Operation(summary = "Validar cambio de estado", description = "Indica si se permite pasar una cita del estado `origen` al estado `destino` (por descripción). Devuelve `{origen, destino, permitida}`; útil para habilitar/deshabilitar botones en el frontend.\n\nAcceso: cualquier usuario autenticado.")
    public ResponseEntity<Map<String, Object>> validarTransicion(
            @Parameter(description = "Nombre del estado de origen de la cita.", example = "Programada") @RequestParam String origen,
            @Parameter(description = "Nombre del estado de destino de la cita.", example = "Confirmada") @RequestParam String destino) {
        boolean permitida = estadoCitaService.esTransicionPermitida(origen, destino);
        return ResponseEntity.ok(Map.of(
                "origen", origen,
                "destino", destino,
                "permitida", permitida
        ));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Creado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos del cuerpo inválidos: ver `detalles` con el mensaje de cada campo"),
            @ApiResponse(responseCode = "409", description = "Ya existe un estado de cita con la descripción; Ya existe un estado de cita con el nombre"),
            @ApiResponse(responseCode = "422", description = "La descripción del estado de cita es obligatoria; Descripción no permitida. Valores base válidos; El nombre del estado no puede superar los 30 caracteres")
    })
    @Operation(summary = "Registrar estado de cita", description = "Agrega un nuevo estado al catálogo.\n\nRoles permitidos: ADMIN.")
    public ResponseEntity<EstadoCitaDTO> crear(@Valid @RequestBody EstadoCitaDTO dto) {
        EstadoCitaDTO creado = estadoCitaService.crear(dto);
        return new ResponseEntity<>(creado, HttpStatus.CREATED);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Actualizado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos del cuerpo inválidos: ver `detalles` con el mensaje de cada campo; Algún parámetro de la ruta o de la consulta tiene un formato inválido (por ejemplo un id no numérico o una fecha mal escrita)"),
            @ApiResponse(responseCode = "404", description = "No existe el estado de cita indicado"),
            @ApiResponse(responseCode = "409", description = "Ya existe otro estado con la descripción; Ya existe un estado de cita con el nombre"),
            @ApiResponse(responseCode = "422", description = "No se permite renombrar un estado base fijo del flujo; Descripción no permitida. Valores base válidos; El nombre del estado no puede superar los 30 caracteres")
    })
    @Operation(summary = "Actualizar estado de cita", description = "Modifica un estado de cita.\n\nRoles permitidos: ADMIN.")
    public ResponseEntity<EstadoCitaDTO> actualizar(
            @Parameter(description = "Id del registro en «Estados de cita» (debe existir).", example = "1") @PathVariable Long id,
            @Valid @RequestBody EstadoCitaDTO dto) {
        EstadoCitaDTO actualizado = estadoCitaService.actualizar(dto, id);
        return ResponseEntity.ok(actualizado);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Operación realizada sin contenido en la respuesta"),
            @ApiResponse(responseCode = "400", description = "Algún parámetro de la ruta o de la consulta tiene un formato inválido (por ejemplo un id no numérico o una fecha mal escrita)"),
            @ApiResponse(responseCode = "404", description = "No existe el estado de cita indicado"),
            @ApiResponse(responseCode = "422", description = "No se puede eliminar el estado base porque es requerido por el flujo del sistema")
    })
    @Operation(summary = "Eliminar estado de cita", description = "Elimina un estado. No se permite si hay citas que lo usan.\n\nRoles permitidos: ADMIN.")
    public ResponseEntity<Void> eliminar(@Parameter(description = "Id del registro en «Estados de cita» (debe existir).", example = "1") @PathVariable Long id) {
        estadoCitaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
