package com.example.hospital.departamentos;

import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import com.example.hospital.departamentos.dto.DepartamentosDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/departamentos")
@Tag(name = "Departamentos", description = "Departamentos o áreas del hospital")
@ApiResponses({
        @ApiResponse(responseCode = "401", description = "Token ausente, inválido o expirado"),
        @ApiResponse(responseCode = "403", description = "El rol del usuario no tiene permiso")
})
public class DepartamentosController {

    private final DepartamentosService departamentosService;

    public DepartamentosController(DepartamentosService departamentosService) {
        this.departamentosService = departamentosService;
    }

    @GetMapping
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Consulta exitosa")
    })
    @Operation(summary = "Listar departamentos", description = "Devuelve todos los departamentos del hospital (p. ej. Pediatría, Cirugía).\n\nAcceso: cualquier usuario autenticado.")
    public List<DepartamentosDTO> findAll() {
        return departamentosService.findAll();
    }

    @GetMapping("/{id}")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Consulta exitosa"),
            @ApiResponse(responseCode = "400", description = "Algún parámetro de la ruta o de la consulta tiene un formato inválido (por ejemplo un id no numérico o una fecha mal escrita)"),
            @ApiResponse(responseCode = "404", description = "No existe el departamento indicado")
    })
    @Operation(summary = "Obtener departamento por ID", description = "Devuelve el detalle de un departamento. Responde 404 si no existe.\n\nAcceso: cualquier usuario autenticado.")
    public DepartamentosDTO findById(@Parameter(description = "Id del registro en «Departamentos» (debe existir).", example = "1") @PathVariable Long id) {
        return departamentosService.findById(id);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Creado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos del cuerpo inválidos: ver `detalles` con el mensaje de cada campo"),
            @ApiResponse(responseCode = "409", description = "Ya existe un departamento con el nombre"),
            @ApiResponse(responseCode = "422", description = "El nombre del departamento es obligatorio")
    })
    @Operation(summary = "Registrar departamento", description = "Crea un nuevo departamento.\n\nRoles permitidos: ADMIN.")
    public ResponseEntity<DepartamentosDTO> save(@Valid @RequestBody DepartamentosDTO dto) {
        DepartamentosDTO creado = departamentosService.crear(dto);
        return new ResponseEntity<>(creado, HttpStatus.CREATED);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Actualizado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos del cuerpo inválidos: ver `detalles` con el mensaje de cada campo; Algún parámetro de la ruta o de la consulta tiene un formato inválido (por ejemplo un id no numérico o una fecha mal escrita)"),
            @ApiResponse(responseCode = "404", description = "No existe el departamento indicado"),
            @ApiResponse(responseCode = "409", description = "Ya existe un departamento con el nombre"),
            @ApiResponse(responseCode = "422", description = "El nombre del departamento es obligatorio")
    })
    @Operation(summary = "Actualizar departamento", description = "Modifica los datos de un departamento.\n\nRoles permitidos: ADMIN.")
    public ResponseEntity<DepartamentosDTO> update(
            @Parameter(description = "Id del registro en «Departamentos» (debe existir).", example = "1") @PathVariable Long id,
            @Valid @RequestBody DepartamentosDTO dto) {
        DepartamentosDTO actualizado = departamentosService.actualizar(dto, id);
        return ResponseEntity.ok(actualizado);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Operación realizada sin contenido en la respuesta"),
            @ApiResponse(responseCode = "400", description = "Algún parámetro de la ruta o de la consulta tiene un formato inválido (por ejemplo un id no numérico o una fecha mal escrita)"),
            @ApiResponse(responseCode = "404", description = "No existe el departamento indicado"),
            @ApiResponse(responseCode = "409", description = "No se puede eliminar el departamento porque tiene clínicas activas asociadas; No se puede eliminar el departamento porque tiene enfermeros activos asignados"),
            @ApiResponse(responseCode = "422", description = "Regla de negocio incumplida")
    })
    @Operation(summary = "Eliminar departamento", description = "Desactiva un departamento. No se permite si tiene clínicas asociadas.\n\nRoles permitidos: ADMIN.")
    public ResponseEntity<Void> delete(@Parameter(description = "Id del registro en «Departamentos» (debe existir).", example = "1") @PathVariable Long id) {
        departamentosService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
