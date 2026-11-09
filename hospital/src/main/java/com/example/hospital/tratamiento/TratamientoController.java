package com.example.hospital.tratamiento;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import com.example.hospital.tratamiento.dto.TratamientoDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tratamiento")
@Tag(name = "Tratamientos", description = "Tratamientos indicados a partir de un diagnóstico")
@ApiResponses({
        @ApiResponse(responseCode = "401", description = "Token ausente, inválido o expirado"),
        @ApiResponse(responseCode = "403", description = "El rol del usuario no tiene permiso")
})
public class TratamientoController {

    private final TratamientoService tratamientoService;

    public TratamientoController(TratamientoService tratamientoService) {
        this.tratamientoService = tratamientoService;
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO', 'ENFERMERO', 'FARMACEUTICO')")
    @GetMapping
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Consulta exitosa"),
            @ApiResponse(responseCode = "400", description = "Algún parámetro de la ruta o de la consulta tiene un formato inválido (por ejemplo un id no numérico o una fecha mal escrita)")
    })
    @Operation(summary = "Listar tratamientos", description = "Devuelve los tratamientos. Filtros opcionales:\n- `idDiagnostico`: tratamientos de un diagnóstico\n- `soloActivos=true`: solo activos\n\nRoles permitidos: ADMIN, MEDICO, ENFERMERO, FARMACEUTICO.")
    public List<TratamientoDTO> findAll(
            @Parameter(description = "Si es true devuelve solo los registros activos; por defecto false (todos).", example = "true") @RequestParam(required = false, defaultValue = "false") boolean soloActivos,
            @Parameter(description = "Id del diagnóstico.", example = "1") @RequestParam(required = false) Long idDiagnostico) {
        return tratamientoService.buscar(idDiagnostico, soloActivos);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO', 'ENFERMERO', 'FARMACEUTICO')")
    @GetMapping("/{id}")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Consulta exitosa"),
            @ApiResponse(responseCode = "400", description = "Algún parámetro de la ruta o de la consulta tiene un formato inválido (por ejemplo un id no numérico o una fecha mal escrita)"),
            @ApiResponse(responseCode = "404", description = "No existe el tratamiento indicado"),
            @ApiResponse(responseCode = "422", description = "Regla de negocio incumplida")
    })
    @Operation(summary = "Obtener tratamiento por ID", description = "Devuelve el detalle de un tratamiento.\n\nRoles permitidos: ADMIN, MEDICO, ENFERMERO, FARMACEUTICO.")
    public TratamientoDTO findById(@Parameter(description = "Id del registro en «Tratamientos» (debe existir).", example = "1") @PathVariable Long id) {
        return tratamientoService.findById(id);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO')")
    @PostMapping
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Creado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos del cuerpo inválidos: ver `detalles` con el mensaje de cada campo"),
            @ApiResponse(responseCode = "404", description = "El diagnóstico indicado no existe"),
            @ApiResponse(responseCode = "422", description = "El diagnóstico indicado se encuentra inactivo; La fecha de fin no puede ser anterior a la fecha de inicio del tratamiento")
    })
    @Operation(summary = "Registrar tratamiento", description = "Registra un tratamiento para un diagnóstico.\n\nRoles permitidos: ADMIN, MEDICO.")
    public ResponseEntity<TratamientoDTO> save(@Valid @RequestBody TratamientoDTO dto) {
        TratamientoDTO creado = tratamientoService.crear(dto);
        return new ResponseEntity<>(creado, HttpStatus.CREATED);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO')")
    @PutMapping("/{id}")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Actualizado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos del cuerpo inválidos: ver `detalles` con el mensaje de cada campo; Algún parámetro de la ruta o de la consulta tiene un formato inválido (por ejemplo un id no numérico o una fecha mal escrita)"),
            @ApiResponse(responseCode = "404", description = "No existe el tratamiento indicado; El diagnóstico indicado no existe"),
            @ApiResponse(responseCode = "422", description = "El diagnóstico indicado se encuentra inactivo; La fecha de fin no puede ser anterior a la fecha de inicio del tratamiento")
    })
    @Operation(summary = "Actualizar tratamiento", description = "Modifica un tratamiento.\n\nRoles permitidos: ADMIN, MEDICO.")
    public ResponseEntity<TratamientoDTO> update(
            @Parameter(description = "Id del registro en «Tratamientos» (debe existir).", example = "1") @PathVariable Long id,
            @Valid @RequestBody TratamientoDTO dto) {
        TratamientoDTO actualizado = tratamientoService.actualizar(dto, id);
        return ResponseEntity.ok(actualizado);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Operación realizada sin contenido en la respuesta"),
            @ApiResponse(responseCode = "400", description = "Algún parámetro de la ruta o de la consulta tiene un formato inválido (por ejemplo un id no numérico o una fecha mal escrita)"),
            @ApiResponse(responseCode = "404", description = "No existe el tratamiento indicado"),
            @ApiResponse(responseCode = "422", description = "No se puede eliminar el tratamiento porque tiene medicamentos asociados")
    })
    @Operation(summary = "Eliminar tratamiento", description = "Desactiva un tratamiento (borrado lógico).\n\nRoles permitidos: ADMIN.")
    public ResponseEntity<Void> delete(@Parameter(description = "Id del registro en «Tratamientos» (debe existir).", example = "1") @PathVariable Long id) {
        tratamientoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
