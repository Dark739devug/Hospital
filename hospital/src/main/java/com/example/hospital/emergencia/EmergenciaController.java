package com.example.hospital.emergencia;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import com.example.hospital.emergencia.dto.EmergenciaDTO;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/emergencia")
@Tag(name = "Emergencia", description = "Ingresos y atención en el área de emergencias")
@ApiResponses({
        @ApiResponse(responseCode = "401", description = "Token ausente, inválido o expirado"),
        @ApiResponse(responseCode = "403", description = "El rol del usuario no tiene permiso")
})
public class EmergenciaController {

    private final EmergenciaService emergenciaService;

    public EmergenciaController(EmergenciaService emergenciaService) {
        this.emergenciaService = emergenciaService;
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO', 'ENFERMERO', 'RECEPCIONISTA')")
    @GetMapping
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Consulta exitosa"),
            @ApiResponse(responseCode = "400", description = "Algún parámetro de la ruta o de la consulta tiene un formato inválido (por ejemplo un id no numérico o una fecha mal escrita)")
    })
    @Operation(summary = "Listar ingresos a emergencia", description = "Devuelve los ingresos a emergencia. Filtros opcionales y combinables:\n- `idPaciente`: ingresos de un paciente (activos e inactivos, salvo que también se envíe `soloActivas=true`)\n- `soloActivas=true`: solo registros activos\n\nRoles permitidos: ADMIN, MEDICO, ENFERMERO, RECEPCIONISTA.")
    public List<EmergenciaDTO> findAll(
            @Parameter(description = "Si es true devuelve solo los registros activos; por defecto false (todos).", example = "true") @RequestParam(required = false, defaultValue = "false") boolean soloActivas,
            @Parameter(description = "Id del paciente.", example = "1") @RequestParam(required = false) Long idPaciente) {
        return emergenciaService.buscar(idPaciente, soloActivas);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO', 'ENFERMERO', 'RECEPCIONISTA')")
    @GetMapping("/{id}")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Consulta exitosa"),
            @ApiResponse(responseCode = "400", description = "Algún parámetro de la ruta o de la consulta tiene un formato inválido (por ejemplo un id no numérico o una fecha mal escrita)"),
            @ApiResponse(responseCode = "404", description = "No existe el registro de emergencia indicado")
    })
    @Operation(summary = "Obtener ingreso por ID", description = "Devuelve el detalle de un ingreso a emergencia. Responde 404 si no existe.\n\nRoles permitidos: ADMIN, MEDICO, ENFERMERO, RECEPCIONISTA.")
    public EmergenciaDTO findById(@Parameter(description = "Id del registro en «Emergencia» (debe existir).", example = "1") @PathVariable Long id) {
        return emergenciaService.findById(id);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO', 'ENFERMERO')")
    @PostMapping
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Creado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos del cuerpo inválidos: ver `detalles` con el mensaje de cada campo"),
            @ApiResponse(responseCode = "422", description = "Debe especificarse un paciente registrado o un nombre temporal para el paciente no identificado; El paciente indicado no existe; El paciente indicado se encuentra inactivo; El médico indicado no existe; El médico indicado se encuentra inactivo")
    })
    @Operation(summary = "Registrar ingreso a emergencia", description = "Registra la llegada de un paciente a emergencia con su motivo, signos vitales, nivel de triaje, médico y cama. Permite `nombreTemporal` cuando aún no se conoce la identidad del paciente.\n\nRoles permitidos: ADMIN, MEDICO, ENFERMERO.")
    public ResponseEntity<EmergenciaDTO> save(@Valid @RequestBody EmergenciaDTO dto) {
        EmergenciaDTO creada = emergenciaService.crear(dto);
        return new ResponseEntity<>(creada, HttpStatus.CREATED);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO', 'ENFERMERO')")
    @PutMapping("/{id}")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Actualizado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos del cuerpo inválidos: ver `detalles` con el mensaje de cada campo; Algún parámetro de la ruta o de la consulta tiene un formato inválido (por ejemplo un id no numérico o una fecha mal escrita)"),
            @ApiResponse(responseCode = "404", description = "No existe el registro de emergencia indicado"),
            @ApiResponse(responseCode = "422", description = "La fecha y hora de ingreso no puede ser posterior a la fecha de alta; Debe especificarse un paciente registrado o un nombre temporal para el paciente no identificado; El paciente indicado no existe; El paciente indicado se encuentra inactivo; El médico indicado no existe")
    })
    @Operation(summary = "Actualizar ingreso", description = "Modifica los datos de un ingreso a emergencia. No modifica la fecha de alta (se conserva): el alta se registra solo con `PATCH /{id}/dar-alta`. Responde 400 con `detalles` si faltan datos obligatorios, 422 si una referencia (paciente, médico, habitación, triaje) no existe o está inactiva.\n\nRoles permitidos: ADMIN, MEDICO, ENFERMERO.")
    public ResponseEntity<EmergenciaDTO> update(
            @Parameter(description = "Id del registro en «Emergencia» (debe existir).", example = "1") @PathVariable Long id,
            @Valid @RequestBody EmergenciaDTO dto) {
        EmergenciaDTO actualizada = emergenciaService.actualizar(dto, id);
        return ResponseEntity.ok(actualizada);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO', 'ENFERMERO')")
    @PatchMapping("/{id}/dar-alta")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Actualizado correctamente"),
            @ApiResponse(responseCode = "400", description = "Algún parámetro de la ruta o de la consulta tiene un formato inválido (por ejemplo un id no numérico o una fecha mal escrita)"),
            @ApiResponse(responseCode = "404", description = "No existe el registro de emergencia indicado"),
            @ApiResponse(responseCode = "409", description = "La emergencia ya cuenta con fecha y hora de alta registrada"),
            @ApiResponse(responseCode = "422", description = "No se puede dar de alta una emergencia inactiva; La fecha y hora de alta no puede ser anterior a la fecha de ingreso")
    })
    @Operation(summary = "Dar de alta de emergencia", description = "Registra la salida del paciente de emergencia. Si no se envía `fechaHoraAlta`, se usa la fecha y hora actual. Responde 409 si el alta ya estaba registrada y 422 si la emergencia está inactiva o el alta es anterior al ingreso.\n\nRoles permitidos: ADMIN, MEDICO, ENFERMERO.")
    public ResponseEntity<EmergenciaDTO> darAlta(
            @Parameter(description = "Id del registro en «Emergencia» (debe existir).", example = "1") @PathVariable Long id,
            @Parameter(description = "Fecha y hora del alta (yyyy-MM-ddTHH:mm:ss); si se omite se usa la actual.", example = "2026-03-16T11:00:00") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaHoraAlta) {
        EmergenciaDTO dadaDeAlta = emergenciaService.darAlta(id, fechaHoraAlta);
        return ResponseEntity.ok(dadaDeAlta);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Operación realizada sin contenido en la respuesta"),
            @ApiResponse(responseCode = "400", description = "Algún parámetro de la ruta o de la consulta tiene un formato inválido (por ejemplo un id no numérico o una fecha mal escrita)"),
            @ApiResponse(responseCode = "404", description = "No existe el registro de emergencia indicado"),
            @ApiResponse(responseCode = "409", description = "No se puede eliminar la emergencia porque tiene procedimientos médicos de emergencia activos asociados"),
            @ApiResponse(responseCode = "422", description = "Regla de negocio incumplida")
    })
    @Operation(summary = "Eliminar ingreso", description = "Desactiva un ingreso a emergencia (borrado lógico).\n\nRoles permitidos: ADMIN.")
    public ResponseEntity<Void> delete(@Parameter(description = "Id del registro en «Emergencia» (debe existir).", example = "1") @PathVariable Long id) {
        emergenciaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
