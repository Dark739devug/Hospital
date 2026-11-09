package com.example.hospital.nivelesurgencia;

import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import com.example.hospital.nivelesurgencia.dto.NivelesUrgenciaDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/niveles-urgencia")
@Tag(name = "Niveles de urgencia", description = "Catálogo de niveles de urgencia para clasificar pacientes")
@ApiResponses({
        @ApiResponse(responseCode = "401", description = "Token ausente, inválido o expirado"),
        @ApiResponse(responseCode = "403", description = "El rol del usuario no tiene permiso")
})
public class NivelesUrgenciaController {

    private final NivelesUrgenciaService nivelesUrgenciaService;

    public NivelesUrgenciaController(NivelesUrgenciaService nivelesUrgenciaService) {
        this.nivelesUrgenciaService = nivelesUrgenciaService;
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO', 'ENFERMERO', 'RECEPCIONISTA')")
    @GetMapping
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Consulta exitosa")
    })
    @Operation(summary = "Listar niveles de urgencia", description = "Devuelve los niveles de urgencia del catálogo.\n\nRoles permitidos: ADMIN, MEDICO, ENFERMERO, RECEPCIONISTA.")
    public List<NivelesUrgenciaDTO> findAll() {
        return nivelesUrgenciaService.findAll();
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO', 'ENFERMERO', 'RECEPCIONISTA')")
    @GetMapping("/{id}")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Consulta exitosa"),
            @ApiResponse(responseCode = "400", description = "Algún parámetro de la ruta o de la consulta tiene un formato inválido (por ejemplo un id no numérico o una fecha mal escrita)"),
            @ApiResponse(responseCode = "404", description = "No existe el nivel de urgencia indicado"),
            @ApiResponse(responseCode = "422", description = "Regla de negocio incumplida")
    })
    @Operation(summary = "Obtener nivel por ID", description = "Devuelve el detalle de un nivel de urgencia.\n\nRoles permitidos: ADMIN, MEDICO, ENFERMERO, RECEPCIONISTA.")
    public NivelesUrgenciaDTO findById(@Parameter(description = "Id del registro en «Niveles de urgencia» (debe existir).", example = "1") @PathVariable Long id) {
        return nivelesUrgenciaService.findById(id);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Creado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos del cuerpo inválidos: ver `detalles` con el mensaje de cada campo"),
            @ApiResponse(responseCode = "409", description = "Ya existe un nivel de urgencia con el código"),
            @ApiResponse(responseCode = "422", description = "El código debe tener el formato N1, N2, N3..; La descripción del nivel de urgencia es obligatoria; El tiempo máximo de espera debe ser mayor o igual a 0")
    })
    @Operation(summary = "Registrar nivel de urgencia", description = "Agrega un nuevo nivel de urgencia.\n\nRoles permitidos: ADMIN.")
    public ResponseEntity<NivelesUrgenciaDTO> crear(@Valid @RequestBody NivelesUrgenciaDTO dto) {
        return new ResponseEntity<>(nivelesUrgenciaService.crear(dto), HttpStatus.CREATED);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Actualizado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos del cuerpo inválidos: ver `detalles` con el mensaje de cada campo; Algún parámetro de la ruta o de la consulta tiene un formato inválido (por ejemplo un id no numérico o una fecha mal escrita)"),
            @ApiResponse(responseCode = "404", description = "No existe el nivel de urgencia indicado"),
            @ApiResponse(responseCode = "409", description = "Ya existe un nivel de urgencia con el código"),
            @ApiResponse(responseCode = "422", description = "El código debe tener el formato N1, N2, N3..; La descripción del nivel de urgencia es obligatoria; El tiempo máximo de espera debe ser mayor o igual a 0")
    })
    @Operation(summary = "Actualizar nivel de urgencia", description = "Modifica un nivel de urgencia.\n\nRoles permitidos: ADMIN.")
    public ResponseEntity<NivelesUrgenciaDTO> actualizar(@Parameter(description = "Id del registro en «Niveles de urgencia» (debe existir).", example = "1") @PathVariable Long id, @Valid @RequestBody NivelesUrgenciaDTO dto) {
        return ResponseEntity.ok(nivelesUrgenciaService.actualizar(dto, id));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Operación realizada sin contenido en la respuesta"),
            @ApiResponse(responseCode = "400", description = "Algún parámetro de la ruta o de la consulta tiene un formato inválido (por ejemplo un id no numérico o una fecha mal escrita)"),
            @ApiResponse(responseCode = "404", description = "No existe el nivel de urgencia indicado"),
            @ApiResponse(responseCode = "422", description = "Regla de negocio incumplida")
    })
    @Operation(summary = "Eliminar nivel de urgencia", description = "Desactiva un nivel de urgencia.\n\nRoles permitidos: ADMIN.")
    public ResponseEntity<Void> eliminar(@Parameter(description = "Id del registro en «Niveles de urgencia» (debe existir).", example = "1") @PathVariable Long id) {
        nivelesUrgenciaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
