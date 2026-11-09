package com.example.hospital.rol;

import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import com.example.hospital.rol.dto.RolDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rol")
@Tag(name = "Roles", description = "Roles de usuario del sistema (ADMIN, MEDICO, ENFERMERO, etc.)")
@ApiResponses({
        @ApiResponse(responseCode = "401", description = "Token ausente, inválido o expirado"),
        @ApiResponse(responseCode = "403", description = "El rol del usuario no tiene permiso")
})
public class RolController {

    private final RolService rolService;

    public RolController(RolService rolService) {
        this.rolService = rolService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Consulta exitosa")
    })
    @Operation(summary = "Listar roles", description = "Devuelve todos los roles del sistema.\n\nRoles permitidos: ADMIN.")
    public List<RolDTO> findAll() {
        return rolService.findAll();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{id}")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Consulta exitosa"),
            @ApiResponse(responseCode = "400", description = "Algún parámetro de la ruta o de la consulta tiene un formato inválido (por ejemplo un id no numérico o una fecha mal escrita)"),
            @ApiResponse(responseCode = "404", description = "No existe el rol indicado"),
            @ApiResponse(responseCode = "422", description = "Regla de negocio incumplida")
    })
    @Operation(summary = "Obtener rol por ID", description = "Devuelve el detalle de un rol.\n\nRoles permitidos: ADMIN.")
    public RolDTO findById(@Parameter(description = "Id del registro en «Roles» (debe existir).", example = "1") @PathVariable Long id) {
        return rolService.findById(id);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Creado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos del cuerpo inválidos: ver `detalles` con el mensaje de cada campo"),
            @ApiResponse(responseCode = "409", description = "Ya existe un rol con el nombre"),
            @ApiResponse(responseCode = "422", description = "El nombre del rol es obligatorio")
    })
    @Operation(summary = "Registrar rol", description = "Crea un nuevo rol.\n\nRoles permitidos: ADMIN.")
    public ResponseEntity<RolDTO> crear(@Valid @RequestBody RolDTO dto) {
        return new ResponseEntity<>(rolService.crear(dto), HttpStatus.CREATED);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Actualizado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos del cuerpo inválidos: ver `detalles` con el mensaje de cada campo; Algún parámetro de la ruta o de la consulta tiene un formato inválido (por ejemplo un id no numérico o una fecha mal escrita)"),
            @ApiResponse(responseCode = "404", description = "No existe el rol indicado"),
            @ApiResponse(responseCode = "409", description = "Ya existe un rol con el nombre"),
            @ApiResponse(responseCode = "422", description = "No se puede cambiar el nombre del rol base; El nombre del rol es obligatorio")
    })
    @Operation(summary = "Actualizar rol", description = "Modifica un rol. Los 6 roles base (ADMIN, MEDICO, ENFERMERO, RECEPCIONISTA, FARMACEUTICO, CAJERO) no se pueden renombrar (422).\n\nRoles permitidos: ADMIN.")
    public ResponseEntity<RolDTO> actualizar(@Parameter(description = "Id del registro en «Roles» (debe existir).", example = "1") @PathVariable Long id, @Valid @RequestBody RolDTO dto) {
        return ResponseEntity.ok(rolService.actualizar(dto, id));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Operación realizada sin contenido en la respuesta"),
            @ApiResponse(responseCode = "400", description = "Algún parámetro de la ruta o de la consulta tiene un formato inválido (por ejemplo un id no numérico o una fecha mal escrita)"),
            @ApiResponse(responseCode = "404", description = "No existe el rol indicado"),
            @ApiResponse(responseCode = "409", description = "No se puede eliminar el rol porque hay usuarios activos con ese rol"),
            @ApiResponse(responseCode = "422", description = "Regla de negocio incumplida")
    })
    @Operation(summary = "Eliminar rol", description = "Desactiva un rol. 409 si hay usuarios activos con ese rol.\n\nRoles permitidos: ADMIN.")
    public ResponseEntity<Void> eliminar(@Parameter(description = "Id del registro en «Roles» (debe existir).", example = "1") @PathVariable Long id) {
        rolService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
