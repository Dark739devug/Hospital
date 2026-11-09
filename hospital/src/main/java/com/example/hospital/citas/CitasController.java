package com.example.hospital.citas;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import com.example.hospital.citas.dto.CitasDTO;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/citas")
@Tag(name = "Citas", description = "Programación y seguimiento de citas médicas")
@ApiResponses({
        @ApiResponse(responseCode = "401", description = "Token ausente, inválido o expirado"),
        @ApiResponse(responseCode = "403", description = "El rol del usuario no tiene permiso")
})
public class CitasController {

    private final CitasService citasService;

    public CitasController(CitasService citasService) {
        this.citasService = citasService;
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO', 'ENFERMERO', 'RECEPCIONISTA')")
    @GetMapping
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Consulta exitosa"),
            @ApiResponse(responseCode = "400", description = "Algún parámetro de la ruta o de la consulta tiene un formato inválido (por ejemplo un id no numérico o una fecha mal escrita)")
    })
    @Operation(summary = "Listar citas (con filtros)", description = "Devuelve las citas. Filtros opcionales y combinables (se aplican todos los que vengan):\n- `idPaciente`: citas de un paciente\n- `idMedico`: citas de un médico\n- `fecha` (yyyy-MM-dd): citas de un día\n- `soloActivas=true`: solo citas activas\n\nSin filtros devuelve todas.\n\nRoles permitidos: ADMIN, MEDICO, ENFERMERO, RECEPCIONISTA.")
    public List<CitasDTO> findAll(
            @Parameter(description = "Si es true devuelve solo los registros activos; por defecto false (todos).", example = "true") @RequestParam(required = false, defaultValue = "false") boolean soloActivas,
            @Parameter(description = "Id del paciente.", example = "1") @RequestParam(required = false) Long idPaciente,
            @Parameter(description = "Id del médico.", example = "1") @RequestParam(required = false) Long idMedico,
            @Parameter(description = "Fecha en formato yyyy-MM-dd.", example = "2026-03-15") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return citasService.buscar(idPaciente, idMedico, fecha, soloActivas);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO', 'ENFERMERO', 'RECEPCIONISTA')")
    @GetMapping("/agenda")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Consulta exitosa"),
            @ApiResponse(responseCode = "400", description = "Algún parámetro de la ruta o de la consulta tiene un formato inválido (por ejemplo un id no numérico o una fecha mal escrita)"),
            @ApiResponse(responseCode = "404", description = "No existe el médico indicado"),
            @ApiResponse(responseCode = "422", description = "Regla de negocio incumplida")
    })
    @Operation(summary = "Agenda diaria de un médico", description = "Devuelve las citas activas de un médico para un día, ordenadas por hora. Si no se envía `fecha`, usa el día de hoy. Responde 404 si el médico no existe.\n\nRoles permitidos: ADMIN, MEDICO, ENFERMERO, RECEPCIONISTA.")
    public List<CitasDTO> agendaMedico(
            @Parameter(description = "Id del médico.", example = "1") @RequestParam Long idMedico,
            @Parameter(description = "Fecha en formato yyyy-MM-dd.", example = "2026-03-15") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return citasService.agendaMedico(idMedico, fecha);
    }

    @GetMapping("/pendientes")
    @Operation(summary = "Agenda de citas pendientes", description = "**Nuevo.** Devuelve las citas activas que aún no se atienden (estado Programada, Confirmada o Reprogramada), ordenadas por fecha y hora. Filtros opcionales y combinables:\n- `idMedico`: solo las de un médico (404 si no existe)\n- `desde` / `hasta` (yyyy-MM-dd, inclusivos): rango de fechas. Sin `desde` también incluye las pendientes de días anteriores (vencidas).\n\nResponde 422 si `desde` es posterior a `hasta`.\n\nAcceso: cualquier usuario autenticado.")
    public List<CitasDTO> pendientes(
            @RequestParam(required = false) Long idMedico,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return citasService.pendientes(idMedico, desde, hasta);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO', 'ENFERMERO', 'RECEPCIONISTA')")
    @GetMapping("/{id}")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Consulta exitosa"),
            @ApiResponse(responseCode = "400", description = "Algún parámetro de la ruta o de la consulta tiene un formato inválido (por ejemplo un id no numérico o una fecha mal escrita)"),
            @ApiResponse(responseCode = "404", description = "No existe la cita médica indicada")
    })
    @Operation(summary = "Obtener cita por ID", description = "Devuelve el detalle de una cita. Responde 404 si no existe.\n\nRoles permitidos: ADMIN, MEDICO, ENFERMERO, RECEPCIONISTA.")
    public CitasDTO findById(@Parameter(description = "Id del registro en «Citas» (debe existir).", example = "1") @PathVariable Long id) {
        return citasService.findById(id);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA')")
    @PostMapping
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Creado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos del cuerpo inválidos: ver `detalles` con el mensaje de cada campo"),
            @ApiResponse(responseCode = "409", description = "El médico ya tiene asignada una cita activa en la fecha"),
            @ApiResponse(responseCode = "422", description = "La fecha de la cita no puede ser anterior a la fecha actual; El paciente indicado no existe; El paciente seleccionado se encuentra inactivo; El médico indicado no existe; El médico seleccionado se encuentra inactivo")
    })
    @Operation(summary = "Programar cita", description = "Crea una nueva cita para un paciente con un médico. Valida que paciente y médico existan y estén activos, y que el médico no tenga otra cita a la misma fecha y hora.\n\nRoles permitidos: ADMIN, RECEPCIONISTA.")
    public ResponseEntity<CitasDTO> save(@Valid @RequestBody CitasDTO dto) {
        CitasDTO creada = citasService.crear(dto);
        return new ResponseEntity<>(creada, HttpStatus.CREATED);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA')")
    @PutMapping("/{id}")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Actualizado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos del cuerpo inválidos: ver `detalles` con el mensaje de cada campo; Algún parámetro de la ruta o de la consulta tiene un formato inválido (por ejemplo un id no numérico o una fecha mal escrita)"),
            @ApiResponse(responseCode = "404", description = "No existe la cita médica indicada"),
            @ApiResponse(responseCode = "409", description = "El médico ya tiene asignada una cita activa en la fecha"),
            @ApiResponse(responseCode = "422", description = "La fecha de la cita no puede ser anterior a la fecha actual; El paciente indicado no existe; El paciente seleccionado se encuentra inactivo; El médico indicado no existe; El médico seleccionado se encuentra inactivo")
    })
    @Operation(summary = "Reprogramar / actualizar cita", description = "Modifica fecha, hora, motivo, médico o paciente de una cita, validando de nuevo choques de horario del médico.\n\nRoles permitidos: ADMIN, RECEPCIONISTA.")
    public ResponseEntity<CitasDTO> update(
            @Parameter(description = "Id del registro en «Citas» (debe existir).", example = "1") @PathVariable Long id,
            @Valid @RequestBody CitasDTO dto) {
        CitasDTO actualizada = citasService.actualizar(dto, id);
        return ResponseEntity.ok(actualizada);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA', 'MEDICO')")
    @PatchMapping("/{id}/estado/{nuevoIdEstado}")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Actualizado correctamente"),
            @ApiResponse(responseCode = "400", description = "Algún parámetro de la ruta o de la consulta tiene un formato inválido (por ejemplo un id no numérico o una fecha mal escrita)"),
            @ApiResponse(responseCode = "404", description = "No existe la cita médica indicada; Estado actual de la cita no encontrado; El nuevo estado indicado no existe"),
            @ApiResponse(responseCode = "422", description = "Una cita en estado es final y no puede cambiar de estado; Transición de estado no permitida")
    })
    @Operation(summary = "Cambiar estado de la cita", description = "Mueve la cita a otro estado (p. ej. Programada → Confirmada → Atendida, o Cancelada). Solo permite transiciones válidas según las reglas de `estado-cita`.\n\nRoles permitidos: ADMIN, RECEPCIONISTA, MEDICO.")
    public ResponseEntity<CitasDTO> cambiarEstado(
            @Parameter(description = "Id del registro en «Citas» (debe existir).", example = "1") @PathVariable Long id,
            @Parameter(description = "Id del estado de cita al que se pasa.", example = "2") @PathVariable Long nuevoIdEstado) {
        CitasDTO actualizada = citasService.cambiarEstado(id, nuevoIdEstado);
        return ResponseEntity.ok(actualizada);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Operación realizada sin contenido en la respuesta"),
            @ApiResponse(responseCode = "400", description = "Algún parámetro de la ruta o de la consulta tiene un formato inválido (por ejemplo un id no numérico o una fecha mal escrita)"),
            @ApiResponse(responseCode = "404", description = "No existe la cita médica indicada"),
            @ApiResponse(responseCode = "409", description = "No se puede eliminar la cita médica porque ya cuenta con una consulta médica registrada"),
            @ApiResponse(responseCode = "422", description = "Regla de negocio incumplida")
    })
    @Operation(summary = "Eliminar cita", description = "Desactiva una cita (borrado lógico).\n\nRoles permitidos: ADMIN.")
    public ResponseEntity<Void> delete(@Parameter(description = "Id del registro en «Citas» (debe existir).", example = "1") @PathVariable Long id) {
        citasService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
