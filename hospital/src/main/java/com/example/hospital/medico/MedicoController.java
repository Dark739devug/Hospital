package com.example.hospital.medico;

import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import com.example.hospital.medico.dto.MedicoDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/medico")
@Tag(name = "Médicos", description = "Personal médico del hospital")
@ApiResponses({
        @ApiResponse(responseCode = "401", description = "Token ausente, inválido o expirado"),
        @ApiResponse(responseCode = "403", description = "El rol del usuario no tiene permiso")
})
public class MedicoController {

    private final MedicoService medicoService;

    public MedicoController(MedicoService medicoService) {
        this.medicoService = medicoService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO', 'ENFERMERO', 'RECEPCIONISTA')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Consulta exitosa"),
            @ApiResponse(responseCode = "400", description = "Algún parámetro de la ruta o de la consulta tiene un formato inválido (por ejemplo un id no numérico o una fecha mal escrita)")
    })
    @Operation(summary = "Listar / buscar médicos", description = "Devuelve los médicos. Filtros opcionales y COMBINABLES:\n- `criterio`: busca por nombre, apellidos o número de colegiado (sin distinguir mayúsculas)\n- `soloActivos=true`: solo médicos activos (por defecto se incluyen también los inactivos)\n\nRoles permitidos: ADMIN, MEDICO, ENFERMERO, RECEPCIONISTA.")
    public List<MedicoDTO> findAll(
            @Parameter(description = "Si es true devuelve solo los registros activos; por defecto false (todos).", example = "true") @RequestParam(required = false, defaultValue = "false") boolean soloActivos,
            @Parameter(description = "Texto a buscar (nombre comercial, genérico o laboratorio).", example = "Paracetamol") @RequestParam(required = false) String criterio) {
        return medicoService.buscar(criterio, soloActivos);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO', 'ENFERMERO', 'RECEPCIONISTA')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Consulta exitosa"),
            @ApiResponse(responseCode = "400", description = "Algún parámetro de la ruta o de la consulta tiene un formato inválido (por ejemplo un id no numérico o una fecha mal escrita)"),
            @ApiResponse(responseCode = "422", description = "No existe el médico indicado")
    })
    @Operation(summary = "Obtener médico por ID", description = "Devuelve los datos de un médico. Responde 404 si no existe.\n\nRoles permitidos: ADMIN, MEDICO, ENFERMERO, RECEPCIONISTA.")
    public MedicoDTO findById(@Parameter(description = "Id del registro en «Médicos» (debe existir).", example = "1") @PathVariable Long id) {
        return medicoService.findById(id);
    }

    @GetMapping("/colegiado/{noColegiado}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO', 'ENFERMERO', 'RECEPCIONISTA')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Consulta exitosa"),
            @ApiResponse(responseCode = "422", description = "El número de colegiado es obligatorio; No se encontró el médico con número de colegiado")
    })
    @Operation(summary = "Buscar médico por colegiado", description = "Devuelve el médico con el número de colegiado indicado. Responde 404 si no existe.\n\nRoles permitidos: ADMIN, MEDICO, ENFERMERO, RECEPCIONISTA.")
    public MedicoDTO findByNoColegiado(@Parameter(description = "Número de colegiado del médico.", example = "12345") @PathVariable String noColegiado) {
        return medicoService.findByNoColegiado(noColegiado);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Creado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos del cuerpo inválidos: ver `detalles` con el mensaje de cada campo"),
            @ApiResponse(responseCode = "409", description = "Ya existe un médico registrado con el número de colegiado; Ya existe otro médico registrado con el número de colegiado; Ya existe un médico registrado con el correo electrónico; Ya existe otro médico registrado con el correo electrónico"),
            @ApiResponse(responseCode = "422", description = "El número de colegiado del médico es obligatorio; El nombre del médico es obligatorio; El apellido paterno del médico es obligatorio; El formato del correo electrónico no es válido; El teléfono debe contener entre 7 y 20 caracteres")
    })
    @Operation(summary = "Registrar médico", description = "Da de alta a un nuevo médico. El número de colegiado debe ser único.\n\nRoles permitidos: ADMIN.")
    public ResponseEntity<MedicoDTO> save(@Valid @RequestBody MedicoDTO dto) {
        MedicoDTO creado = medicoService.crear(dto);
        return new ResponseEntity<>(creado, HttpStatus.CREATED);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Actualizado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos del cuerpo inválidos: ver `detalles` con el mensaje de cada campo; Algún parámetro de la ruta o de la consulta tiene un formato inválido (por ejemplo un id no numérico o una fecha mal escrita)"),
            @ApiResponse(responseCode = "409", description = "Ya existe un médico registrado con el número de colegiado; Ya existe otro médico registrado con el número de colegiado; Ya existe un médico registrado con el correo electrónico; Ya existe otro médico registrado con el correo electrónico"),
            @ApiResponse(responseCode = "422", description = "No se encontró el médico indicado; No se puede desactivar o eliminar el médico porque tiene citas médicas activas programadas; No se puede desactivar o eliminar el médico porque tiene hospitalizaciones activas a su cargo; No se puede desactivar o eliminar el médico porque tiene atenciones de emergencia activas asignadas; No existe el médico indicado")
    })
    @Operation(summary = "Actualizar médico", description = "Modifica los datos de un médico.\n\nRoles permitidos: ADMIN.")
    public ResponseEntity<MedicoDTO> update(
            @Parameter(description = "Id del registro en «Médicos» (debe existir).", example = "1") @PathVariable Long id,
            @Valid @RequestBody MedicoDTO dto) {
        MedicoDTO actualizado = medicoService.actualizar(dto, id);
        return ResponseEntity.ok(actualizado);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/estado")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Actualizado correctamente"),
            @ApiResponse(responseCode = "400", description = "Algún parámetro de la ruta o de la consulta tiene un formato inválido (por ejemplo un id no numérico o una fecha mal escrita)"),
            @ApiResponse(responseCode = "422", description = "No se encontró el médico indicado; No se puede desactivar o eliminar el médico porque tiene citas médicas activas programadas; No se puede desactivar o eliminar el médico porque tiene hospitalizaciones activas a su cargo; No se puede desactivar o eliminar el médico porque tiene atenciones de emergencia activas asignadas; No existe el médico indicado")
    })
    @Operation(summary = "Activar / desactivar médico", description = "Activa (`nuevoEstado=true`) o desactiva (`false`) a un médico sin borrarlo.\n\nRoles permitidos: ADMIN.")
    public ResponseEntity<MedicoDTO> cambiarEstado(
            @Parameter(description = "Id del registro en «Médicos» (debe existir).", example = "1") @PathVariable Long id,
            @Parameter(description = "true activa y false desactiva.", example = "true") @RequestParam Boolean nuevoEstado) {
        MedicoDTO actualizado = medicoService.cambiarEstado(id, nuevoEstado);
        return ResponseEntity.ok(actualizado);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Operación realizada sin contenido en la respuesta"),
            @ApiResponse(responseCode = "400", description = "Algún parámetro de la ruta o de la consulta tiene un formato inválido (por ejemplo un id no numérico o una fecha mal escrita)"),
            @ApiResponse(responseCode = "422", description = "No se encontró el médico indicado; El médico ya se encuentra inactivo; No se puede desactivar o eliminar el médico porque tiene citas médicas activas programadas; No se puede desactivar o eliminar el médico porque tiene hospitalizaciones activas a su cargo; No se puede desactivar o eliminar el médico porque tiene atenciones de emergencia activas asignadas")
    })
    @Operation(summary = "Eliminar médico", description = "Desactiva a un médico. No se permite si tiene citas activas u hospitalizaciones a su cargo.\n\nRoles permitidos: ADMIN.")
    public ResponseEntity<Void> delete(@Parameter(description = "Id del registro en «Médicos» (debe existir).", example = "1") @PathVariable Long id) {
        medicoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
