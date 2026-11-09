package com.example.hospital.habitacion;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import com.example.hospital.habitacion.dto.HabitacionDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/habitacion")
@Tag(name = "Habitaciones", description = "Habitaciones y camas del hospital")
@ApiResponses({
        @ApiResponse(responseCode = "401", description = "Token ausente, inválido o expirado"),
        @ApiResponse(responseCode = "403", description = "El rol del usuario no tiene permiso")
})
public class HabitacionController {

    private final HabitacionService habitacionService;

    public HabitacionController(HabitacionService habitacionService) {
        this.habitacionService = habitacionService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO', 'ENFERMERO', 'RECEPCIONISTA')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Consulta exitosa"),
            @ApiResponse(responseCode = "400", description = "Algún parámetro de la ruta o de la consulta tiene un formato inválido (por ejemplo un id no numérico o una fecha mal escrita)")
    })
    @Operation(summary = "Listar habitaciones", description = "Devuelve las habitaciones. Todos los filtros son opcionales y se aplican juntos:\n- `piso`\n- `idTipoHabitacion`\n- `idEstadoHabitacion`\n- `soloActivas=true`: solo habitaciones activas\n- `soloDisponibles=true`: solo habitaciones activas en estado Disponible\n\nRoles permitidos: ADMIN, MEDICO, ENFERMERO, RECEPCIONISTA.")
    public List<HabitacionDTO> findAll(
            @Parameter(description = "Si es true devuelve solo los registros activos; por defecto false (todos).", example = "true") @RequestParam(required = false, defaultValue = "false") boolean soloActivas,
            @Parameter(description = "Si es true devuelve solo habitaciones con cupo disponible.", example = "true") @RequestParam(required = false, defaultValue = "false") boolean soloDisponibles,
            @Parameter(description = "Número de piso.", example = "3") @RequestParam(required = false) Integer piso,
            @Parameter(description = "Id del tipo de habitación.", example = "1") @RequestParam(required = false) Long idTipoHabitacion,
            @Parameter(description = "Id del estado de habitación (1 = Disponible, 2 = Ocupada, 3 = Mantenimiento).", example = "1") @RequestParam(required = false) Long idEstadoHabitacion) {
        return habitacionService.buscar(piso, idTipoHabitacion, idEstadoHabitacion, soloActivas, soloDisponibles);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO', 'ENFERMERO', 'RECEPCIONISTA')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Consulta exitosa"),
            @ApiResponse(responseCode = "400", description = "Algún parámetro de la ruta o de la consulta tiene un formato inválido (por ejemplo un id no numérico o una fecha mal escrita)"),
            @ApiResponse(responseCode = "404", description = "No existe la habitación indicada")
    })
    @Operation(summary = "Obtener habitación por ID", description = "Devuelve el detalle de una habitación (número, piso, capacidad, tipo y estado).\n\nRoles permitidos: ADMIN, MEDICO, ENFERMERO, RECEPCIONISTA.")
    public HabitacionDTO findById(@Parameter(description = "Id del registro en «Habitaciones» (debe existir).", example = "1") @PathVariable Long id) {
        return habitacionService.findById(id);
    }

    @GetMapping("/numero/{numero}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO', 'ENFERMERO', 'RECEPCIONISTA')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Consulta exitosa"),
            @ApiResponse(responseCode = "404", description = "No existe la habitación con número"),
            @ApiResponse(responseCode = "422", description = "El número de habitación es obligatorio")
    })
    @Operation(summary = "Buscar habitación por número", description = "Devuelve la habitación con el número indicado (p. ej. `203-A`). Responde 404 si no existe.\n\nRoles permitidos: ADMIN, MEDICO, ENFERMERO, RECEPCIONISTA.")
    public HabitacionDTO findByNumero(@Parameter(description = "Número de la habitación.", example = "301") @PathVariable String numero) {
        return habitacionService.findByNumero(numero);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Creado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos del cuerpo inválidos: ver `detalles` con el mensaje de cada campo"),
            @ApiResponse(responseCode = "409", description = "Ya existe una habitación registrada con el número; Ya existe otra habitación registrada con el número"),
            @ApiResponse(responseCode = "422", description = "El tipo de habitación indicado no existe; El tipo de habitación seleccionado se encuentra inactivo; El estado de habitación indicado no existe; El estado de habitación seleccionado se encuentra inactivo; No se puede marcar como Disponible una habitación llena")
    })
    @Operation(summary = "Registrar habitación", description = "Crea una nueva habitación. El número debe ser único.\n\nRoles permitidos: ADMIN.")
    public ResponseEntity<HabitacionDTO> save(@Valid @RequestBody HabitacionDTO dto) {
        HabitacionDTO creada = habitacionService.crear(dto);
        return new ResponseEntity<>(creada, HttpStatus.CREATED);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Actualizado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos del cuerpo inválidos: ver `detalles` con el mensaje de cada campo; Algún parámetro de la ruta o de la consulta tiene un formato inválido (por ejemplo un id no numérico o una fecha mal escrita)"),
            @ApiResponse(responseCode = "404", description = "No existe la habitación indicada"),
            @ApiResponse(responseCode = "409", description = "Ya existe una habitación registrada con el número; Ya existe otra habitación registrada con el número"),
            @ApiResponse(responseCode = "422", description = "El tipo de habitación indicado no existe; El tipo de habitación seleccionado se encuentra inactivo; El estado de habitación indicado no existe; El estado de habitación seleccionado se encuentra inactivo; No se puede marcar como Disponible una habitación llena")
    })
    @Operation(summary = "Actualizar habitación", description = "Modifica los datos de una habitación.\n\nRoles permitidos: ADMIN.")
    public ResponseEntity<HabitacionDTO> update(
            @Parameter(description = "Id del registro en «Habitaciones» (debe existir).", example = "1") @PathVariable Long id,
            @Valid @RequestBody HabitacionDTO dto) {
        HabitacionDTO actualizada = habitacionService.actualizar(dto, id);
        return ResponseEntity.ok(actualizada);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/estado/{nuevoEstadoId}")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Actualizado correctamente"),
            @ApiResponse(responseCode = "400", description = "Algún parámetro de la ruta o de la consulta tiene un formato inválido (por ejemplo un id no numérico o una fecha mal escrita)"),
            @ApiResponse(responseCode = "404", description = "No existe la habitación indicada"),
            @ApiResponse(responseCode = "422", description = "El estado de habitación indicado no existe; El estado de habitación seleccionado se encuentra inactivo; No se puede marcar como Disponible una habitación llena; No se puede marcar como Ocupada una habitación sin pacientes hospitalizados; No se puede pasar a Mantenimiento una habitación con pacientes hospitalizados")
    })
    @Operation(summary = "Cambiar estado de habitación", description = "Cambia el estado de la habitación (Disponible, Ocupada, Mantenimiento). Responde 422 si no es coherente con la ocupación real: no pasa a Disponible si está llena, no pasa a Ocupada si no tiene pacientes y no pasa a Mantenimiento si tiene pacientes hospitalizados. La misma regla se aplica en PUT cuando cambia el estado.\n\nRoles permitidos: ADMIN.")
    public ResponseEntity<HabitacionDTO> cambiarEstado(
            @Parameter(description = "Id del registro en «Habitaciones» (debe existir).", example = "1") @PathVariable Long id,
            @Parameter(description = "Id del estado al que se pasa.", example = "2") @PathVariable Long nuevoEstadoId) {
        HabitacionDTO actualizada = habitacionService.cambiarEstado(id, nuevoEstadoId);
        return ResponseEntity.ok(actualizada);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Operación realizada sin contenido en la respuesta"),
            @ApiResponse(responseCode = "400", description = "Algún parámetro de la ruta o de la consulta tiene un formato inválido (por ejemplo un id no numérico o una fecha mal escrita)"),
            @ApiResponse(responseCode = "404", description = "No existe la habitación indicada"),
            @ApiResponse(responseCode = "422", description = "No se puede desactivar la habitación porque tiene pacientes actualmente hospitalizados en ella; No se puede desactivar la habitación porque tiene ingresos de emergencia activos asignados")
    })
    @Operation(summary = "Eliminar habitación", description = "Desactiva una habitación. No se permite si tiene pacientes hospitalizados o en emergencia.\n\nRoles permitidos: ADMIN.")
    public ResponseEntity<Void> delete(@Parameter(description = "Id del registro en «Habitaciones» (debe existir).", example = "1") @PathVariable Long id) {
        habitacionService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
