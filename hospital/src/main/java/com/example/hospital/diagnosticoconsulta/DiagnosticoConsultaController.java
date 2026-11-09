package com.example.hospital.diagnosticoconsulta;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import com.example.hospital.diagnosticoconsulta.dto.DiagnosticoConsultaDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/diagnostico-consulta")
@Tag(name = "Diagnóstico por consulta", description = "Relación entre consultas y los diagnósticos emitidos en ellas")
@ApiResponses({
        @ApiResponse(responseCode = "401", description = "Token ausente, inválido o expirado"),
        @ApiResponse(responseCode = "403", description = "El rol del usuario no tiene permiso")
})
public class DiagnosticoConsultaController {

    private final DiagnosticoConsultaService diagnosticoConsultaService;

    public DiagnosticoConsultaController(DiagnosticoConsultaService diagnosticoConsultaService) {
        this.diagnosticoConsultaService = diagnosticoConsultaService;
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO', 'ENFERMERO')")
    @GetMapping
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Consulta exitosa")
    })
    @Operation(summary = "Listar relaciones", description = "Devuelve todas las relaciones diagnóstico–consulta.\n\nRoles permitidos: ADMIN, MEDICO, ENFERMERO.")
    public List<DiagnosticoConsultaDTO> findAll() {
        return diagnosticoConsultaService.findAll();
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO', 'ENFERMERO')")
    @GetMapping("/consulta/{idConsulta}")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Consulta exitosa"),
            @ApiResponse(responseCode = "400", description = "Algún parámetro de la ruta o de la consulta tiene un formato inválido (por ejemplo un id no numérico o una fecha mal escrita)"),
            @ApiResponse(responseCode = "422", description = "Regla de negocio incumplida")
    })
    @Operation(summary = "Diagnósticos de una consulta", description = "Devuelve los diagnósticos emitidos en una consulta.\n\nRoles permitidos: ADMIN, MEDICO, ENFERMERO.")
    public List<DiagnosticoConsultaDTO> findByConsulta(@Parameter(description = "Id de la consulta.", example = "1") @PathVariable Long idConsulta) {
        return diagnosticoConsultaService.findByConsulta(idConsulta);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO', 'ENFERMERO')")
    @GetMapping("/diagnostico/{idDiagnostico}")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Consulta exitosa"),
            @ApiResponse(responseCode = "400", description = "Algún parámetro de la ruta o de la consulta tiene un formato inválido (por ejemplo un id no numérico o una fecha mal escrita)"),
            @ApiResponse(responseCode = "422", description = "Regla de negocio incumplida")
    })
    @Operation(summary = "Consultas con un diagnóstico", description = "Devuelve las consultas en las que se emitió un diagnóstico.\n\nRoles permitidos: ADMIN, MEDICO, ENFERMERO.")
    public List<DiagnosticoConsultaDTO> findByDiagnostico(@Parameter(description = "Id del diagnóstico.", example = "1") @PathVariable Long idDiagnostico) {
        return diagnosticoConsultaService.findByDiagnostico(idDiagnostico);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO', 'ENFERMERO')")
    @GetMapping("/{idDiagnostico}/{idConsulta}")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Consulta exitosa"),
            @ApiResponse(responseCode = "400", description = "Algún parámetro de la ruta o de la consulta tiene un formato inválido (por ejemplo un id no numérico o una fecha mal escrita)"),
            @ApiResponse(responseCode = "404", description = "No existe la asociación entre diagnóstico y consulta"),
            @ApiResponse(responseCode = "422", description = "Regla de negocio incumplida")
    })
    @Operation(summary = "Obtener relación", description = "Devuelve una relación específica a partir de su llave compuesta (idDiagnostico + idConsulta).\n\nRoles permitidos: ADMIN, MEDICO, ENFERMERO.")
    public DiagnosticoConsultaDTO findById(
            @Parameter(description = "Id del diagnóstico.", example = "1") @PathVariable Long idDiagnostico,
            @Parameter(description = "Id de la consulta.", example = "1") @PathVariable Long idConsulta) {
        return diagnosticoConsultaService.findById(idDiagnostico, idConsulta);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO')")
    @PostMapping
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Creado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos del cuerpo inválidos: ver `detalles` con el mensaje de cada campo"),
            @ApiResponse(responseCode = "409", description = "El diagnóstico ya fue asignado previamente a esta consulta"),
            @ApiResponse(responseCode = "422", description = "La consulta indicada no existe; La consulta médica seleccionada se encuentra inactiva; El diagnóstico indicado no existe; El diagnóstico médico seleccionado se encuentra inactivo")
    })
    @Operation(summary = "Asociar diagnóstico a consulta", description = "Vincula un diagnóstico a una consulta. No permite duplicados.\n\nRoles permitidos: ADMIN, MEDICO.")
    public ResponseEntity<DiagnosticoConsultaDTO> save(@Valid @RequestBody DiagnosticoConsultaDTO dto) {
        DiagnosticoConsultaDTO creado = diagnosticoConsultaService.crear(dto);
        return new ResponseEntity<>(creado, HttpStatus.CREATED);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO')")
    @DeleteMapping("/{idDiagnostico}/{idConsulta}")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Operación realizada sin contenido en la respuesta"),
            @ApiResponse(responseCode = "400", description = "Algún parámetro de la ruta o de la consulta tiene un formato inválido (por ejemplo un id no numérico o una fecha mal escrita)"),
            @ApiResponse(responseCode = "404", description = "No existe la asociación de diagnóstico para la consulta"),
            @ApiResponse(responseCode = "422", description = "Regla de negocio incumplida")
    })
    @Operation(summary = "Quitar diagnóstico de consulta", description = "Elimina el vínculo entre un diagnóstico y una consulta.\n\nRoles permitidos: ADMIN, MEDICO.")
    public ResponseEntity<Void> delete(
            @Parameter(description = "Id del diagnóstico.", example = "1") @PathVariable Long idDiagnostico,
            @Parameter(description = "Id de la consulta.", example = "1") @PathVariable Long idConsulta) {
        diagnosticoConsultaService.eliminar(idDiagnostico, idConsulta);
        return ResponseEntity.noContent().build();
    }
}
