package com.example.hospital.medicoespecialidades;

import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import com.example.hospital.medicoespecialidades.dto.MedicoEspecialidadesDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/medico-especialidades")
@Tag(name = "Médico – especialidades", description = "Especialidades asignadas a cada médico")
@ApiResponses({
        @ApiResponse(responseCode = "401", description = "Token ausente, inválido o expirado"),
        @ApiResponse(responseCode = "403", description = "El rol del usuario no tiene permiso")
})
public class MedicoEspecialidadesController {

    private final MedicoEspecialidadesService medicoEspecialidadesService;

    public MedicoEspecialidadesController(MedicoEspecialidadesService medicoEspecialidadesService) {
        this.medicoEspecialidadesService = medicoEspecialidadesService;
    }

    @GetMapping
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Consulta exitosa")
    })
    @Operation(summary = "Listar asignaciones", description = "Devuelve todas las relaciones médico–especialidad.\n\nAcceso: cualquier usuario autenticado.")
    public List<MedicoEspecialidadesDTO> findAll() {
        return medicoEspecialidadesService.findAll();
    }

    @GetMapping("/medico/{idMedico}")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Consulta exitosa"),
            @ApiResponse(responseCode = "400", description = "Algún parámetro de la ruta o de la consulta tiene un formato inválido (por ejemplo un id no numérico o una fecha mal escrita)"),
            @ApiResponse(responseCode = "422", description = "Regla de negocio incumplida")
    })
    @Operation(summary = "Especialidades de un médico", description = "Devuelve las especialidades que tiene un médico.\n\nAcceso: cualquier usuario autenticado.")
    public List<MedicoEspecialidadesDTO> findByMedico(@Parameter(description = "Id del médico.", example = "1") @PathVariable Long idMedico) {
        return medicoEspecialidadesService.findByMedico(idMedico);
    }

    @GetMapping("/especialidad/{idEspecialidad}")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Consulta exitosa"),
            @ApiResponse(responseCode = "400", description = "Algún parámetro de la ruta o de la consulta tiene un formato inválido (por ejemplo un id no numérico o una fecha mal escrita)"),
            @ApiResponse(responseCode = "422", description = "Regla de negocio incumplida")
    })
    @Operation(summary = "Médicos de una especialidad", description = "Devuelve los médicos que tienen una especialidad; útil para elegir médico al programar una cita.\n\nAcceso: cualquier usuario autenticado.")
    public List<MedicoEspecialidadesDTO> findByEspecialidad(@Parameter(description = "Id de la especialidad.", example = "1") @PathVariable Long idEspecialidad) {
        return medicoEspecialidadesService.findByEspecialidad(idEspecialidad);
    }

    @GetMapping("/{idEspecialidad}/{idMedico}")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Consulta exitosa"),
            @ApiResponse(responseCode = "400", description = "Algún parámetro de la ruta o de la consulta tiene un formato inválido (por ejemplo un id no numérico o una fecha mal escrita)"),
            @ApiResponse(responseCode = "404", description = "No existe la asignación para la especialidad y médico"),
            @ApiResponse(responseCode = "422", description = "Regla de negocio incumplida")
    })
    @Operation(summary = "Obtener asignación", description = "Devuelve una relación específica por su llave compuesta (idEspecialidad + idMedico).\n\nAcceso: cualquier usuario autenticado.")
    public MedicoEspecialidadesDTO findById(@Parameter(description = "Id de la especialidad.", example = "1") @PathVariable Long idEspecialidad, @Parameter(description = "Id del médico.", example = "1") @PathVariable Long idMedico) {
        return medicoEspecialidadesService.findById(idEspecialidad, idMedico);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Creado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos del cuerpo inválidos: ver `detalles` con el mensaje de cada campo"),
            @ApiResponse(responseCode = "409", description = "El médico ya tiene asignada esta especialidad"),
            @ApiResponse(responseCode = "422", description = "El médico indicado no existe; La especialidad médica indicada no existe; La especialidad médica se encuentra inactiva")
    })
    @Operation(summary = "Asignar especialidad a médico", description = "Vincula una especialidad a un médico. No permite duplicados.\n\nRoles permitidos: ADMIN.")
    public ResponseEntity<MedicoEspecialidadesDTO> save(@Valid @RequestBody MedicoEspecialidadesDTO dto) {
        MedicoEspecialidadesDTO creado = medicoEspecialidadesService.crear(dto);
        return new ResponseEntity<>(creado, HttpStatus.CREATED);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{idEspecialidad}/{idMedico}")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Operación realizada sin contenido en la respuesta"),
            @ApiResponse(responseCode = "400", description = "Algún parámetro de la ruta o de la consulta tiene un formato inválido (por ejemplo un id no numérico o una fecha mal escrita)"),
            @ApiResponse(responseCode = "404", description = "No existe la asignación de especialidad para el médico"),
            @ApiResponse(responseCode = "422", description = "Regla de negocio incumplida")
    })
    @Operation(summary = "Quitar especialidad a médico", description = "Elimina el vínculo entre un médico y una especialidad.\n\nRoles permitidos: ADMIN.")
    public ResponseEntity<Void> delete(@Parameter(description = "Id de la especialidad.", example = "1") @PathVariable Long idEspecialidad, @Parameter(description = "Id del médico.", example = "1") @PathVariable Long idMedico) {
        medicoEspecialidadesService.eliminar(idEspecialidad, idMedico);
        return ResponseEntity.noContent().build();
    }
}
