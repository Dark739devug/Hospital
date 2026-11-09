package com.example.hospital.triaje;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import com.example.hospital.triaje.dto.TriajeDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/triaje")
@Tag(name = "Triaje", description = "Niveles de triaje para clasificar la gravedad en emergencia")
@ApiResponses({
        @ApiResponse(responseCode = "401", description = "Token ausente, inválido o expirado"),
        @ApiResponse(responseCode = "403", description = "El rol del usuario no tiene permiso")
})
public class TriajeController {

    private final TriajeService triajeService;

    public TriajeController(TriajeService triajeService) {
        this.triajeService = triajeService;
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO', 'ENFERMERO', 'RECEPCIONISTA')")
    @GetMapping
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Consulta exitosa")
    })
    @Operation(summary = "Listar niveles de triaje", description = "Devuelve los niveles de triaje (p. ej. Rojo, Naranja, Amarillo, Verde, Azul).\n\nRoles permitidos: ADMIN, MEDICO, ENFERMERO, RECEPCIONISTA.")
    public List<TriajeDTO> findAll() {
        return triajeService.findAll();
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO', 'ENFERMERO', 'RECEPCIONISTA')")
    @GetMapping("/{id}")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Consulta exitosa"),
            @ApiResponse(responseCode = "400", description = "Algún parámetro de la ruta o de la consulta tiene un formato inválido (por ejemplo un id no numérico o una fecha mal escrita)"),
            @ApiResponse(responseCode = "404", description = "No existe el triaje indicado"),
            @ApiResponse(responseCode = "422", description = "Regla de negocio incumplida")
    })
    @Operation(summary = "Obtener triaje por ID", description = "Devuelve el detalle de un nivel de triaje.\n\nRoles permitidos: ADMIN, MEDICO, ENFERMERO, RECEPCIONISTA.")
    public TriajeDTO findById(@Parameter(description = "Id del registro en «Triaje» (debe existir).", example = "1") @PathVariable Long id) {
        return triajeService.findById(id);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO', 'ENFERMERO')")
    @PostMapping
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Creado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos del cuerpo inválidos: ver `detalles` con el mensaje de cada campo"),
            @ApiResponse(responseCode = "409", description = "Ya existe un triaje con el nivel"),
            @ApiResponse(responseCode = "422", description = "Regla de negocio incumplida")
    })
    @Operation(summary = "Registrar nivel de triaje", description = "Agrega un nivel de triaje. El nombre del nivel debe ser único.\n\nRoles permitidos: ADMIN, MEDICO, ENFERMERO.")
    public ResponseEntity<TriajeDTO> crear(@Valid @RequestBody TriajeDTO dto) {
        return new ResponseEntity<>(triajeService.crear(dto), HttpStatus.CREATED);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO', 'ENFERMERO')")
    @PutMapping("/{id}")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Actualizado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos del cuerpo inválidos: ver `detalles` con el mensaje de cada campo; Algún parámetro de la ruta o de la consulta tiene un formato inválido (por ejemplo un id no numérico o una fecha mal escrita)"),
            @ApiResponse(responseCode = "404", description = "No existe el triaje indicado"),
            @ApiResponse(responseCode = "409", description = "Ya existe un triaje con el nivel"),
            @ApiResponse(responseCode = "422", description = "Regla de negocio incumplida")
    })
    @Operation(summary = "Actualizar nivel de triaje", description = "Modifica un nivel de triaje.\n\nRoles permitidos: ADMIN, MEDICO, ENFERMERO.")
    public ResponseEntity<TriajeDTO> actualizar(@Parameter(description = "Id del registro en «Triaje» (debe existir).", example = "1") @PathVariable Long id, @Valid @RequestBody TriajeDTO dto) {
        return ResponseEntity.ok(triajeService.actualizar(dto, id));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Operación realizada sin contenido en la respuesta"),
            @ApiResponse(responseCode = "400", description = "Algún parámetro de la ruta o de la consulta tiene un formato inválido (por ejemplo un id no numérico o una fecha mal escrita)"),
            @ApiResponse(responseCode = "404", description = "No existe el triaje indicado"),
            @ApiResponse(responseCode = "409", description = "No se puede desactivar el nivel de triaje porque hay ingresos a emergencia activos que lo usan"),
            @ApiResponse(responseCode = "422", description = "Regla de negocio incumplida")
    })
    @Operation(summary = "Eliminar nivel de triaje", description = "Desactiva un nivel de triaje. Responde 409 si hay ingresos a emergencia activos que lo usan.\n\nRoles permitidos: ADMIN.")
    public ResponseEntity<Void> eliminar(@Parameter(description = "Id del registro en «Triaje» (debe existir).", example = "1") @PathVariable Long id) {
        triajeService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
