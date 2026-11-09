package com.example.hospital.tiposhabitacion;

import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import com.example.hospital.tiposhabitacion.dto.TiposHabitacionDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tipos-habitacion")
@Tag(name = "Tipos de habitación", description = "Catálogo de tipos de habitación (individual, compartida, UCI, etc.)")
@ApiResponses({
        @ApiResponse(responseCode = "401", description = "Token ausente, inválido o expirado"),
        @ApiResponse(responseCode = "403", description = "El rol del usuario no tiene permiso")
})
public class TiposHabitacionController {

    private final TiposHabitacionService tiposHabitacionService;

    public TiposHabitacionController(TiposHabitacionService tiposHabitacionService) {
        this.tiposHabitacionService = tiposHabitacionService;
    }

    @GetMapping
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Consulta exitosa")
    })
    @Operation(summary = "Listar tipos de habitación", description = "Devuelve el catálogo de tipos de habitación.\n\nAcceso: cualquier usuario autenticado.")
    public List<TiposHabitacionDTO> findAll() {
        return tiposHabitacionService.findAll();
    }

    @GetMapping("/{id}")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Consulta exitosa"),
            @ApiResponse(responseCode = "400", description = "Algún parámetro de la ruta o de la consulta tiene un formato inválido (por ejemplo un id no numérico o una fecha mal escrita)"),
            @ApiResponse(responseCode = "404", description = "No existe el tipo de habitación indicado")
    })
    @Operation(summary = "Obtener tipo de habitación por ID", description = "Devuelve el detalle de un tipo de habitación.\n\nAcceso: cualquier usuario autenticado.")
    public TiposHabitacionDTO findById(@Parameter(description = "Id del registro en «Tipos de habitación» (debe existir).", example = "1") @PathVariable Long id) {
        return tiposHabitacionService.findById(id);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Creado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos del cuerpo inválidos: ver `detalles` con el mensaje de cada campo"),
            @ApiResponse(responseCode = "409", description = "Ya existe un tipo de habitación con la descripción"),
            @ApiResponse(responseCode = "422", description = "La descripción del tipo de habitación es obligatoria; El costo por día debe ser un valor mayor a cero")
    })
    @Operation(summary = "Registrar tipo de habitación", description = "Agrega un tipo de habitación al catálogo.\n\nRoles permitidos: ADMIN.")
    public ResponseEntity<TiposHabitacionDTO> crear(@Valid @RequestBody TiposHabitacionDTO dto) {
        TiposHabitacionDTO creado = tiposHabitacionService.crear(dto);
        return new ResponseEntity<>(creado, HttpStatus.CREATED);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Actualizado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos del cuerpo inválidos: ver `detalles` con el mensaje de cada campo; Algún parámetro de la ruta o de la consulta tiene un formato inválido (por ejemplo un id no numérico o una fecha mal escrita)"),
            @ApiResponse(responseCode = "404", description = "No existe el tipo de habitación indicado"),
            @ApiResponse(responseCode = "409", description = "Ya existe un tipo de habitación con la descripción"),
            @ApiResponse(responseCode = "422", description = "No se permite renombrar el tipo de habitación base; La descripción del tipo de habitación es obligatoria; El costo por día debe ser un valor mayor a cero")
    })
    @Operation(summary = "Actualizar tipo de habitación", description = "Modifica un tipo de habitación.\n\nRoles permitidos: ADMIN.")
    public ResponseEntity<TiposHabitacionDTO> actualizar(
            @Parameter(description = "Id del registro en «Tipos de habitación» (debe existir).", example = "1") @PathVariable Long id,
            @Valid @RequestBody TiposHabitacionDTO dto) {
        TiposHabitacionDTO actualizado = tiposHabitacionService.actualizar(dto, id);
        return ResponseEntity.ok(actualizado);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Operación realizada sin contenido en la respuesta"),
            @ApiResponse(responseCode = "400", description = "Algún parámetro de la ruta o de la consulta tiene un formato inválido (por ejemplo un id no numérico o una fecha mal escrita)"),
            @ApiResponse(responseCode = "404", description = "No existe el tipo de habitación indicado"),
            @ApiResponse(responseCode = "409", description = "No se puede eliminar el tipo de habitación porque existen habitaciones asociadas a él"),
            @ApiResponse(responseCode = "422", description = "No se permite eliminar el tipo de habitación base")
    })
    @Operation(summary = "Eliminar tipo de habitación", description = "Elimina un tipo. No se permite si hay habitaciones de ese tipo.\n\nRoles permitidos: ADMIN.")
    public ResponseEntity<Void> eliminar(@Parameter(description = "Id del registro en «Tipos de habitación» (debe existir).", example = "1") @PathVariable Long id) {
        tiposHabitacionService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
