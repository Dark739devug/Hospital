package com.example.hospital.turnos;

import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import com.example.hospital.turnos.dto.TurnosDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/turnos")
@Tag(name = "Turnos", description = "Turnos de trabajo del personal (mañana, tarde, noche)")
@ApiResponses({
        @ApiResponse(responseCode = "401", description = "Token ausente, inválido o expirado"),
        @ApiResponse(responseCode = "403", description = "El rol del usuario no tiene permiso")
})
public class TurnosController {

    private final TurnosService turnosService;

    public TurnosController(TurnosService turnosService) {
        this.turnosService = turnosService;
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO', 'ENFERMERO', 'RECEPCIONISTA')")
    @GetMapping
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Consulta exitosa")
    })
    @Operation(summary = "Listar turnos", description = "Devuelve los turnos de trabajo con su hora de inicio y fin.\n\nRoles permitidos: ADMIN, MEDICO, ENFERMERO, RECEPCIONISTA.")
    public List<TurnosDTO> findAll() {
        return turnosService.findAll();
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO', 'ENFERMERO', 'RECEPCIONISTA')")
    @GetMapping("/{id}")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Consulta exitosa"),
            @ApiResponse(responseCode = "400", description = "Algún parámetro de la ruta o de la consulta tiene un formato inválido (por ejemplo un id no numérico o una fecha mal escrita)"),
            @ApiResponse(responseCode = "404", description = "No existe el turno indicado")
    })
    @Operation(summary = "Obtener turno por ID", description = "Devuelve el detalle de un turno.\n\nRoles permitidos: ADMIN, MEDICO, ENFERMERO, RECEPCIONISTA.")
    public TurnosDTO findById(@Parameter(description = "Id del registro en «Turnos» (debe existir).", example = "1") @PathVariable Long id) {
        return turnosService.findById(id);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Creado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos del cuerpo inválidos: ver `detalles` con el mensaje de cada campo"),
            @ApiResponse(responseCode = "409", description = "Ya existe un turno con la descripción"),
            @ApiResponse(responseCode = "422", description = "La descripción del turno es obligatoria y no puede estar vacía; La hora de inicio es obligatoria; La hora de finalización es obligatoria; La hora de inicio y la hora de finalización no pueden ser iguales; La duración del turno debe ser mayor a cero")
    })
    @Operation(summary = "Registrar turno", description = "Crea un nuevo turno. La descripción no puede repetirse.\n\nRoles permitidos: ADMIN.")
    public ResponseEntity<TurnosDTO> registrar(@Valid @RequestBody TurnosDTO dto) {
        TurnosDTO creado = turnosService.registrar(dto);
        return new ResponseEntity<>(creado, HttpStatus.CREATED);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Actualizado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos del cuerpo inválidos: ver `detalles` con el mensaje de cada campo; Algún parámetro de la ruta o de la consulta tiene un formato inválido (por ejemplo un id no numérico o una fecha mal escrita)"),
            @ApiResponse(responseCode = "404", description = "No existe el turno indicado"),
            @ApiResponse(responseCode = "409", description = "No se pueden modificar los horarios del turno porque tiene asignaciones de enfermeros vigentes o futuras asociadas; Ya existe un turno con la descripción"),
            @ApiResponse(responseCode = "422", description = "La descripción del turno es obligatoria y no puede estar vacía; La hora de inicio es obligatoria; La hora de finalización es obligatoria; La hora de inicio y la hora de finalización no pueden ser iguales; La duración del turno debe ser mayor a cero")
    })
    @Operation(summary = "Actualizar turno", description = "Modifica un turno.\n\nRoles permitidos: ADMIN.")
    public ResponseEntity<TurnosDTO> update(
            @Parameter(description = "Id del registro en «Turnos» (debe existir).", example = "1") @PathVariable Long id,
            @Valid @RequestBody TurnosDTO dto) {
        TurnosDTO actualizado = turnosService.actualizar(dto, id);
        return ResponseEntity.ok(actualizado);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Operación realizada sin contenido en la respuesta"),
            @ApiResponse(responseCode = "400", description = "Algún parámetro de la ruta o de la consulta tiene un formato inválido (por ejemplo un id no numérico o una fecha mal escrita)"),
            @ApiResponse(responseCode = "404", description = "No existe el turno indicado"),
            @ApiResponse(responseCode = "422", description = "Regla de negocio incumplida")
    })
    @Operation(summary = "Eliminar turno", description = "Desactiva un turno. No se permite si tiene asignaciones vigentes o futuras.\n\nRoles permitidos: ADMIN.")
    public ResponseEntity<Void> delete(@Parameter(description = "Id del registro en «Turnos» (debe existir).", example = "1") @PathVariable Long id) {
        turnosService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
