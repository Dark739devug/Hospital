package com.example.hospital.estadoshospitalizacion;

import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import com.example.hospital.estadoshospitalizacion.dto.EstadosHospitalizacionDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/estados-hospitalizacion")
@Tag(name = "Estados de hospitalización", description = "Catálogo de estados de hospitalización (Activa, Alta, etc.)")
@ApiResponses({
        @ApiResponse(responseCode = "401", description = "Token ausente, inválido o expirado"),
        @ApiResponse(responseCode = "403", description = "El rol del usuario no tiene permiso")
})
public class EstadosHospitalizacionController {

    private final EstadosHospitalizacionService estadosHospitalizacionService;

    public EstadosHospitalizacionController(EstadosHospitalizacionService estadosHospitalizacionService) {
        this.estadosHospitalizacionService = estadosHospitalizacionService;
    }

    @GetMapping
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Consulta exitosa"),
            @ApiResponse(responseCode = "400", description = "Algún parámetro de la ruta o de la consulta tiene un formato inválido (por ejemplo un id no numérico o una fecha mal escrita)")
    })
    @Operation(summary = "Listar estados de hospitalización", description = "Devuelve los estados de hospitalización. Con `soloActivos=true` solo los activos.\n\nAcceso: cualquier usuario autenticado.")
    public List<EstadosHospitalizacionDTO> findAll(
            @Parameter(description = "Si es true devuelve solo los registros activos; por defecto false (todos).", example = "true") @RequestParam(required = false, defaultValue = "false") boolean soloActivos) {
        if (soloActivos) {
            return estadosHospitalizacionService.findActivos();
        }
        return estadosHospitalizacionService.findAll();
    }

    @GetMapping("/{id}")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Consulta exitosa"),
            @ApiResponse(responseCode = "400", description = "Algún parámetro de la ruta o de la consulta tiene un formato inválido (por ejemplo un id no numérico o una fecha mal escrita)"),
            @ApiResponse(responseCode = "404", description = "No existe el estado de hospitalización indicado")
    })
    @Operation(summary = "Obtener estado por ID", description = "Devuelve el detalle de un estado de hospitalización.\n\nAcceso: cualquier usuario autenticado.")
    public EstadosHospitalizacionDTO findById(@Parameter(description = "Id del registro en «Estados de hospitalización» (debe existir).", example = "1") @PathVariable Long id) {
        return estadosHospitalizacionService.findById(id);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Creado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos del cuerpo inválidos: ver `detalles` con el mensaje de cada campo"),
            @ApiResponse(responseCode = "409", description = "Ya existe un estado de hospitalización con la descripción; Ya existe otro estado de hospitalización con la descripción"),
            @ApiResponse(responseCode = "422", description = "La descripción del estado de hospitalización es obligatoria; La descripción no puede exceder los 30 caracteres")
    })
    @Operation(summary = "Registrar estado de hospitalización", description = "Agrega un nuevo estado al catálogo.\n\nRoles permitidos: ADMIN.")
    public ResponseEntity<EstadosHospitalizacionDTO> save(@Valid @RequestBody EstadosHospitalizacionDTO dto) {
        EstadosHospitalizacionDTO creado = estadosHospitalizacionService.crear(dto);
        return new ResponseEntity<>(creado, HttpStatus.CREATED);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Actualizado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos del cuerpo inválidos: ver `detalles` con el mensaje de cada campo; Algún parámetro de la ruta o de la consulta tiene un formato inválido (por ejemplo un id no numérico o una fecha mal escrita)"),
            @ApiResponse(responseCode = "404", description = "No existe el estado de hospitalización indicado"),
            @ApiResponse(responseCode = "409", description = "Ya existe un estado de hospitalización con la descripción; Ya existe otro estado de hospitalización con la descripción"),
            @ApiResponse(responseCode = "422", description = "No se permite modificar la descripción esencial de los estados base del sistema hospitalario: Activa, Alta, Traslado, Fallecimiento; No se permite desactivar un estado base esencial del sistema hospitalario; La descripción del estado de hospitalización es obligatoria; La descripción no puede exceder los 30 caracteres")
    })
    @Operation(summary = "Actualizar estado de hospitalización", description = "Modifica un estado de hospitalización.\n\nRoles permitidos: ADMIN.")
    public ResponseEntity<EstadosHospitalizacionDTO> update(
            @Parameter(description = "Id del registro en «Estados de hospitalización» (debe existir).", example = "1") @PathVariable Long id,
            @Valid @RequestBody EstadosHospitalizacionDTO dto) {
        EstadosHospitalizacionDTO actualizado = estadosHospitalizacionService.actualizar(dto, id);
        return ResponseEntity.ok(actualizado);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Operación realizada sin contenido en la respuesta"),
            @ApiResponse(responseCode = "400", description = "Algún parámetro de la ruta o de la consulta tiene un formato inválido (por ejemplo un id no numérico o una fecha mal escrita)"),
            @ApiResponse(responseCode = "404", description = "No existe el estado de hospitalización indicado"),
            @ApiResponse(responseCode = "409", description = "No se puede eliminar el estado de hospitalización porque existen pacientes u hospitalizaciones asociadas"),
            @ApiResponse(responseCode = "422", description = "No está permitido eliminar los estados base esenciales del sistema hospitalario")
    })
    @Operation(summary = "Eliminar estado de hospitalización", description = "Desactiva un estado. No se permite si hay hospitalizaciones que lo usan.\n\nRoles permitidos: ADMIN.")
    public ResponseEntity<Void> delete(@Parameter(description = "Id del registro en «Estados de hospitalización» (debe existir).", example = "1") @PathVariable Long id) {
        estadosHospitalizacionService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
