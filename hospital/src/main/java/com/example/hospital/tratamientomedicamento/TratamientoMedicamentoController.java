package com.example.hospital.tratamientomedicamento;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import com.example.hospital.tratamientomedicamento.dto.TratamientoMedicamentoDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tratamiento-medicamento")
@Tag(name = "Tratamiento – medicamentos", description = "Medicamentos recetados dentro de cada tratamiento")
@ApiResponses({
        @ApiResponse(responseCode = "401", description = "Token ausente, inválido o expirado"),
        @ApiResponse(responseCode = "403", description = "El rol del usuario no tiene permiso")
})
public class TratamientoMedicamentoController {

    private final TratamientoMedicamentoService tratamientoMedicamentoService;

    public TratamientoMedicamentoController(TratamientoMedicamentoService tratamientoMedicamentoService) {
        this.tratamientoMedicamentoService = tratamientoMedicamentoService;
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO', 'FARMACEUTICO')")
    @GetMapping
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Consulta exitosa"),
            @ApiResponse(responseCode = "400", description = "Algún parámetro de la ruta o de la consulta tiene un formato inválido (por ejemplo un id no numérico o una fecha mal escrita)")
    })
    @Operation(summary = "Listar medicamentos recetados", description = "Devuelve las relaciones tratamiento–medicamento. Filtros opcionales y COMBINABLES: `idTratamiento` (medicamentos de ese tratamiento) e `idMedicamento` (tratamientos que incluyen ese medicamento).\n\nRoles permitidos: ADMIN, MEDICO, FARMACEUTICO.")
    public List<TratamientoMedicamentoDTO> findAll(
            @Parameter(description = "Id del tratamiento.", example = "1") @RequestParam(required = false) Long idTratamiento,
            @Parameter(description = "Id del medicamento.", example = "1") @RequestParam(required = false) Long idMedicamento) {
        return tratamientoMedicamentoService.buscar(idTratamiento, idMedicamento);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO', 'FARMACEUTICO')")
    @GetMapping("/{idMedicamento}/{idTratamiento}")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Consulta exitosa"),
            @ApiResponse(responseCode = "400", description = "Algún parámetro de la ruta o de la consulta tiene un formato inválido (por ejemplo un id no numérico o una fecha mal escrita)"),
            @ApiResponse(responseCode = "404", description = "No existe la relación tratamiento-medicamento")
    })
    @Operation(summary = "Obtener receta", description = "Devuelve un medicamento recetado en un tratamiento por su llave compuesta (idMedicamento + idTratamiento).\n\nRoles permitidos: ADMIN, MEDICO, FARMACEUTICO.")
    public TratamientoMedicamentoDTO findById(@Parameter(description = "Id del medicamento.", example = "1") @PathVariable Long idMedicamento,
                                              @Parameter(description = "Id del tratamiento.", example = "1") @PathVariable Long idTratamiento) {
        return tratamientoMedicamentoService.findById(idMedicamento, idTratamiento);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO')")
    @PostMapping
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Creado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos del cuerpo inválidos: ver `detalles` con el mensaje de cada campo"),
            @ApiResponse(responseCode = "409", description = "El medicamento ya está asignado a este tratamiento"),
            @ApiResponse(responseCode = "422", description = "No existe el tratamiento indicado; No se puede asignar un medicamento a un tratamiento inactivo; No existe el medicamento indicado; No se puede asignar un medicamento inactivo a un tratamiento")
    })
    @Operation(summary = "Recetar medicamento", description = "Agrega un medicamento a un tratamiento. El tratamiento y el medicamento deben existir y estar activos (422 en caso contrario); si la relación ya existe responde 409. Recetar NO descuenta ni exige stock (prescribir no es dispensar): un medicamento sin stock puede recetarse.\n\nErrores: 400 (ids ausentes, con `detalles`), 409 (duplicado), 422 (inexistente o inactivo).\n\nRoles permitidos: ADMIN, MEDICO.")
    public ResponseEntity<TratamientoMedicamentoDTO> crear(@Valid @RequestBody TratamientoMedicamentoDTO dto) {
        return new ResponseEntity<>(tratamientoMedicamentoService.crear(dto), HttpStatus.CREATED);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO')")
    @DeleteMapping("/{idMedicamento}/{idTratamiento}")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Operación realizada sin contenido en la respuesta"),
            @ApiResponse(responseCode = "400", description = "Algún parámetro de la ruta o de la consulta tiene un formato inválido (por ejemplo un id no numérico o una fecha mal escrita)"),
            @ApiResponse(responseCode = "404", description = "No existe la relación tratamiento-medicamento indicada")
    })
    @Operation(summary = "Quitar medicamento del tratamiento", description = "Elimina un medicamento de un tratamiento (borrado físico de la relación).\n\nErrores: 404 si la relación no existe.\n\nRoles permitidos: ADMIN, MEDICO.")
    public ResponseEntity<Void> eliminar(@Parameter(description = "Id del medicamento.", example = "1") @PathVariable Long idMedicamento,
                                         @Parameter(description = "Id del tratamiento.", example = "1") @PathVariable Long idTratamiento) {
        tratamientoMedicamentoService.eliminar(idMedicamento, idTratamiento);
        return ResponseEntity.noContent().build();
    }
}
