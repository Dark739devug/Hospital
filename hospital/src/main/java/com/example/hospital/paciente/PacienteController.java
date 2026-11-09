package com.example.hospital.paciente;

import io.swagger.v3.oas.annotations.Parameter;
import com.example.hospital.paciente.dto.PacienteRequest;
import com.example.hospital.paciente.dto.PacienteResponse;
import com.example.hospital.paciente.dto.PacienteResumenDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/paciente")
@RequiredArgsConstructor
@Tag(name = "Pacientes", description = "Gestión de pacientes del hospital")
@ApiResponses({
        @ApiResponse(responseCode = "401", description = "Token ausente, inválido o expirado"),
        @ApiResponse(responseCode = "403", description = "El rol no tiene permiso")
})
public class PacienteController {

    private final PacienteService service;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MEDICO','ENFERMERO','RECEPCIONISTA')")
    @Operation(summary = "Listar / buscar pacientes", description = "Devuelve los pacientes activos. Parámetros opcionales:\n- `q`: busca por nombre, apellido paterno, apellido materno o DPI (coincidencia parcial, sin distinguir mayúsculas)\n- `todos=true`: incluye también los pacientes inactivos (también cuando se usa junto con `q`)\n\nRoles permitidos: ADMIN, MEDICO, ENFERMERO, RECEPCIONISTA.")
    @ApiResponse(responseCode = "200", description = "Lista de pacientes")
    public List<PacienteResponse> listar(
            @Parameter(description = "Si es true incluye también a los pacientes inactivos; por defecto false.", example = "false") @RequestParam(required = false, defaultValue = "false") boolean todos,
            @Parameter(description = "Texto a buscar (nombre, apellido o DPI).", example = "García") @RequestParam(required = false) String q) {
        if (q != null && !q.trim().isEmpty()) {
            return service.buscar(q, todos);
        }
        if (todos) {
            return service.listarTodos();
        }
        return service.listar();
    }

