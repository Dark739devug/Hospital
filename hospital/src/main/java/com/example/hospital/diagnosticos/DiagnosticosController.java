package com.example.hospital.diagnosticos;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import com.example.hospital.diagnosticos.dto.DiagnosticosDTO;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/diagnosticos")
@Tag(name = "Diagnósticos", description = "Diagnósticos médicos emitidos")
@ApiResponses({
        @ApiResponse(responseCode = "401", description = "Token ausente, inválido o expirado"),
        @ApiResponse(responseCode = "403", description = "El rol del usuario no tiene permiso")
})
public class DiagnosticosController {

    private final DiagnosticosService diagnosticosService;

    public DiagnosticosController(DiagnosticosService diagnosticosService) {
        this.diagnosticosService = diagnosticosService;
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO', 'ENFERMERO')")
    @GetMapping
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Consulta exitosa"),
            @ApiResponse(responseCode = "400", description = "Algún parámetro de la ruta o de la consulta tiene un formato inválido (por ejemplo un id no numérico o una fecha mal escrita)")
    })
    @Operation(summary = "Listar diagnósticos", description = "Devuelve los diagnósticos. Filtros opcionales:\n- `idTipoDiagnostico`: por tipo\n- `fecha` (yyyy-MM-dd): emitidos ese día\n- `soloActivos=true`: solo activos\n\nRoles permitidos: ADMIN, MEDICO, ENFERMERO.")
    public List<DiagnosticosDTO> findAll(
            @Parameter(description = "Si es true devuelve solo los registros activos; por defecto false (todos).", example = "true") @RequestParam(required = false, defaultValue = "false") boolean soloActivos,
            @Parameter(description = "Id del tipo de diagnóstico.", example = "2") @RequestParam(required = false) Long idTipoDiagnostico,
            @Parameter(description = "Fecha en formato yyyy-MM-dd.", example = "2026-03-15") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return diagnosticosService.buscar(idTipoDiagnostico, fecha, soloActivos);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO', 'ENFERMERO')")
    @GetMapping("/{id}")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Consulta exitosa"),
            @ApiResponse(responseCode = "400", description = "Algún parámetro de la ruta o de la consulta tiene un formato inválido (por ejemplo un id no numérico o una fecha mal escrita)"),
            @ApiResponse(responseCode = "404", description = "No existe el diagnóstico indicado")
    })
    @Operation(summary = "Obtener diagnóstico por ID", description = "Devuelve el detalle de un diagnóstico. Responde 404 si no existe.\n\nRoles permitidos: ADMIN, MEDICO, ENFERMERO.")
    public DiagnosticosDTO findById(@Parameter(description = "Id del registro en «Diagnósticos» (debe existir).", example = "1") @PathVariable Long id) {
        return diagnosticosService.findById(id);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO')")
    @PostMapping
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Creado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos del cuerpo inválidos: ver `detalles` con el mensaje de cada campo"),
            @ApiResponse(responseCode = "422", description = "El tipo de diagnóstico indicado no existe; El tipo de diagnóstico se encuentra inactivo")
    })
    @Operation(summary = "Registrar diagnóstico", description = "Registra un nuevo diagnóstico médico.\n\nRoles permitidos: ADMIN, MEDICO.")
    public ResponseEntity<DiagnosticosDTO> save(@Valid @RequestBody DiagnosticosDTO dto) {
        DiagnosticosDTO creado = diagnosticosService.crear(dto);
        return new ResponseEntity<>(creado, HttpStatus.CREATED);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO')")
    @PutMapping("/{id}")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Actualizado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos del cuerpo inválidos: ver `detalles` con el mensaje de cada campo; Algún parámetro de la ruta o de la consulta tiene un formato inválido (por ejemplo un id no numérico o una fecha mal escrita)"),
            @ApiResponse(responseCode = "404", description = "No existe el diagnóstico indicado"),
            @ApiResponse(responseCode = "422", description = "El tipo de diagnóstico indicado no existe; El tipo de diagnóstico se encuentra inactivo")
    })
    @Operation(summary = "Actualizar diagnóstico", description = "Modifica la información de un diagnóstico.\n\nRoles permitidos: ADMIN, MEDICO.")
    public ResponseEntity<DiagnosticosDTO> update(
            @Parameter(description = "Id del registro en «Diagnósticos» (debe existir).", example = "1") @PathVariable Long id,
            @Valid @RequestBody DiagnosticosDTO dto) {
        DiagnosticosDTO actualizado = diagnosticosService.actualizar(dto, id);
        return ResponseEntity.ok(actualizado);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Operación realizada sin contenido en la respuesta"),
            @ApiResponse(responseCode = "400", description = "Algún parámetro de la ruta o de la consulta tiene un formato inválido (por ejemplo un id no numérico o una fecha mal escrita)"),
            @ApiResponse(responseCode = "404", description = "No existe el diagnóstico indicado"),
            @ApiResponse(responseCode = "409", description = "No se puede eliminar el diagnóstico porque se encuentra vinculado a una consulta médica; No se puede eliminar el diagnóstico porque tiene tratamientos médicos registrados"),
            @ApiResponse(responseCode = "422", description = "Regla de negocio incumplida")
    })
    @Operation(summary = "Eliminar diagnóstico", description = "Desactiva un diagnóstico (borrado lógico).\n\nRoles permitidos: ADMIN.")
    public ResponseEntity<Void> delete(@Parameter(description = "Id del registro en «Diagnósticos» (debe existir).", example = "1") @PathVariable Long id) {
        diagnosticosService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
