package com.example.hospital.consultas;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import com.example.hospital.consultas.dto.ConsultasDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/consultas")
@Tag(name = "Consultas", description = "Registro de la atención médica (signos vitales y síntomas) derivada de una cita")
@ApiResponses({
        @ApiResponse(responseCode = "401", description = "Token ausente, inválido o expirado"),
        @ApiResponse(responseCode = "403", description = "El rol del usuario no tiene permiso")
})
public class ConsultasController {

    private final ConsultasService consultasService;

    public ConsultasController(ConsultasService consultasService) {
        this.consultasService = consultasService;
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO', 'ENFERMERO')")
    @GetMapping
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Consulta exitosa"),
            @ApiResponse(responseCode = "400", description = "Algún parámetro de la ruta o de la consulta tiene un formato inválido (por ejemplo un id no numérico o una fecha mal escrita)"),
            @ApiResponse(responseCode = "422", description = "Regla de negocio incumplida")
    })
    @Operation(summary = "Listar consultas", description = "Devuelve las consultas médicas. Filtros opcionales:\n- `idCita`: la consulta asociada a esa cita\n- `soloActivas=true`: solo consultas activas\n\nRoles permitidos: ADMIN, MEDICO, ENFERMERO.")
    public List<ConsultasDTO> findAll(
            @Parameter(description = "Si es true devuelve solo los registros activos; por defecto false (todos).", example = "true") @RequestParam(required = false, defaultValue = "false") boolean soloActivas,
            @Parameter(description = "Id de la cita.", example = "1") @RequestParam(required = false) Long idCita) {
        if (idCita != null) {
            return consultasService.listarPorCita(idCita);
        }
        if (soloActivas) {
            return consultasService.findActivas();
        }
        return consultasService.findAll();
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO', 'ENFERMERO')")
    @GetMapping("/{id}")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Consulta exitosa"),
            @ApiResponse(responseCode = "400", description = "Algún parámetro de la ruta o de la consulta tiene un formato inválido (por ejemplo un id no numérico o una fecha mal escrita)"),
            @ApiResponse(responseCode = "404", description = "No existe la consulta médica indicada")
    })
    @Operation(summary = "Obtener consulta por ID", description = "Devuelve el detalle de una consulta (síntomas, peso, talla, temperatura, presión, frecuencia cardíaca). Responde 404 si no existe.\n\nRoles permitidos: ADMIN, MEDICO, ENFERMERO.")
    public ConsultasDTO findById(@Parameter(description = "Id del registro en «Consultas» (debe existir).", example = "1") @PathVariable Long id) {
        return consultasService.findById(id);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO')")
    @PostMapping
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Creado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos del cuerpo inválidos: ver `detalles` con el mensaje de cada campo"),
            @ApiResponse(responseCode = "409", description = "Ya existe una consulta médica registrada para la cita ID"),
            @ApiResponse(responseCode = "422", description = "Solo se puede registrar la consulta de una cita Confirmada o En curso (la cita está ); No se puede registrar la consulta de una cita con fecha futura; La cita médica indicada no existe; La cita médica asociada se encuentra inactiva o cancelada; Falta el estado en el catálogo de estados de cita")
    })
    @Operation(summary = "Registrar consulta", description = "Registra la atención de una cita: síntomas y signos vitales. Una cita solo puede tener una consulta.\n\nRoles permitidos: ADMIN, MEDICO.")
    public ResponseEntity<ConsultasDTO> save(@Valid @RequestBody ConsultasDTO dto) {
        ConsultasDTO creada = consultasService.crear(dto);
        return new ResponseEntity<>(creada, HttpStatus.CREATED);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO')")
    @PutMapping("/{id}")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Actualizado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos del cuerpo inválidos: ver `detalles` con el mensaje de cada campo; Algún parámetro de la ruta o de la consulta tiene un formato inválido (por ejemplo un id no numérico o una fecha mal escrita)"),
            @ApiResponse(responseCode = "404", description = "No existe la consulta médica indicada"),
            @ApiResponse(responseCode = "409", description = "Ya existe una consulta médica registrada para la cita ID"),
            @ApiResponse(responseCode = "422", description = "La consulta tiene más de 24 horas y ya no se puede modificar; La cita médica indicada no existe; La cita médica asociada se encuentra inactiva o cancelada")
    })
    @Operation(summary = "Actualizar consulta", description = "Corrige los datos registrados en una consulta. Solo durante las 24 horas siguientes a su atención; la fecha/hora no se modifica.\n\nRoles permitidos: ADMIN, MEDICO.")
    public ResponseEntity<ConsultasDTO> update(
            @Parameter(description = "Id del registro en «Consultas» (debe existir).", example = "1") @PathVariable Long id,
            @Valid @RequestBody ConsultasDTO dto) {
        ConsultasDTO actualizada = consultasService.actualizar(dto, id);
        return ResponseEntity.ok(actualizada);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Operación realizada sin contenido en la respuesta"),
            @ApiResponse(responseCode = "400", description = "Algún parámetro de la ruta o de la consulta tiene un formato inválido (por ejemplo un id no numérico o una fecha mal escrita)"),
            @ApiResponse(responseCode = "404", description = "No existe la consulta médica indicada"),
            @ApiResponse(responseCode = "409", description = "No se puede eliminar la consulta médica porque tiene diagnósticos clínicos asociados"),
            @ApiResponse(responseCode = "422", description = "Regla de negocio incumplida")
    })
    @Operation(summary = "Eliminar consulta", description = "Desactiva una consulta (borrado lógico).\n\nRoles permitidos: ADMIN.")
    public ResponseEntity<Void> delete(@Parameter(description = "Id del registro en «Consultas» (debe existir).", example = "1") @PathVariable Long id) {
        consultasService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
