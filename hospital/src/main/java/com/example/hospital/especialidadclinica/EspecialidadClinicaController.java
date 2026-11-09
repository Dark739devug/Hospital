package com.example.hospital.especialidadclinica;

import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import com.example.hospital.especialidadclinica.dto.EspecialidadClinicaDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/especialidad-clinica")
@Tag(name = "Especialidad de clínica", description = "Especialidades que se atienden en cada clínica")
@ApiResponses({
        @ApiResponse(responseCode = "401", description = "Token ausente, inválido o expirado"),
        @ApiResponse(responseCode = "403", description = "El rol del usuario no tiene permiso")
})
public class EspecialidadClinicaController {

    private final EspecialidadClinicaService especialidadClinicaService;

    public EspecialidadClinicaController(EspecialidadClinicaService especialidadClinicaService) {
        this.especialidadClinicaService = especialidadClinicaService;
    }

    @GetMapping
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Consulta exitosa"),
            @ApiResponse(responseCode = "400", description = "Algún parámetro de la ruta o de la consulta tiene un formato inválido (por ejemplo un id no numérico o una fecha mal escrita)")
    })
    @Operation(summary = "Listar especialidades de clínica", description = "Devuelve las especialidades por clínica. Con `soloActivas=true` solo las activas.\n\nAcceso: cualquier usuario autenticado.")
    public List<EspecialidadClinicaDTO> findAll(@Parameter(description = "Si es true devuelve solo los registros activos; por defecto false (todos).", example = "true") @RequestParam(required = false, defaultValue = "false") boolean soloActivas) {
        if (soloActivas) {
            return especialidadClinicaService.findActivas();
        }
        return especialidadClinicaService.findAll();
    }

    @GetMapping("/{id}")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Consulta exitosa"),
            @ApiResponse(responseCode = "400", description = "Algún parámetro de la ruta o de la consulta tiene un formato inválido (por ejemplo un id no numérico o una fecha mal escrita)"),
            @ApiResponse(responseCode = "404", description = "No existe la especialidad clínica indicada")
    })
    @Operation(summary = "Obtener especialidad de clínica por ID", description = "Devuelve el detalle de una especialidad de clínica.\n\nAcceso: cualquier usuario autenticado.")
    public EspecialidadClinicaDTO findById(@Parameter(description = "Id del registro en «Especialidad de clínica» (debe existir).", example = "1") @PathVariable Long id) {
        return especialidadClinicaService.findById(id);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Creado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos del cuerpo inválidos: ver `detalles` con el mensaje de cada campo"),
            @ApiResponse(responseCode = "409", description = "Ya existe una especialidad clínica con el nombre"),
            @ApiResponse(responseCode = "422", description = "El nombre de la especialidad clínica es obligatorio")
    })
    @Operation(summary = "Registrar especialidad de clínica", description = "Crea una nueva especialidad para una clínica.\n\nRoles permitidos: ADMIN.")
    public ResponseEntity<EspecialidadClinicaDTO> save(@Valid @RequestBody EspecialidadClinicaDTO dto) {
        EspecialidadClinicaDTO creada = especialidadClinicaService.crear(dto);
        return new ResponseEntity<>(creada, HttpStatus.CREATED);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Actualizado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos del cuerpo inválidos: ver `detalles` con el mensaje de cada campo; Algún parámetro de la ruta o de la consulta tiene un formato inválido (por ejemplo un id no numérico o una fecha mal escrita)"),
            @ApiResponse(responseCode = "404", description = "No existe la especialidad clínica indicada"),
            @ApiResponse(responseCode = "409", description = "Ya existe una especialidad clínica con el nombre"),
            @ApiResponse(responseCode = "422", description = "El nombre de la especialidad clínica es obligatorio")
    })
    @Operation(summary = "Actualizar especialidad de clínica", description = "Modifica una especialidad de clínica.\n\nRoles permitidos: ADMIN.")
    public ResponseEntity<EspecialidadClinicaDTO> update(
            @Parameter(description = "Id del registro en «Especialidad de clínica» (debe existir).", example = "1") @PathVariable Long id,
            @Valid @RequestBody EspecialidadClinicaDTO dto) {
        EspecialidadClinicaDTO actualizada = especialidadClinicaService.actualizar(dto, id);
        return ResponseEntity.ok(actualizada);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Operación realizada sin contenido en la respuesta"),
            @ApiResponse(responseCode = "400", description = "Algún parámetro de la ruta o de la consulta tiene un formato inválido (por ejemplo un id no numérico o una fecha mal escrita)"),
            @ApiResponse(responseCode = "404", description = "No existe la especialidad clínica indicada"),
            @ApiResponse(responseCode = "409", description = "No se puede eliminar la especialidad porque la usan clínicas activas"),
            @ApiResponse(responseCode = "422", description = "Regla de negocio incumplida")
    })
    @Operation(summary = "Eliminar especialidad de clínica", description = "Desactiva una especialidad de clínica.\n\nRoles permitidos: ADMIN.")
    public ResponseEntity<Void> delete(@Parameter(description = "Id del registro en «Especialidad de clínica» (debe existir).", example = "1") @PathVariable Long id) {
        especialidadClinicaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
