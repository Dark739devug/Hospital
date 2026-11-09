package com.example.hospital.enfermeros;

import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import com.example.hospital.enfermeros.dto.EnfermerosDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/enfermeros")
@Tag(name = "Enfermeros", description = "Personal de enfermería")
@ApiResponses({
        @ApiResponse(responseCode = "401", description = "Token ausente, inválido o expirado"),
        @ApiResponse(responseCode = "403", description = "El rol del usuario no tiene permiso")
})
public class EnfermerosController {

    private final EnfermerosService enfermerosService;

    public EnfermerosController(EnfermerosService enfermerosService) {
        this.enfermerosService = enfermerosService;
    }

    @GetMapping
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Consulta exitosa"),
            @ApiResponse(responseCode = "400", description = "Algún parámetro de la ruta o de la consulta tiene un formato inválido (por ejemplo un id no numérico o una fecha mal escrita)")
    })
    @Operation(summary = "Listar enfermeros", description = "Devuelve el personal de enfermería. Con `soloActivos=true` devuelve solo los activos.\n\nAcceso: cualquier usuario autenticado.")
    public List<EnfermerosDTO> findAll(@Parameter(description = "Si es true devuelve solo los registros activos; por defecto false (todos).", example = "true") @RequestParam(required = false, defaultValue = "false") boolean soloActivos) {
        if (soloActivos) {
            return enfermerosService.findActivos();
        }
        return enfermerosService.findAll();
    }

    @GetMapping("/{id}")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Consulta exitosa"),
            @ApiResponse(responseCode = "400", description = "Algún parámetro de la ruta o de la consulta tiene un formato inválido (por ejemplo un id no numérico o una fecha mal escrita)"),
            @ApiResponse(responseCode = "404", description = "No existe el enfermero indicado")
    })
    @Operation(summary = "Obtener enfermero por ID", description = "Devuelve los datos de un enfermero. Responde 404 si no existe.\n\nAcceso: cualquier usuario autenticado.")
    public EnfermerosDTO findById(@Parameter(description = "Id del registro en «Enfermeros» (debe existir).", example = "1") @PathVariable Long id) {
        return enfermerosService.findById(id);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Creado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos del cuerpo inválidos: ver `detalles` con el mensaje de cada campo"),
            @ApiResponse(responseCode = "409", description = "Ya existe un enfermero registrado con la cédula profesional"),
            @ApiResponse(responseCode = "422", description = "La cédula profesional del enfermero es obligatoria; El nombre del enfermero es obligatorio; El apellido paterno del enfermero es obligatorio; El departamento es obligatorio para el enfermero; El departamento indicado no existe")
    })
    @Operation(summary = "Registrar enfermero", description = "Da de alta a un nuevo enfermero.\n\nRoles permitidos: ADMIN.")
    public ResponseEntity<EnfermerosDTO> save(@Valid @RequestBody EnfermerosDTO dto) {
        EnfermerosDTO creado = enfermerosService.crear(dto);
        return new ResponseEntity<>(creado, HttpStatus.CREATED);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Actualizado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos del cuerpo inválidos: ver `detalles` con el mensaje de cada campo; Algún parámetro de la ruta o de la consulta tiene un formato inválido (por ejemplo un id no numérico o una fecha mal escrita)"),
            @ApiResponse(responseCode = "404", description = "No existe el enfermero indicado"),
            @ApiResponse(responseCode = "409", description = "Ya existe un enfermero registrado con la cédula profesional"),
            @ApiResponse(responseCode = "422", description = "La cédula profesional del enfermero es obligatoria; El nombre del enfermero es obligatorio; El apellido paterno del enfermero es obligatorio; El departamento es obligatorio para el enfermero; El departamento indicado no existe")
    })
    @Operation(summary = "Actualizar enfermero", description = "Modifica los datos de un enfermero.\n\nRoles permitidos: ADMIN.")
    public ResponseEntity<EnfermerosDTO> update(
            @Parameter(description = "Id del registro en «Enfermeros» (debe existir).", example = "1") @PathVariable Long id,
            @Valid @RequestBody EnfermerosDTO dto) {
        EnfermerosDTO actualizado = enfermerosService.actualizar(dto, id);
        return ResponseEntity.ok(actualizado);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Operación realizada sin contenido en la respuesta"),
            @ApiResponse(responseCode = "400", description = "Algún parámetro de la ruta o de la consulta tiene un formato inválido (por ejemplo un id no numérico o una fecha mal escrita)"),
            @ApiResponse(responseCode = "404", description = "No existe el enfermero indicado"),
            @ApiResponse(responseCode = "409", description = "No se puede eliminar el enfermero porque tiene asignaciones activas"),
            @ApiResponse(responseCode = "422", description = "Regla de negocio incumplida")
    })
    @Operation(summary = "Eliminar enfermero", description = "Desactiva a un enfermero. No se permite si tiene asignaciones registradas.\n\nRoles permitidos: ADMIN.")
    public ResponseEntity<Void> delete(@Parameter(description = "Id del registro en «Enfermeros» (debe existir).", example = "1") @PathVariable Long id) {
        enfermerosService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
