package com.example.hospital.medicamentos;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import com.example.hospital.medicamentos.dto.MedicamentosDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/medicamentos")
@Tag(name = "Medicamentos", description = "Inventario de medicamentos de farmacia")
@ApiResponses({
        @ApiResponse(responseCode = "401", description = "Token ausente, inválido o expirado"),
        @ApiResponse(responseCode = "403", description = "El rol del usuario no tiene permiso")
})
public class MedicamentosController {

    private final MedicamentosService medicamentosService;

    public MedicamentosController(MedicamentosService medicamentosService) {
        this.medicamentosService = medicamentosService;
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO', 'ENFERMERO', 'FARMACEUTICO')")
    @GetMapping
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Consulta exitosa"),
            @ApiResponse(responseCode = "400", description = "Algún parámetro de la ruta o de la consulta tiene un formato inválido (por ejemplo un id no numérico o una fecha mal escrita)")
    })
    @Operation(summary = "Listar / buscar medicamentos", description = "Devuelve los medicamentos. Filtros opcionales y COMBINABLES (se aplican todos a la vez):\n- `criterio`: busca por nombre comercial, genérico o laboratorio (sin distinguir mayúsculas)\n- `bajoStock=true`: stock igual o menor al mínimo\n- `soloActivos=true`: solo activos (por defecto se devuelven también los inactivos)\n\nRoles permitidos: ADMIN, MEDICO, ENFERMERO, FARMACEUTICO.")
    public List<MedicamentosDTO> findAll(
            @Parameter(description = "Si es true devuelve solo los registros activos; por defecto false (todos).", example = "true") @RequestParam(required = false, defaultValue = "false") boolean soloActivos,
            @Parameter(description = "Si es true devuelve solo medicamentos en o por debajo de su stock mínimo.", example = "true") @RequestParam(required = false, defaultValue = "false") boolean bajoStock,
            @Parameter(description = "Texto a buscar (nombre comercial, genérico o laboratorio).", example = "Paracetamol") @RequestParam(required = false) String criterio) {
        return medicamentosService.buscar(criterio, bajoStock, soloActivos);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO', 'ENFERMERO', 'FARMACEUTICO')")
    @GetMapping("/{id}")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Consulta exitosa"),
            @ApiResponse(responseCode = "400", description = "Algún parámetro de la ruta o de la consulta tiene un formato inválido (por ejemplo un id no numérico o una fecha mal escrita)"),
            @ApiResponse(responseCode = "404", description = "No existe el medicamento indicado")
    })
    @Operation(summary = "Obtener medicamento por ID", description = "Devuelve el detalle de un medicamento (stock, precio, si requiere receta).\n\nErrores: 404 si no existe.\n\nRoles permitidos: ADMIN, MEDICO, ENFERMERO, FARMACEUTICO.")
    public MedicamentosDTO findById(@Parameter(description = "Id del registro en «Medicamentos» (debe existir).", example = "1") @PathVariable Long id) {
        return medicamentosService.findById(id);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'FARMACEUTICO', 'MEDICO')")
    @PostMapping
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Creado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos del cuerpo inválidos: ver `detalles` con el mensaje de cada campo"),
            @ApiResponse(responseCode = "409", description = "Ya existe un medicamento registrado con el nombre comercial; Ya existe otro medicamento registrado con el nombre comercial"),
            @ApiResponse(responseCode = "422", description = "Regla de negocio incumplida")
    })
    @Operation(summary = "Registrar medicamento", description = "Agrega un medicamento al inventario. El nombre comercial debe ser único. El campo `stock` se toma como stock inicial (por defecto 0).\n\nErrores: 400 (campos inválidos, con `detalles`), 409 (nombre comercial repetido).\n\nRoles permitidos: ADMIN, FARMACEUTICO, MEDICO.")
    public ResponseEntity<MedicamentosDTO> save(@Valid @RequestBody MedicamentosDTO dto) {
        MedicamentosDTO creado = medicamentosService.crear(dto);
        return new ResponseEntity<>(creado, HttpStatus.CREATED);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'FARMACEUTICO')")
    @PutMapping("/{id}")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Actualizado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos del cuerpo inválidos: ver `detalles` con el mensaje de cada campo; Algún parámetro de la ruta o de la consulta tiene un formato inválido (por ejemplo un id no numérico o una fecha mal escrita)"),
            @ApiResponse(responseCode = "404", description = "No se encontró el medicamento indicado; No existe el medicamento indicado"),
            @ApiResponse(responseCode = "409", description = "Ya existe un medicamento registrado con el nombre comercial; Ya existe otro medicamento registrado con el nombre comercial"),
            @ApiResponse(responseCode = "422", description = "Regla de negocio incumplida")
    })
    @Operation(summary = "Actualizar medicamento", description = "Modifica los datos de un medicamento. NO modifica el stock: el campo `stock` del cuerpo se ignora y se conserva el actual; el stock solo cambia con `PATCH /{id}/stock`.\n\nErrores: 400 (campos inválidos, con `detalles`), 404 (no existe), 409 (nombre comercial repetido).\n\nRoles permitidos: ADMIN, FARMACEUTICO.")
    public ResponseEntity<MedicamentosDTO> update(
            @Parameter(description = "Id del registro en «Medicamentos» (debe existir).", example = "1") @PathVariable Long id,
            @Valid @RequestBody MedicamentosDTO dto) {
        MedicamentosDTO actualizado = medicamentosService.actualizar(dto, id);
        return ResponseEntity.ok(actualizado);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'FARMACEUTICO')")
    @PatchMapping("/{id}/stock")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Actualizado correctamente"),
            @ApiResponse(responseCode = "400", description = "Algún parámetro de la ruta o de la consulta tiene un formato inválido (por ejemplo un id no numérico o una fecha mal escrita)"),
            @ApiResponse(responseCode = "404", description = "No existe el medicamento indicado"),
            @ApiResponse(responseCode = "422", description = "No se puede ajustar el stock de un medicamento inactivo; Stock insuficiente. Stock actual: %d, cambio solicitado: %d (resultado: %d)")
    })
    @Operation(summary = "Ajustar stock", description = "Suma (valor positivo, p. ej. una compra) o resta (valor negativo, p. ej. una salida) unidades al stock. No permite stock negativo ni ajustar medicamentos inactivos.\n\nErrores: 400 (formato inválido), 404 (no existe), 422 (cambio 0, stock insuficiente o medicamento inactivo).\n\nRoles permitidos: ADMIN, FARMACEUTICO.")
    public ResponseEntity<MedicamentosDTO> ajustarStock(
            @Parameter(description = "Id del registro en «Medicamentos» (debe existir).", example = "1") @PathVariable Long id,
            @Parameter(description = "Unidades a sumar (positivo) o restar (negativo) al stock; no puede ser 0 ni dejar el stock negativo.", example = "10") @RequestParam Integer cambio) {
        MedicamentosDTO actualizado = medicamentosService.ajustarStock(id, cambio);
        return ResponseEntity.ok(actualizado);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Operación realizada sin contenido en la respuesta"),
            @ApiResponse(responseCode = "400", description = "Algún parámetro de la ruta o de la consulta tiene un formato inválido (por ejemplo un id no numérico o una fecha mal escrita)"),
            @ApiResponse(responseCode = "404", description = "No se encontró el medicamento indicado; No existe el medicamento indicado"),
            @ApiResponse(responseCode = "409", description = "No se puede desactivar el medicamento porque está asociado a uno o más tratamientos médicos"),
            @ApiResponse(responseCode = "422", description = "El medicamento ya se encuentra inactivo")
    })
    @Operation(summary = "Eliminar medicamento", description = "Desactiva un medicamento (borrado lógico).\n\nErrores: 404 (no existe), 409 (está asignado a tratamientos), 422 (ya inactivo).\n\nRoles permitidos: ADMIN.")
    public ResponseEntity<Void> delete(@Parameter(description = "Id del registro en «Medicamentos» (debe existir).", example = "1") @PathVariable Long id) {
        medicamentosService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
