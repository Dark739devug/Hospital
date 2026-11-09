package com.example.hospital.tiposangre;

import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import com.example.hospital.tiposangre.dto.TipoSangreDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tipo-sangre")
@Tag(name = "Tipos de sangre", description = "Catálogo de tipos de sangre (A+, O-, etc.)")
@ApiResponses({
        @ApiResponse(responseCode = "401", description = "Token ausente, inválido o expirado"),
        @ApiResponse(responseCode = "403", description = "El rol del usuario no tiene permiso")
})
public class TipoSangreController {

    private final TipoSangreService tipoSangreService;

    public TipoSangreController(TipoSangreService tipoSangreService) {
        this.tipoSangreService = tipoSangreService;
    }

    // RN-TS-03: Catálogo de solo lectura para usuarios operativos
    @GetMapping
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Consulta exitosa")
    })
    @Operation(summary = "Listar tipos de sangre", description = "Devuelve el catálogo de tipos de sangre para asignar a los pacientes.\n\nAcceso: cualquier usuario autenticado.")
    public List<TipoSangreDTO> findAll() {
        return tipoSangreService.findAll();
    }

    // RN-TS-03: Catálogo de solo lectura para usuarios operativos
    @GetMapping("/{id}")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Consulta exitosa"),
            @ApiResponse(responseCode = "400", description = "Algún parámetro de la ruta o de la consulta tiene un formato inválido (por ejemplo un id no numérico o una fecha mal escrita)"),
            @ApiResponse(responseCode = "404", description = "No existe el tipo de sangre indicado")
    })
    @Operation(summary = "Obtener tipo de sangre por ID", description = "Devuelve el detalle de un tipo de sangre.\n\nAcceso: cualquier usuario autenticado.")
    public TipoSangreDTO findById(@Parameter(description = "Id del registro en «Tipos de sangre» (debe existir).", example = "1") @PathVariable Long id) {
        return tipoSangreService.findById(id);
    }

    // RN-TS-03: Solo el administrador lo modifica
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Creado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos del cuerpo inválidos: ver `detalles` con el mensaje de cada campo"),
            @ApiResponse(responseCode = "409", description = "Ya existe un registro para el tipo de sangre"),
            @ApiResponse(responseCode = "422", description = "El nombre del tipo de sangre es obligatorio; Valor de tipo de sangre inválido:. Solo se admiten los valores: A+, A-, B+, B-, AB+, AB-, O+, O-")
    })
    @Operation(summary = "Registrar tipo de sangre", description = "Agrega un tipo de sangre al catálogo.\n\nRoles permitidos: ADMIN.")
    public ResponseEntity<TipoSangreDTO> crear(@Valid @RequestBody TipoSangreDTO dto) {
        TipoSangreDTO creado = tipoSangreService.crear(dto);
        return new ResponseEntity<>(creado, HttpStatus.CREATED);
    }

    // RN-TS-03: Solo el administrador lo modifica
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Actualizado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos del cuerpo inválidos: ver `detalles` con el mensaje de cada campo; Algún parámetro de la ruta o de la consulta tiene un formato inválido (por ejemplo un id no numérico o una fecha mal escrita)"),
            @ApiResponse(responseCode = "404", description = "No existe el tipo de sangre indicado"),
            @ApiResponse(responseCode = "409", description = "Ya existe un registro para el tipo de sangre"),
            @ApiResponse(responseCode = "422", description = "El nombre del tipo de sangre es obligatorio; Valor de tipo de sangre inválido:. Solo se admiten los valores: A+, A-, B+, B-, AB+, AB-, O+, O-")
    })
    @Operation(summary = "Actualizar tipo de sangre", description = "Modifica un tipo de sangre.\n\nRoles permitidos: ADMIN.")
    public ResponseEntity<TipoSangreDTO> actualizar(
            @Parameter(description = "Id del registro en «Tipos de sangre» (debe existir).", example = "1") @PathVariable Long id,
            @Valid @RequestBody TipoSangreDTO dto) {
        TipoSangreDTO actualizado = tipoSangreService.actualizar(dto, id);
        return ResponseEntity.ok(actualizado);
    }

    // RN-TS-03: Solo el administrador lo modifica
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Operación realizada sin contenido en la respuesta"),
            @ApiResponse(responseCode = "400", description = "Algún parámetro de la ruta o de la consulta tiene un formato inválido (por ejemplo un id no numérico o una fecha mal escrita)"),
            @ApiResponse(responseCode = "404", description = "No existe el tipo de sangre indicado"),
            @ApiResponse(responseCode = "409", description = "No se puede eliminar el tipo de sangre porque se encuentra asignado a uno o más pacientes"),
            @ApiResponse(responseCode = "422", description = "Regla de negocio incumplida")
    })
    @Operation(summary = "Eliminar tipo de sangre", description = "Desactiva un tipo de sangre. No se permite si hay pacientes que lo tienen asignado.\n\nRoles permitidos: ADMIN.")
    public ResponseEntity<Void> eliminar(@Parameter(description = "Id del registro en «Tipos de sangre» (debe existir).", example = "1") @PathVariable Long id) {
        tipoSangreService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
