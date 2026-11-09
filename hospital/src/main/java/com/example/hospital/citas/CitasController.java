package com.example.hospital.citas;

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

    @GetMapping
    @Operation(summary = "Listar citas (con filtros)", description = "Devuelve las citas. Filtros opcionales y combinables (se aplican todos los que vengan):\n- `idPaciente`: citas de un paciente\n- `idMedico`: citas de un médico\n- `fecha` (yyyy-MM-dd): citas de un día\n- `soloActivas=true`: solo citas activas\n\nSin filtros devuelve todas.\n\nAcceso: cualquier usuario autenticado.")
    public List<CitasDTO> findAll(
            @RequestParam(required = false, defaultValue = "false") boolean soloActivas,
            @RequestParam(required = false) Long idPaciente,
            @RequestParam(required = false) Long idMedico,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return citasService.buscar(idPaciente, idMedico, fecha, soloActivas);
    }

    @GetMapping("/agenda")
    @Operation(summary = "Agenda diaria de un médico", description = "**Nuevo.** Devuelve las citas activas de un médico para un día, ordenadas por hora. Si no se envía `fecha`, usa el día de hoy. Responde 404 si el médico no existe.\n\nAcceso: cualquier usuario autenticado.")
    public List<CitasDTO> agendaMedico(
            @RequestParam Long idMedico,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
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

    @GetMapping("/{id}")
    @Operation(summary = "Obtener cita por ID", description = "Devuelve el detalle de una cita. Responde 404 si no existe.\n\nAcceso: cualquier usuario autenticado.")
    public CitasDTO findById(@PathVariable Long id) {
        return citasService.findById(id);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA')")
    @PostMapping
    @Operation(summary = "Programar cita", description = "Crea una nueva cita para un paciente con un médico. Valida que paciente y médico existan y estén activos, y que el médico no tenga otra cita a la misma fecha y hora.\n\nRoles permitidos: ADMIN, RECEPCIONISTA.")
    public ResponseEntity<CitasDTO> save(@Valid @RequestBody CitasDTO dto) {
        CitasDTO creada = citasService.crear(dto);
        return new ResponseEntity<>(creada, HttpStatus.CREATED);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA')")
    @PutMapping("/{id}")
    @Operation(summary = "Reprogramar / actualizar cita", description = "Modifica fecha, hora, motivo, médico o paciente de una cita, validando de nuevo choques de horario del médico.\n\nRoles permitidos: ADMIN, RECEPCIONISTA.")
    public ResponseEntity<CitasDTO> update(
            @PathVariable Long id,
            @Valid @RequestBody CitasDTO dto) {
        CitasDTO actualizada = citasService.actualizar(dto, id);
        return ResponseEntity.ok(actualizada);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA', 'MEDICO')")
    @PatchMapping("/{id}/estado/{nuevoIdEstado}")
    @Operation(summary = "Cambiar estado de la cita", description = "Mueve la cita a otro estado (p. ej. Programada → Confirmada → Atendida, o Cancelada). Solo permite transiciones válidas según las reglas de `estado-cita`.\n\nRoles permitidos: ADMIN, RECEPCIONISTA, MEDICO.")
    public ResponseEntity<CitasDTO> cambiarEstado(
            @PathVariable Long id,
            @PathVariable Long nuevoIdEstado) {
        CitasDTO actualizada = citasService.cambiarEstado(id, nuevoIdEstado);
        return ResponseEntity.ok(actualizada);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar cita", description = "Desactiva una cita (borrado lógico).\n\nRoles permitidos: ADMIN.")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        citasService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