    // Mínimo privilegio para caja: CAJERO no puede leer el listado ni el detalle completo de pacientes, pero necesita identificar
    // al paciente al emitir facturas y cuentas. Estos endpoints devuelven SOLO id, nombre completo y DPI.
    // (Las rutas literales "/busqueda" y "/{id}/resumen" no chocan con "/{id}": Spring prefiere la coincidencia literal.)
    @GetMapping("/busqueda")
    @PreAuthorize("hasAnyRole('ADMIN','CAJERO','RECEPCIONISTA')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Consulta exitosa")
    })
    @Operation(summary = "Búsqueda mínima de pacientes (caja)", description = "Busca pacientes ACTIVOS por nombre, apellidos o DPI (parcial, sin distinguir mayúsculas) y devuelve solo `idPaciente`, `nombreCompleto`, `dpiPaciente` y `activo`. NO expone teléfono, correo, dirección ni datos clínicos. `q` con menos de 2 caracteres devuelve lista vacía (200). Máximo 20 resultados, ordenados por nombre.\n\nJustificación: mínimo privilegio; permite a CAJERO identificar al paciente en facturas y cuentas sin darle acceso al listado completo.\n\nRoles permitidos: ADMIN, CAJERO, RECEPCIONISTA.")
    public List<PacienteResumenDTO> busqueda(@Parameter(description = "Texto a buscar (nombre, apellido o DPI).", example = "García") @RequestParam(required = false) String q) {
        return service.buscarResumen(q);
    }

    @GetMapping("/{id}/resumen")
    @PreAuthorize("hasAnyRole('ADMIN','CAJERO','RECEPCIONISTA')")
    @Operation(summary = "Resumen mínimo de un paciente (caja)", description = "Devuelve solo `idPaciente`, `nombreCompleto`, `dpiPaciente` y `activo` de un paciente (activo o inactivo), para mostrar su nombre en facturas, cuentas y pagos. Responde 404 solo si no existe.\n\nJustificación: mínimo privilegio para caja.\n\nRoles permitidos: ADMIN, CAJERO, RECEPCIONISTA.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Resumen del paciente"),
            @ApiResponse(responseCode = "404", description = "El paciente no existe")
    })
    public PacienteResumenDTO resumen(@Parameter(description = "Id del registro en «Pacientes» (debe existir).", example = "1") @PathVariable Long id) {
        return service.resumen(id);
    }

    @GetMapping("/dpi/{dpi}")
    @PreAuthorize("hasAnyRole('ADMIN','MEDICO','ENFERMERO','RECEPCIONISTA')")
    @Operation(summary = "Obtener paciente por DPI", description = "Busca un paciente activo por su DPI exacto (13 dígitos). Útil en recepción para identificar rápidamente al paciente.\n\nRoles permitidos: ADMIN, MEDICO, ENFERMERO, RECEPCIONISTA.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paciente encontrado"),
            @ApiResponse(responseCode = "404", description = "El paciente no existe")
    })
    public PacienteResponse obtenerPorDpi(@Parameter(description = "DPI de 13 dígitos.", example = "2547896320101") @PathVariable String dpi) {
        return service.buscarPorDpi(dpi);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MEDICO','ENFERMERO','RECEPCIONISTA')")
    @Operation(summary = "Obtener paciente por ID", description = "Devuelve los datos de un paciente, activo o inactivo (el campo `activo` lo indica). Responde 404 solo si no existe.\n\nRoles permitidos: ADMIN, MEDICO, ENFERMERO, RECEPCIONISTA.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paciente encontrado"),
            @ApiResponse(responseCode = "404", description = "El paciente no existe")
    })
    public PacienteResponse obtener(@Parameter(description = "Id del registro en «Pacientes» (debe existir).", example = "1") @PathVariable Long id) {
        return service.obtenerDetalle(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','RECEPCIONISTA')")
    @Operation(summary = "Registrar paciente", description = "Registra un nuevo paciente. Valida DPI y correo únicos, fecha de nacimiento coherente y que la alergia y el tipo de sangre existan y estén activos. La fecha de registro la asigna el servidor.\n\nRoles permitidos: ADMIN, RECEPCIONISTA.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Paciente creado"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "409", description = "El DPI ya existe"),
            @ApiResponse(responseCode = "422", description = "Alergia o tipo de sangre inexistente")
    })
    public ResponseEntity<PacienteResponse> crear(@Valid @RequestBody PacienteRequest request) {
        PacienteResponse creado = service.crear(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(creado.idPaciente()).toUri();
        return ResponseEntity.created(location).body(creado);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPCIONISTA')")
    @Operation(summary = "Actualizar paciente", description = "Modifica los datos de un paciente aplicando las mismas validaciones que en el registro.\n\nRoles permitidos: ADMIN, RECEPCIONISTA.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paciente actualizado"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "404", description = "El paciente no existe"),
            @ApiResponse(responseCode = "409", description = "El DPI ya pertenece a otro paciente"),
            @ApiResponse(responseCode = "422", description = "Alergia o tipo de sangre inexistente")
    })
    public PacienteResponse actualizar(@Parameter(description = "Id del registro en «Pacientes» (debe existir).", example = "1") @PathVariable Long id, @Valid @RequestBody PacienteRequest request) {
        return service.actualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Desactivar paciente", description = "Borrado lógico del paciente. No se permite si tiene citas activas, una hospitalización en curso, un registro activo en emergencia o cuentas activas.\n\nRoles permitidos: ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Paciente desactivado"),
            @ApiResponse(responseCode = "404", description = "El paciente no existe o ya está inactivo"),
            @ApiResponse(responseCode = "422", description = "Tiene citas activas, hospitalización en curso, registro activo en emergencia o cuentas activas")
    })
    public void eliminar(@Parameter(description = "Id del registro en «Pacientes» (debe existir).", example = "1") @PathVariable Long id) {
        service.eliminar(id);
    }
}
