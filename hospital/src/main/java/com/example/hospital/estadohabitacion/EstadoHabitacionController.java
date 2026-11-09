package com.example.hospital.estadohabitacion;

import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import com.example.hospital.estadohabitacion.dto.EstadoHabitacionDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/estado-habitacion")
@Tag(name = "Estados de habitación", description = "Catálogo de estados de habitación (Disponible, Ocupada, Mantenimiento)")
@ApiResponses({
        @ApiResponse(responseCode = "401", description = "Token ausente, inválido o expirado"),
        @ApiResponse(responseCode = "403", description = "El rol del usuario no tiene permiso")
})
public class EstadoHabitacionController {

    private final EstadoHabitacionService estadoHabitacionService;

    public EstadoHabitacionController(EstadoHabitacionService estadoHabitacionService) {
        this.estadoHabitacionService = estadoHabitacionService;
    }

    // Consulta de libre lectura para usuarios autenticados
    @GetMapping
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Consulta exitosa")
    })
    @Operation(summary = "Listar estados de habitación", description = "Devuelve los estados posibles de una habitación.\n\nAcceso: cualquier usuario autenticado.")
    public List<EstadoHabitacionDTO> findAll() {
        return estadoHabitacionService.findAll();
    }

    @GetMapping("/{id}")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Consulta exitosa"),
            @ApiResponse(responseCode = "400", description = "Algún parámetro de la ruta o de la consulta tiene un formato inválido (por ejemplo un id no numérico o una fecha mal escrita)"),
            @ApiResponse(responseCode = "404", description = "No existe el estado de habitación indicado")
    })
    @Operation(summary = "Obtener estado por ID", description = "Devuelve el detalle de un estado de habitación.\n\nAcceso: cualquier usuario autenticado.")
    public EstadoHabitacionDTO findById(@Parameter(description = "Id del registro en «Estados de habitación» (debe existir).", example = "1") @PathVariable Long id) {
        return estadoHabitacionService.findById(id);
    }

    // Solo ADMIN puede modificar catálogos base
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Creado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos del cuerpo inválidos: ver `detalles` con el mensaje de cada campo"),
            @ApiResponse(responseCode = "409", description = "Ya existe un estado de habitación con la descripción; Ya existe un estado de habitación con el nombre"),
            @ApiResponse(responseCode = "422", description = "La descripción del estado de habitación es obligatoria; El nombre del estado no puede superar los 30 caracteres")
    })
    @Operation(summary = "Registrar estado de habitación", description = "Agrega un nuevo estado al catálogo.\n\nRoles permitidos: ADMIN.")
    public ResponseEntity<EstadoHabitacionDTO> crear(@Valid @RequestBody EstadoHabitacionDTO dto) {
        EstadoHabitacionDTO creado = estadoHabitacionService.crear(dto);
        return new ResponseEntity<>(creado, HttpStatus.CREATED);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Actualizado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos del cuerpo inválidos: ver `detalles` con el mensaje de cada campo; Algún parámetro de la ruta o de la consulta tiene un formato inválido (por ejemplo un id no numérico o una fecha mal escrita)"),
            @ApiResponse(responseCode = "404", description = "No existe el estado de habitación indicado"),
            @ApiResponse(responseCode = "409", description = "Ya existe otro estado de habitación con la descripción; Ya existe un estado de habitación con el nombre"),
            @ApiResponse(responseCode = "422", description = "No se permite renombrar el estado base fijo; El nombre del estado no puede superar los 30 caracteres")
    })
    @Operation(summary = "Actualizar estado de habitación", description = "Modifica un estado de habitación.\n\nRoles permitidos: ADMIN.")
    public ResponseEntity<EstadoHabitacionDTO> actualizar(
            @Parameter(description = "Id del registro en «Estados de habitación» (debe existir).", example = "1") @PathVariable Long id,
            @Valid @RequestBody EstadoHabitacionDTO dto) {
        EstadoHabitacionDTO actualizado = estadoHabitacionService.actualizar(dto, id);
        return ResponseEntity.ok(actualizado);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Operación realizada sin contenido en la respuesta"),
            @ApiResponse(responseCode = "400", description = "Algún parámetro de la ruta o de la consulta tiene un formato inválido (por ejemplo un id no numérico o una fecha mal escrita)"),
            @ApiResponse(responseCode = "404", description = "No existe el estado de habitación indicado"),
            @ApiResponse(responseCode = "409", description = "No se puede eliminar el estado porque se encuentra asignado a una o más habitaciones"),
            @ApiResponse(responseCode = "422", description = "No se puede eliminar el estado base requerido por la lógica del sistema")
    })
    @Operation(summary = "Eliminar estado de habitación", description = "Elimina un estado. No se permite si hay habitaciones que lo usan.\n\nRoles permitidos: ADMIN.")
    public ResponseEntity<Void> eliminar(@Parameter(description = "Id del registro en «Estados de habitación» (debe existir).", example = "1") @PathVariable Long id) {
        estadoHabitacionService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
