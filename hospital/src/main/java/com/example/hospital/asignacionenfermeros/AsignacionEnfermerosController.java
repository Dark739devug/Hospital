package com.example.hospital.asignacionenfermeros;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import com.example.hospital.asignacionenfermeros.dto.AsignacionEnfermerosDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/asignacion-enfermeros")
@Tag(name = "Asignación de enfermeros", description = "Asignación de enfermeros a pacientes hospitalizados por fecha y turno")
@ApiResponses({
        @ApiResponse(responseCode = "401", description = "Token ausente, inválido o expirado"),
        @ApiResponse(responseCode = "403", description = "El rol del usuario no tiene permiso")
})
public class AsignacionEnfermerosController {

    private final AsignacionEnfermerosService asignacionEnfermerosService;

    public AsignacionEnfermerosController(AsignacionEnfermerosService asignacionEnfermerosService) {
        this.asignacionEnfermerosService = asignacionEnfermerosService;
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO', 'ENFERMERO', 'RECEPCIONISTA')")
    @GetMapping
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Consulta exitosa")
    })
    @Operation(summary = "Listar asignaciones", description = "Devuelve todas las asignaciones de enfermeros a hospitalizaciones.\n\nRoles permitidos: ADMIN, MEDICO, ENFERMERO, RECEPCIONISTA.")
    public List<AsignacionEnfermerosDTO> findAll() {
        return asignacionEnfermerosService.findAll();
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO', 'ENFERMERO', 'RECEPCIONISTA')")
    @GetMapping("/{id}")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Consulta exitosa"),
            @ApiResponse(responseCode = "400", description = "Algún parámetro de la ruta o de la consulta tiene un formato inválido (por ejemplo un id no numérico o una fecha mal escrita)"),
            @ApiResponse(responseCode = "404", description = "No existe la asignación indicada")
    })
    @Operation(summary = "Obtener asignación por ID", description = "Devuelve el detalle de una asignación específica (enfermero, hospitalización, fecha y turno).\n\nRoles permitidos: ADMIN, MEDICO, ENFERMERO, RECEPCIONISTA.")
    public AsignacionEnfermerosDTO findById(@Parameter(description = "Id del registro en «Asignación de enfermeros» (debe existir).", example = "1") @PathVariable Long id) {
        return asignacionEnfermerosService.findById(id);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Creado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos del cuerpo inválidos: ver `detalles` con el mensaje de cada campo"),
            @ApiResponse(responseCode = "409", description = "Ya existe una asignación idéntica para este enfermero en la misma hospitalización, fecha y turno"),
            @ApiResponse(responseCode = "422", description = "El enfermero indicado no existe; El enfermero no se encuentra activo; La hospitalización indicada no existe; La hospitalización no se encuentra en estado Activa; El turno indicado no existe")
    })
    @Operation(summary = "Asignar enfermero", description = "Asigna un enfermero a una hospitalización activa en una fecha y turno. No permite asignaciones duplicadas (mismo enfermero, hospitalización, fecha y turno).\n\nRoles permitidos: ADMIN.")
    public ResponseEntity<AsignacionEnfermerosDTO> save(@Valid @RequestBody AsignacionEnfermerosDTO dto) {
        AsignacionEnfermerosDTO creado = asignacionEnfermerosService.crear(dto);
        return new ResponseEntity<>(creado, HttpStatus.CREATED);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Actualizado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos del cuerpo inválidos: ver `detalles` con el mensaje de cada campo; Algún parámetro de la ruta o de la consulta tiene un formato inválido (por ejemplo un id no numérico o una fecha mal escrita)"),
            @ApiResponse(responseCode = "404", description = "No existe la asignación indicada"),
            @ApiResponse(responseCode = "409", description = "Ya existe una asignación idéntica para este enfermero en la misma hospitalización, fecha y turno"),
            @ApiResponse(responseCode = "422", description = "El enfermero indicado no existe; El enfermero no se encuentra activo; La hospitalización indicada no existe; La hospitalización no se encuentra en estado Activa; El turno indicado no existe")
    })
    @Operation(summary = "Actualizar asignación", description = "Modifica el enfermero, la fecha o el turno de una asignación existente.\n\nRoles permitidos: ADMIN.")
    public ResponseEntity<AsignacionEnfermerosDTO> update(
            @Parameter(description = "Id del registro en «Asignación de enfermeros» (debe existir).", example = "1") @PathVariable Long id,
            @Valid @RequestBody AsignacionEnfermerosDTO dto) {
        AsignacionEnfermerosDTO actualizado = asignacionEnfermerosService.actualizar(dto, id);
        return ResponseEntity.ok(actualizado);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Operación realizada sin contenido en la respuesta"),
            @ApiResponse(responseCode = "400", description = "Algún parámetro de la ruta o de la consulta tiene un formato inválido (por ejemplo un id no numérico o una fecha mal escrita)"),
            @ApiResponse(responseCode = "404", description = "No existe la asignación indicada"),
            @ApiResponse(responseCode = "422", description = "Regla de negocio incumplida")
    })
    @Operation(summary = "Eliminar asignación", description = "Elimina/desactiva una asignación de enfermero.\n\nRoles permitidos: ADMIN.")
    public ResponseEntity<Void> delete(@Parameter(description = "Id del registro en «Asignación de enfermeros» (debe existir).", example = "1") @PathVariable Long id) {
        asignacionEnfermerosService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO', 'ENFERMERO', 'RECEPCIONISTA')")
    @GetMapping("/enfermero/{idEnfermero}")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Consulta exitosa"),
            @ApiResponse(responseCode = "400", description = "Algún parámetro de la ruta o de la consulta tiene un formato inválido (por ejemplo un id no numérico o una fecha mal escrita)"),
            @ApiResponse(responseCode = "404", description = "No existe el enfermero indicado")
    })
    @Operation(summary = "Asignaciones de un enfermero", description = "Devuelve todas las asignaciones de un enfermero; sirve para ver su carga de trabajo o su calendario de turnos. Responde 404 si el enfermero no existe.\n\nRoles permitidos: ADMIN, MEDICO, ENFERMERO, RECEPCIONISTA.")
    public List<AsignacionEnfermerosDTO> findByEnfermero(@Parameter(description = "Id del enfermero.", example = "1") @PathVariable Long idEnfermero) {
        return asignacionEnfermerosService.findByEnfermero(idEnfermero);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO', 'ENFERMERO', 'RECEPCIONISTA')")
    @GetMapping("/hospitalizacion/{idHospitalizacion}")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Consulta exitosa"),
            @ApiResponse(responseCode = "400", description = "Algún parámetro de la ruta o de la consulta tiene un formato inválido (por ejemplo un id no numérico o una fecha mal escrita)"),
            @ApiResponse(responseCode = "404", description = "No existe la hospitalización indicada")
    })
    @Operation(summary = "Enfermeros de una hospitalización", description = "Devuelve los enfermeros asignados a una hospitalización (quién atiende al paciente en cada turno). Responde 404 si la hospitalización no existe.\n\nRoles permitidos: ADMIN, MEDICO, ENFERMERO, RECEPCIONISTA.\n\nParámetro opcional `soloActivas` (por defecto `false`, igual que en los demás módulos): con `true` omite las asignaciones quitadas (borrado lógico).")
    public List<AsignacionEnfermerosDTO> findByHospitalizacion(
            @Parameter(description = "Id de la hospitalización.", example = "1") @PathVariable Long idHospitalizacion,
            @Parameter(description = "Si es true devuelve solo los registros activos; por defecto false (todos).", example = "true") @RequestParam(required = false, defaultValue = "false") boolean soloActivas) {
        return asignacionEnfermerosService.findByHospitalizacion(idHospitalizacion, soloActivas);
    }
}
