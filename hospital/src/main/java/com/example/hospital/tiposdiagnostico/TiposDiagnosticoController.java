package com.example.hospital.tiposdiagnostico;

import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import com.example.hospital.tiposdiagnostico.dto.TiposDiagnosticoDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tipos-diagnostico")
@Tag(name = "Tipos de diagnóstico", description = "Catálogo de tipos de diagnóstico (presuntivo, definitivo, etc.)")
@ApiResponses({
        @ApiResponse(responseCode = "401", description = "Token ausente, inválido o expirado"),
        @ApiResponse(responseCode = "403", description = "El rol del usuario no tiene permiso")
})
public class TiposDiagnosticoController {

    private final TiposDiagnosticoService tiposDiagnosticoService;

    public TiposDiagnosticoController(TiposDiagnosticoService tiposDiagnosticoService) {
        this.tiposDiagnosticoService = tiposDiagnosticoService;
    }

    @GetMapping
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Consulta exitosa")
    })
    @Operation(summary = "Listar tipos de diagnóstico", description = "Devuelve el catálogo de tipos de diagnóstico.\n\nAcceso: cualquier usuario autenticado.")
    public List<TiposDiagnosticoDTO> findAll() {
        return tiposDiagnosticoService.findAll();
    }

    @GetMapping("/{id}")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Consulta exitosa"),
            @ApiResponse(responseCode = "400", description = "Algún parámetro de la ruta o de la consulta tiene un formato inválido (por ejemplo un id no numérico o una fecha mal escrita)"),
            @ApiResponse(responseCode = "404", description = "No existe el tipo de diagnóstico indicado")
    })
    @Operation(summary = "Obtener tipo de diagnóstico por ID", description = "Devuelve el detalle de un tipo de diagnóstico.\n\nAcceso: cualquier usuario autenticado.")
    public TiposDiagnosticoDTO findById(@Parameter(description = "Id del registro en «Tipos de diagnóstico» (debe existir).", example = "1") @PathVariable Long id) {
        return tiposDiagnosticoService.findById(id);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Creado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos del cuerpo inválidos: ver `detalles` con el mensaje de cada campo"),
            @ApiResponse(responseCode = "409", description = "Ya existe un tipo de diagnóstico con la descripción"),
            @ApiResponse(responseCode = "422", description = "La descripción del tipo de diagnóstico es obligatoria")
    })
    @Operation(summary = "Registrar tipo de diagnóstico", description = "Agrega un tipo de diagnóstico al catálogo.\n\nRoles permitidos: ADMIN.")
    public ResponseEntity<TiposDiagnosticoDTO> crear(@Valid @RequestBody TiposDiagnosticoDTO dto) {
        TiposDiagnosticoDTO creado = tiposDiagnosticoService.crear(dto);
        return new ResponseEntity<>(creado, HttpStatus.CREATED);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Actualizado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos del cuerpo inválidos: ver `detalles` con el mensaje de cada campo; Algún parámetro de la ruta o de la consulta tiene un formato inválido (por ejemplo un id no numérico o una fecha mal escrita)"),
            @ApiResponse(responseCode = "404", description = "No existe el tipo de diagnóstico indicado"),
            @ApiResponse(responseCode = "409", description = "Ya existe otro tipo de diagnóstico con la descripción"),
            @ApiResponse(responseCode = "422", description = "No se permite renombrar el tipo de diagnóstico base")
    })
    @Operation(summary = "Actualizar tipo de diagnóstico", description = "Modifica un tipo de diagnóstico.\n\nRoles permitidos: ADMIN.")
    public ResponseEntity<TiposDiagnosticoDTO> actualizar(
            @Parameter(description = "Id del registro en «Tipos de diagnóstico» (debe existir).", example = "1") @PathVariable Long id,
            @Valid @RequestBody TiposDiagnosticoDTO dto) {
        TiposDiagnosticoDTO actualizado = tiposDiagnosticoService.actualizar(dto, id);
        return ResponseEntity.ok(actualizado);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Operación realizada sin contenido en la respuesta"),
            @ApiResponse(responseCode = "400", description = "Algún parámetro de la ruta o de la consulta tiene un formato inválido (por ejemplo un id no numérico o una fecha mal escrita)"),
            @ApiResponse(responseCode = "404", description = "No existe el tipo de diagnóstico indicado"),
            @ApiResponse(responseCode = "409", description = "No se puede eliminar el tipo de diagnóstico porque está asociado a uno o más diagnósticos médicos"),
            @ApiResponse(responseCode = "422", description = "No se puede eliminar el tipo de diagnóstico base")
    })
    @Operation(summary = "Eliminar tipo de diagnóstico", description = "Elimina un tipo. No se permite si hay diagnósticos que lo usan.\n\nRoles permitidos: ADMIN.")
    public ResponseEntity<Void> eliminar(@Parameter(description = "Id del registro en «Tipos de diagnóstico» (debe existir).", example = "1") @PathVariable Long id) {
        tiposDiagnosticoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
