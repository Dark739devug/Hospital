package com.example.hospital.clinicas;

import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import com.example.hospital.clinicas.dto.ClinicasDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clinicas")
@Tag(name = "Clínicas", description = "Clínicas o unidades de atención del hospital")
@ApiResponses({
        @ApiResponse(responseCode = "401", description = "Token ausente, inválido o expirado"),
        @ApiResponse(responseCode = "403", description = "El rol del usuario no tiene permiso")
})
public class ClinicasController {

    private final ClinicasService clinicasService;

    public ClinicasController(ClinicasService clinicasService) {
        this.clinicasService = clinicasService;
    }

    @GetMapping
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Consulta exitosa"),
            @ApiResponse(responseCode = "400", description = "Algún parámetro de la ruta o de la consulta tiene un formato inválido (por ejemplo un id no numérico o una fecha mal escrita)")
    })
    @Operation(summary = "Listar clínicas", description = "Devuelve las clínicas del hospital. Con `soloActivas=true` devuelve únicamente las activas.\n\nAcceso: cualquier usuario autenticado.")
    public List<ClinicasDTO> findAll(@Parameter(description = "Si es true devuelve solo los registros activos; por defecto false (todos).", example = "true") @RequestParam(required = false, defaultValue = "false") boolean soloActivas) {
        if (soloActivas) {
            return clinicasService.findActivas();
        }
        return clinicasService.findAll();
    }

    @GetMapping("/{id}")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Consulta exitosa"),
            @ApiResponse(responseCode = "400", description = "Algún parámetro de la ruta o de la consulta tiene un formato inválido (por ejemplo un id no numérico o una fecha mal escrita)"),
            @ApiResponse(responseCode = "404", description = "No existe la clínica indicada")
    })
    @Operation(summary = "Obtener clínica por ID", description = "Devuelve el detalle de una clínica. Responde 404 si no existe.\n\nAcceso: cualquier usuario autenticado.")
    public ClinicasDTO findById(@Parameter(description = "Id del registro en «Clínicas» (debe existir).", example = "1") @PathVariable Long id) {
        return clinicasService.findById(id);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Creado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos del cuerpo inválidos: ver `detalles` con el mensaje de cada campo"),
            @ApiResponse(responseCode = "409", description = "El nombre de la clínica debe ser único. Ya existe una clínica con el nombre"),
            @ApiResponse(responseCode = "422", description = "El nombre de la clínica es obligatorio; El departamento es obligatorio. Toda clínica debe pertenecer a un departamento existente; El departamento indicado no existe; El departamento seleccionado se encuentra inactivo")
    })
    @Operation(summary = "Registrar clínica", description = "Crea una nueva clínica dentro de un departamento.\n\nRoles permitidos: ADMIN.")
    public ResponseEntity<ClinicasDTO> save(@Valid @RequestBody ClinicasDTO dto) {
        ClinicasDTO creado = clinicasService.crear(dto);
        return new ResponseEntity<>(creado, HttpStatus.CREATED);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Actualizado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos del cuerpo inválidos: ver `detalles` con el mensaje de cada campo; Algún parámetro de la ruta o de la consulta tiene un formato inválido (por ejemplo un id no numérico o una fecha mal escrita)"),
            @ApiResponse(responseCode = "404", description = "No existe la clínica indicada"),
            @ApiResponse(responseCode = "409", description = "El nombre de la clínica debe ser único. Ya existe una clínica con el nombre"),
            @ApiResponse(responseCode = "422", description = "El nombre de la clínica es obligatorio; El departamento es obligatorio. Toda clínica debe pertenecer a un departamento existente; El departamento indicado no existe; El departamento seleccionado se encuentra inactivo")
    })
    @Operation(summary = "Actualizar clínica", description = "Modifica los datos de una clínica existente.\n\nRoles permitidos: ADMIN.")
    public ResponseEntity<ClinicasDTO> update(
            @Parameter(description = "Id del registro en «Clínicas» (debe existir).", example = "1") @PathVariable Long id,
            @Valid @RequestBody ClinicasDTO dto) {
        ClinicasDTO actualizado = clinicasService.actualizar(dto, id);
        return ResponseEntity.ok(actualizado);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Operación realizada sin contenido en la respuesta"),
            @ApiResponse(responseCode = "400", description = "Algún parámetro de la ruta o de la consulta tiene un formato inválido (por ejemplo un id no numérico o una fecha mal escrita)"),
            @ApiResponse(responseCode = "404", description = "No existe la clínica indicada"),
            @ApiResponse(responseCode = "409", description = "No se puede eliminar la clínica porque tiene hospitalizaciones activas"),
            @ApiResponse(responseCode = "422", description = "Regla de negocio incumplida")
    })
    @Operation(summary = "Eliminar clínica", description = "Desactiva una clínica. No se permite si tiene registros activos asociados.\n\nRoles permitidos: ADMIN.")
    public ResponseEntity<Void> delete(@Parameter(description = "Id del registro en «Clínicas» (debe existir).", example = "1") @PathVariable Long id) {
        clinicasService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
