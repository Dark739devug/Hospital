package com.example.hospital.especialidadmedico;

import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import com.example.hospital.especialidadmedico.dto.EspecialidadMedicoDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/especialidad-medico")
@Tag(name = "Especialidades médicas", description = "Catálogo de especialidades que puede tener un médico")
@ApiResponses({
        @ApiResponse(responseCode = "401", description = "Token ausente, inválido o expirado"),
        @ApiResponse(responseCode = "403", description = "El rol del usuario no tiene permiso")
})
public class EspecialidadMedicoController {

    private final EspecialidadMedicoService especialidadMedicoService;

    public EspecialidadMedicoController(EspecialidadMedicoService especialidadMedicoService) {
        this.especialidadMedicoService = especialidadMedicoService;
    }

    @GetMapping
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Consulta exitosa"),
            @ApiResponse(responseCode = "400", description = "Algún parámetro de la ruta o de la consulta tiene un formato inválido (por ejemplo un id no numérico o una fecha mal escrita)")
    })
    @Operation(summary = "Listar especialidades médicas", description = "Devuelve el catálogo de especialidades (Cardiología, Pediatría, etc.). Con `soloActivas=true` solo las activas.\n\nAcceso: cualquier usuario autenticado.")
    public List<EspecialidadMedicoDTO> findAll(@Parameter(description = "Si es true devuelve solo los registros activos; por defecto false (todos).", example = "true") @RequestParam(required = false, defaultValue = "false") boolean soloActivas) {
        if (soloActivas) {
            return especialidadMedicoService.findActivas();
        }
        return especialidadMedicoService.findAll();
    }

    @GetMapping("/{id}")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Consulta exitosa"),
            @ApiResponse(responseCode = "400", description = "Algún parámetro de la ruta o de la consulta tiene un formato inválido (por ejemplo un id no numérico o una fecha mal escrita)"),
            @ApiResponse(responseCode = "404", description = "No existe la especialidad médica indicada")
    })
    @Operation(summary = "Obtener especialidad por ID", description = "Devuelve el detalle de una especialidad médica.\n\nAcceso: cualquier usuario autenticado.")
    public EspecialidadMedicoDTO findById(@Parameter(description = "Id del registro en «Especialidades médicas» (debe existir).", example = "1") @PathVariable Long id) {
        return especialidadMedicoService.findById(id);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Creado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos del cuerpo inválidos: ver `detalles` con el mensaje de cada campo"),
            @ApiResponse(responseCode = "409", description = "Ya existe una especialidad médica con el nombre"),
            @ApiResponse(responseCode = "422", description = "El nombre de la especialidad médica es obligatorio")
    })
    @Operation(summary = "Registrar especialidad", description = "Agrega una nueva especialidad al catálogo.\n\nRoles permitidos: ADMIN.")
    public ResponseEntity<EspecialidadMedicoDTO> save(@Valid @RequestBody EspecialidadMedicoDTO dto) {
        EspecialidadMedicoDTO creada = especialidadMedicoService.crear(dto);
        return new ResponseEntity<>(creada, HttpStatus.CREATED);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Actualizado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos del cuerpo inválidos: ver `detalles` con el mensaje de cada campo; Algún parámetro de la ruta o de la consulta tiene un formato inválido (por ejemplo un id no numérico o una fecha mal escrita)"),
            @ApiResponse(responseCode = "404", description = "No existe la especialidad médica indicada"),
            @ApiResponse(responseCode = "409", description = "Ya existe una especialidad médica con el nombre"),
            @ApiResponse(responseCode = "422", description = "El nombre de la especialidad médica es obligatorio")
    })
    @Operation(summary = "Actualizar especialidad", description = "Modifica una especialidad médica.\n\nRoles permitidos: ADMIN.")
    public ResponseEntity<EspecialidadMedicoDTO> update(
            @Parameter(description = "Id del registro en «Especialidades médicas» (debe existir).", example = "1") @PathVariable Long id,
            @Valid @RequestBody EspecialidadMedicoDTO dto) {
        EspecialidadMedicoDTO actualizada = especialidadMedicoService.actualizar(dto, id);
        return ResponseEntity.ok(actualizada);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Operación realizada sin contenido en la respuesta"),
            @ApiResponse(responseCode = "400", description = "Algún parámetro de la ruta o de la consulta tiene un formato inválido (por ejemplo un id no numérico o una fecha mal escrita)"),
            @ApiResponse(responseCode = "404", description = "No existe la especialidad médica indicada"),
            @ApiResponse(responseCode = "422", description = "Regla de negocio incumplida")
    })
    @Operation(summary = "Eliminar especialidad", description = "Desactiva una especialidad. No se permite si hay médicos que la tienen asignada.\n\nRoles permitidos: ADMIN.")
    public ResponseEntity<Void> delete(@Parameter(description = "Id del registro en «Especialidades médicas» (debe existir).", example = "1") @PathVariable Long id) {
        especialidadMedicoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
