package com.example.hospital.alergias;

import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import com.example.hospital.alergias.dto.AlergiasDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/alergias")
@Tag(name = "Alergias", description = "Catálogo de alergias que se pueden asignar a los pacientes")
@ApiResponses({
        @ApiResponse(responseCode = "401", description = "Token ausente, inválido o expirado"),
        @ApiResponse(responseCode = "403", description = "El rol del usuario no tiene permiso")
})
public class AlergiasController {

    private final AlergiasService alergiasService;

    public AlergiasController(AlergiasService alergiasService) {
        this.alergiasService = alergiasService;
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO', 'ENFERMERO', 'RECEPCIONISTA')")
    @GetMapping
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Consulta exitosa")
    })
    @Operation(summary = "Listar alergias", description = "Devuelve todas las alergias registradas en el catálogo. Se usa para llenar listas desplegables al registrar o editar un paciente.\n\nRoles permitidos: ADMIN, MEDICO, ENFERMERO, RECEPCIONISTA.")
    public List<AlergiasDTO> findAll() {
        return alergiasService.findAll();
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO', 'ENFERMERO', 'RECEPCIONISTA')")
    @GetMapping("/{id}")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Consulta exitosa"),
            @ApiResponse(responseCode = "400", description = "Algún parámetro de la ruta o de la consulta tiene un formato inválido (por ejemplo un id no numérico o una fecha mal escrita)"),
            @ApiResponse(responseCode = "404", description = "No existe la alergia indicada")
    })
    @Operation(summary = "Obtener alergia por ID", description = "Devuelve el detalle de una alergia específica. Responde 404 si no existe.\n\nRoles permitidos: ADMIN, MEDICO, ENFERMERO, RECEPCIONISTA.")
    public AlergiasDTO findById(@Parameter(description = "Id del registro en «Alergias» (debe existir).", example = "1") @PathVariable Long id) {
        return alergiasService.findById(id);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO')")
    @PostMapping
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Creado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos del cuerpo inválidos: ver `detalles` con el mensaje de cada campo"),
            @ApiResponse(responseCode = "409", description = "Ya existe una alergia con el nombre"),
            @ApiResponse(responseCode = "422", description = "El nombre de la alergia es obligatorio")
    })
    @Operation(summary = "Registrar alergia", description = "Agrega una nueva alergia al catálogo. Valida que el nombre no esté duplicado.\n\nRoles permitidos: ADMIN, MEDICO.")
    public ResponseEntity<AlergiasDTO> save(@Valid @RequestBody AlergiasDTO dto) {
        AlergiasDTO creado = alergiasService.crear(dto);
        return new ResponseEntity<>(creado, HttpStatus.CREATED);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO')")
    @PutMapping("/{id}")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Actualizado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos del cuerpo inválidos: ver `detalles` con el mensaje de cada campo; Algún parámetro de la ruta o de la consulta tiene un formato inválido (por ejemplo un id no numérico o una fecha mal escrita)"),
            @ApiResponse(responseCode = "404", description = "No existe la alergia indicada"),
            @ApiResponse(responseCode = "409", description = "Ya existe una alergia con el nombre"),
            @ApiResponse(responseCode = "422", description = "Datos para actualizar inválidos; El nombre de la alergia es obligatorio")
    })
    @Operation(summary = "Actualizar alergia", description = "Modifica los datos de una alergia existente.\n\nRoles permitidos: ADMIN, MEDICO.")
    public ResponseEntity<AlergiasDTO> update(
            @Parameter(description = "Id del registro en «Alergias» (debe existir).", example = "1") @PathVariable Long id,
            @Valid @RequestBody AlergiasDTO dto) {
        AlergiasDTO actualizado = alergiasService.actualizar(dto, id);
        return ResponseEntity.ok(actualizado);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Operación realizada sin contenido en la respuesta"),
            @ApiResponse(responseCode = "400", description = "Algún parámetro de la ruta o de la consulta tiene un formato inválido (por ejemplo un id no numérico o una fecha mal escrita)"),
            @ApiResponse(responseCode = "404", description = "No existe la alergia indicada"),
            @ApiResponse(responseCode = "409", description = "No se puede eliminar la alergia porque está asignada a uno o más pacientes")
    })
    @Operation(summary = "Eliminar alergia", description = "Desactiva una alergia del catálogo. No se permite si hay pacientes que la tienen asignada.\n\nRoles permitidos: ADMIN.")
    public ResponseEntity<Void> delete(@Parameter(description = "Id del registro en «Alergias» (debe existir).", example = "1") @PathVariable Long id) {
        alergiasService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
