package com.example.hospital.hospitalizaciones;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import com.example.hospital.hospitalizaciones.dto.HospitalizacionResumenDTO;
import com.example.hospital.hospitalizaciones.dto.HospitalizacionesDTO;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/hospitalizaciones")
@Tag(name = "Hospitalizaciones", description = "Ingreso, seguimiento y alta de pacientes hospitalizados")
@ApiResponses({
        @ApiResponse(responseCode = "401", description = "Token ausente, inválido o expirado"),
        @ApiResponse(responseCode = "403", description = "El rol del usuario no tiene permiso")
})
public class HospitalizacionesController {

    private final HospitalizacionesService hospitalizacionesService;

    public HospitalizacionesController(HospitalizacionesService hospitalizacionesService) {
        this.hospitalizacionesService = hospitalizacionesService;
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO', 'ENFERMERO', 'RECEPCIONISTA')")
    @GetMapping
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Consulta exitosa")
    })
    @Operation(summary = "Listar hospitalizaciones", description = "Devuelve todas las hospitalizaciones (activas e históricas).\n\nRoles permitidos: ADMIN, MEDICO, ENFERMERO, RECEPCIONISTA.")
    public List<HospitalizacionesDTO> findAll() {
        return hospitalizacionesService.findAll();
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO', 'ENFERMERO', 'RECEPCIONISTA')")
    @GetMapping("/activas")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Consulta exitosa")
    })
    @Operation(summary = "Pacientes hospitalizados actualmente", description = "Devuelve solo las hospitalizaciones en curso (estado Activa). Sirve para el censo de pacientes internados.\n\nRoles permitidos: ADMIN, MEDICO, ENFERMERO, RECEPCIONISTA.")
    public List<HospitalizacionesDTO> findActivas() {
        return hospitalizacionesService.findActivas();
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'CAJERO', 'RECEPCIONISTA')")
    @GetMapping("/paciente/{idPaciente}/resumen")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Consulta exitosa"),
            @ApiResponse(responseCode = "400", description = "Algún parámetro de la ruta o de la consulta tiene un formato inválido (por ejemplo un id no numérico o una fecha mal escrita)"),
            @ApiResponse(responseCode = "404", description = "No existe el paciente indicado; No existe la hospitalización indicada")
    })
    @Operation(summary = "Resumen de hospitalizaciones de un paciente (caja)", description = "Devuelve, para elegir en facturas y cuentas, solo `idHospitalizacion`, `fechaIngreso`, `fechaAlta`, `estado` (por nombre) y `numeroHabitacion`, más recientes primero. NO incluye motivo de ingreso, diagnóstico, médico ni clínica. Responde 404 si el paciente no existe.\n\nJustificación: mínimo privilegio; CAJERO no puede leer el historial completo de hospitalizaciones.\n\nRoles permitidos: ADMIN, CAJERO, RECEPCIONISTA.")
    public List<HospitalizacionResumenDTO> resumenPorPaciente(@Parameter(description = "Id del paciente.", example = "1") @PathVariable Long idPaciente) {
        return hospitalizacionesService.resumenPorPaciente(idPaciente);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO', 'ENFERMERO', 'RECEPCIONISTA')")
    @GetMapping("/paciente/{idPaciente}")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Consulta exitosa"),
            @ApiResponse(responseCode = "400", description = "Algún parámetro de la ruta o de la consulta tiene un formato inválido (por ejemplo un id no numérico o una fecha mal escrita)"),
            @ApiResponse(responseCode = "404", description = "No existe el paciente indicado")
    })
    @Operation(summary = "Hospitalizaciones de un paciente", description = "Devuelve el historial de hospitalizaciones de un paciente. Responde 404 si el paciente no existe.\n\nRoles permitidos: ADMIN, MEDICO, ENFERMERO, RECEPCIONISTA.")
    public List<HospitalizacionesDTO> findByPaciente(@Parameter(description = "Id del paciente.", example = "1") @PathVariable Long idPaciente) {
        return hospitalizacionesService.findByPaciente(idPaciente);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO', 'ENFERMERO', 'RECEPCIONISTA')")
    @GetMapping("/{id}")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Consulta exitosa"),
            @ApiResponse(responseCode = "400", description = "Algún parámetro de la ruta o de la consulta tiene un formato inválido (por ejemplo un id no numérico o una fecha mal escrita)"),
            @ApiResponse(responseCode = "404", description = "No existe la hospitalización indicada")
    })
    @Operation(summary = "Obtener hospitalización por ID", description = "Devuelve el detalle de una hospitalización. Responde 404 si no existe.\n\nRoles permitidos: ADMIN, MEDICO, ENFERMERO, RECEPCIONISTA.")
    public HospitalizacionesDTO findById(@Parameter(description = "Id del registro en «Hospitalizaciones» (debe existir).", example = "1") @PathVariable Long id) {
        return hospitalizacionesService.findById(id);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO', 'RECEPCIONISTA')")
    @PostMapping
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Creado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos del cuerpo inválidos: ver `detalles` con el mensaje de cada campo"),
            @ApiResponse(responseCode = "409", description = "El paciente ya cuenta con una hospitalización Activa"),
            @ApiResponse(responseCode = "422", description = "El paciente indicado no existe; El paciente seleccionado se encuentra inactivo; El médico indicado no existe; El médico responsable se encuentra inactivo; La habitación indicada no existe")
    })
    @Operation(summary = "Ingresar paciente", description = "Registra el ingreso de un paciente a una habitación con su médico responsable y clínica. Valida que el paciente no tenga otra hospitalización activa y que la habitación tenga cupo.\n\nRoles permitidos: ADMIN, MEDICO, RECEPCIONISTA.")
    public ResponseEntity<HospitalizacionesDTO> registrar(@Valid @RequestBody HospitalizacionesDTO dto) {
        HospitalizacionesDTO creado = hospitalizacionesService.registrar(dto);
        return new ResponseEntity<>(creado, HttpStatus.CREATED);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO')")
    @PutMapping("/{id}")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Actualizado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos del cuerpo inválidos: ver `detalles` con el mensaje de cada campo; Algún parámetro de la ruta o de la consulta tiene un formato inválido (por ejemplo un id no numérico o una fecha mal escrita)"),
            @ApiResponse(responseCode = "404", description = "No existe la hospitalización indicada"),
            @ApiResponse(responseCode = "422", description = "No se permite modificar una hospitalización finalizada o no activa; No se permite modificar el paciente asociado a una hospitalización registrada; El médico indicado no existe; El nuevo médico responsable no está activo; La nueva habitación no existe")
    })
    @Operation(summary = "Actualizar hospitalización", description = "Reemplaza los campos editables de una hospitalización Activa: médico responsable, habitación (traslado), clínica, motivo y diagnóstico de ingreso; un `null` en clínica o diagnóstico los borra. NO modifica el paciente, la fecha de ingreso, la fecha de alta ni el estado. Responde 400 con `detalles` si faltan datos obligatorios y 422 si una referencia no existe, la habitación está llena o en mantenimiento, o la hospitalización ya no está Activa.\n\nRoles permitidos: ADMIN, MEDICO.")
    public ResponseEntity<HospitalizacionesDTO> update(
            @Parameter(description = "Id del registro en «Hospitalizaciones» (debe existir).", example = "1") @PathVariable Long id,
            @Valid @RequestBody HospitalizacionesDTO dto) {
        HospitalizacionesDTO actualizado = hospitalizacionesService.actualizar(dto, id);
        return ResponseEntity.ok(actualizado);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO')")
    @PostMapping("/{id}/alta")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Alta registrada correctamente"),
            @ApiResponse(responseCode = "400", description = "Algún parámetro de la ruta o de la consulta tiene un formato inválido (por ejemplo un id no numérico o una fecha mal escrita)"),
            @ApiResponse(responseCode = "404", description = "No existe la hospitalización indicada"),
            @ApiResponse(responseCode = "409", description = "Solo se puede dar de alta una hospitalización que se encuentre Activa"),
            @ApiResponse(responseCode = "422", description = "La fecha de alta no puede ser anterior a la fecha de ingreso")
    })
    @Operation(summary = "Dar de alta", description = "Registra el alta del paciente, cierra la hospitalización y libera la habitación. Si no se envía `fechaAlta`, se usa la fecha y hora actual.\n\nRoles permitidos: ADMIN, MEDICO.")
    public ResponseEntity<HospitalizacionesDTO> darAlta(
            @Parameter(description = "Id del registro en «Hospitalizaciones» (debe existir).", example = "1") @PathVariable Long id,
            @Parameter(description = "Fecha y hora del alta (yyyy-MM-ddTHH:mm:ss); si se omite se usa la actual.", example = "2026-03-16T11:00:00") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaAlta) {
        HospitalizacionesDTO conAlta = hospitalizacionesService.darAlta(id, fechaAlta);
        return ResponseEntity.ok(conAlta);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Operación realizada sin contenido en la respuesta"),
            @ApiResponse(responseCode = "400", description = "Algún parámetro de la ruta o de la consulta tiene un formato inválido (por ejemplo un id no numérico o una fecha mal escrita)"),
            @ApiResponse(responseCode = "404", description = "No existe la hospitalización indicada"),
            @ApiResponse(responseCode = "409", description = "No se puede desactivar una hospitalización Activa; registre primero el alta del paciente")
    })
    @Operation(summary = "Eliminar hospitalización", description = "Desactiva una hospitalización (borrado lógico). Responde 409 si la hospitalización está Activa: primero hay que registrar el alta (`POST /{id}/alta`). Una hospitalización con alta sí se puede desactivar.\n\nRoles permitidos: ADMIN.")
    public ResponseEntity<Void> delete(@Parameter(description = "Id del registro en «Hospitalizaciones» (debe existir).", example = "1") @PathVariable Long id) {
        hospitalizacionesService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
