package com.example.hospital.procedimientosemergencia;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import com.example.hospital.procedimientosemergencia.dto.ProcedimientosEmergenciaDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/procedimientos-emergencia")
@Tag(name = "Procedimientos de emergencia", description = "Procedimientos realizados a pacientes en emergencia")
@ApiResponses({
        @ApiResponse(responseCode = "401", description = "Token ausente, inválido o expirado"),
        @ApiResponse(responseCode = "403", description = "El rol del usuario no tiene permiso")
})
public class ProcedimientosEmergenciaController {

    private final ProcedimientosEmergenciaService procedimientosEmergenciaService;

    public ProcedimientosEmergenciaController(ProcedimientosEmergenciaService procedimientosEmergenciaService) {
        this.procedimientosEmergenciaService = procedimientosEmergenciaService;
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO', 'ENFERMERO')")
    @GetMapping
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Consulta exitosa")
    })
    @Operation(summary = "Listar procedimientos", description = "Devuelve los procedimientos realizados en emergencia.\n\nRoles permitidos: ADMIN, MEDICO, ENFERMERO.")
    public List<ProcedimientosEmergenciaDTO> findAll() {
        return procedimientosEmergenciaService.findAll();
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO', 'ENFERMERO')")
    @GetMapping("/{id}")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Consulta exitosa"),
            @ApiResponse(responseCode = "400", description = "Algún parámetro de la ruta o de la consulta tiene un formato inválido (por ejemplo un id no numérico o una fecha mal escrita)"),
            @ApiResponse(responseCode = "404", description = "No existe el procedimiento indicado"),
            @ApiResponse(responseCode = "422", description = "Regla de negocio incumplida")
    })
    @Operation(summary = "Obtener procedimiento por ID", description = "Devuelve el detalle de un procedimiento de emergencia.\n\nRoles permitidos: ADMIN, MEDICO, ENFERMERO.")
    public ProcedimientosEmergenciaDTO findById(@Parameter(description = "Id del registro en «Procedimientos de emergencia» (debe existir).", example = "1") @PathVariable Long id) {
        return procedimientosEmergenciaService.findById(id);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO')")
    @PostMapping
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Creado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos del cuerpo inválidos: ver `detalles` con el mensaje de cada campo"),
            @ApiResponse(responseCode = "422", description = "No existe la emergencia indicada; No se pueden registrar procedimientos en una emergencia cerrada; El médico indicado no existe; El médico indicado se encuentra inactivo; La fecha del procedimiento no puede ser futura")
    })
    @Operation(summary = "Registrar procedimiento", description = "Registra un procedimiento realizado a un paciente en emergencia.\n\nRoles permitidos: ADMIN, MEDICO.")
    public ResponseEntity<ProcedimientosEmergenciaDTO> crear(@Valid @RequestBody ProcedimientosEmergenciaDTO dto) {
        return new ResponseEntity<>(procedimientosEmergenciaService.crear(dto), HttpStatus.CREATED);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO')")
    @PutMapping("/{id}")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Actualizado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos del cuerpo inválidos: ver `detalles` con el mensaje de cada campo; Algún parámetro de la ruta o de la consulta tiene un formato inválido (por ejemplo un id no numérico o una fecha mal escrita)"),
            @ApiResponse(responseCode = "404", description = "No existe el procedimiento indicado"),
            @ApiResponse(responseCode = "422", description = "No existe la emergencia indicada; No se pueden registrar procedimientos en una emergencia cerrada; El médico indicado no existe; El médico indicado se encuentra inactivo; La fecha del procedimiento no puede ser futura")
    })
    @Operation(summary = "Actualizar procedimiento", description = "Modifica un procedimiento de emergencia.\n\nRoles permitidos: ADMIN, MEDICO.")
    public ResponseEntity<ProcedimientosEmergenciaDTO> actualizar(@Parameter(description = "Id del registro en «Procedimientos de emergencia» (debe existir).", example = "1") @PathVariable Long id, @Valid @RequestBody ProcedimientosEmergenciaDTO dto) {
        return ResponseEntity.ok(procedimientosEmergenciaService.actualizar(dto, id));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Operación realizada sin contenido en la respuesta"),
            @ApiResponse(responseCode = "400", description = "Algún parámetro de la ruta o de la consulta tiene un formato inválido (por ejemplo un id no numérico o una fecha mal escrita)"),
            @ApiResponse(responseCode = "404", description = "No existe el procedimiento indicado"),
            @ApiResponse(responseCode = "422", description = "Regla de negocio incumplida")
    })
    @Operation(summary = "Eliminar procedimiento", description = "Desactiva un procedimiento de emergencia.\n\nRoles permitidos: ADMIN.")
    public ResponseEntity<Void> eliminar(@Parameter(description = "Id del registro en «Procedimientos de emergencia» (debe existir).", example = "1") @PathVariable Long id) {
        procedimientosEmergenciaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
