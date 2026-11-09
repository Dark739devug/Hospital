package com.example.hospital.cuentas;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import com.example.hospital.cuentas.dto.CuentasDTO;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/cuentas")
@Tag(name = "Cuentas", description = "Cuentas hospitalarias donde se acumulan los cargos de un paciente")
@ApiResponses({
        @ApiResponse(responseCode = "401", description = "Token ausente, inválido o expirado"),
        @ApiResponse(responseCode = "403", description = "El rol del usuario no tiene permiso")
})
public class CuentasController {

    private final CuentasService cuentasService;

    public CuentasController(CuentasService cuentasService) {
        this.cuentasService = cuentasService;
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'CAJERO', 'RECEPCIONISTA')")
    @GetMapping
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Consulta exitosa"),
            @ApiResponse(responseCode = "400", description = "Algún parámetro de la ruta o de la consulta tiene un formato inválido (por ejemplo un id no numérico o una fecha mal escrita)"),
            @ApiResponse(responseCode = "422", description = "El estado de la cuenta debe ser Abierta, Cerrada o Liquidada")
    })
    @Operation(summary = "Listar cuentas", description = "Devuelve las cuentas hospitalarias. Filtros opcionales y COMBINABLES:\n- `idPaciente`\n- `idHospitalizacion`\n- `estado` (Abierta, Cerrada o Liquidada; otro valor responde 422)\n- `soloActivas=true`: oculta las desactivadas (por defecto se devuelven también)\n\nRoles permitidos: ADMIN, CAJERO, RECEPCIONISTA.")
    public List<CuentasDTO> findAll(
            @Parameter(description = "Si es true devuelve solo los registros activos; por defecto false (todos).", example = "true") @RequestParam(required = false, defaultValue = "false") boolean soloActivas,
            @Parameter(description = "Id del paciente.", example = "1") @RequestParam(required = false) Long idPaciente,
            @Parameter(description = "Id de la hospitalización.", example = "1") @RequestParam(required = false) Long idHospitalizacion,
            @Parameter(description = "Estado de la cuenta (por ejemplo Abierta o Cerrada).", example = "Abierta") @RequestParam(required = false) String estado) {
        return cuentasService.buscar(idPaciente, idHospitalizacion, estado, soloActivas);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'CAJERO', 'RECEPCIONISTA')")
    @GetMapping("/{id}")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Consulta exitosa"),
            @ApiResponse(responseCode = "400", description = "Algún parámetro de la ruta o de la consulta tiene un formato inválido (por ejemplo un id no numérico o una fecha mal escrita)"),
            @ApiResponse(responseCode = "404", description = "No existe la cuenta hospitalaria indicada")
    })
    @Operation(summary = "Obtener cuenta por ID", description = "Devuelve el detalle de una cuenta (estado y saldo). Responde 404 si no existe.\n\nRoles permitidos: ADMIN, CAJERO, RECEPCIONISTA.")
    public CuentasDTO findById(@Parameter(description = "Id del registro en «Cuentas» (debe existir).", example = "1") @PathVariable Long id) {
        return cuentasService.findById(id);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'CAJERO')")
    @PostMapping
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Creado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos del cuerpo inválidos: ver `detalles` con el mensaje de cada campo"),
            @ApiResponse(responseCode = "409", description = "Ya existe una cuenta hospitalaria activa para esta hospitalización"),
            @ApiResponse(responseCode = "422", description = "El paciente indicado no existe; El paciente seleccionado se encuentra inactivo; La hospitalización indicada no existe; La hospitalización no corresponde al paciente seleccionado; El saldo de la cuenta no puede ser negativo")
    })
    @Operation(summary = "Abrir cuenta", description = "Abre una cuenta hospitalaria para un paciente/hospitalización. Una hospitalización solo puede tener una cuenta activa. La cuenta nace Abierta y activa (se ignoran `estado` y `activo`); el cliente solo define el saldo inicial (>= 0, por defecto 0).\n\nErrores: 400 (campos inválidos, con `detalles`), 409 (la hospitalización ya tiene una cuenta activa), 422 (paciente u hospitalización inexistentes/inactivos o de otro paciente).\n\nRoles permitidos: ADMIN, CAJERO.")
    public ResponseEntity<CuentasDTO> save(@Valid @RequestBody CuentasDTO dto) {
        CuentasDTO creada = cuentasService.crear(dto);
        return new ResponseEntity<>(creada, HttpStatus.CREATED);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'CAJERO')")
    @PutMapping("/{id}")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Actualizado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos del cuerpo inválidos: ver `detalles` con el mensaje de cada campo; Algún parámetro de la ruta o de la consulta tiene un formato inválido (por ejemplo un id no numérico o una fecha mal escrita)"),
            @ApiResponse(responseCode = "404", description = "No existe la cuenta hospitalaria indicada"),
            @ApiResponse(responseCode = "409", description = "Ya existe una cuenta hospitalaria activa para esta hospitalización"),
            @ApiResponse(responseCode = "422", description = "La cuenta está desactivada y no se puede modificar; No se puede cambiar el paciente de una cuenta que tiene pagos registrados; Una cuenta solo puede quedar Liquidada con saldo 0; El paciente indicado no existe; El paciente seleccionado se encuentra inactivo")
    })
    @Operation(summary = "Actualizar cuenta", description = "Modifica paciente, hospitalización y estado (Abierta, Cerrada o Liquidada; Liquidada exige saldo 0). El saldo NO se modifica aquí (se ignora): use `PATCH /ajustar-saldo`. Una cuenta Liquidada puede reabrirse con estado Abierta. No cambia el paciente si la cuenta tiene pagos.\n\nErrores: 400, 404, 409, 422.\n\nRoles permitidos: ADMIN, CAJERO.")
    public ResponseEntity<CuentasDTO> update(
            @Parameter(description = "Id del registro en «Cuentas» (debe existir).", example = "1") @PathVariable Long id,
            @Valid @RequestBody CuentasDTO dto) {
        CuentasDTO actualizada = cuentasService.actualizar(dto, id);
        return ResponseEntity.ok(actualizada);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'CAJERO')")
    @PatchMapping("/{id}/ajustar-saldo")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Actualizado correctamente"),
            @ApiResponse(responseCode = "400", description = "Algún parámetro de la ruta o de la consulta tiene un formato inválido (por ejemplo un id no numérico o una fecha mal escrita)"),
            @ApiResponse(responseCode = "404", description = "No existe la cuenta hospitalaria indicada"),
            @ApiResponse(responseCode = "422", description = "El monto de ajuste es obligatorio y no puede ser cero; El monto de ajuste admite como máximo 2 decimales; No se puede ajustar el saldo de una cuenta desactivada; No se puede ajustar el saldo de una cuenta Cerrada: reábrala antes; El ajuste resultaría en un saldo negativo no permitido")
    })
    @Operation(summary = "Ajustar saldo", description = "Suma (monto positivo) o resta (monto negativo) un valor al saldo de la cuenta. Reglas (422): monto distinto de 0 y con hasta 2 decimales; el saldo resultante no puede ser negativo ni mayor a 99,999,999.99; no se ajustan cuentas desactivadas ni Cerradas. Con saldo 0 una cuenta Abierta pasa a Liquidada y una Liquidada vuelve a Abierta si el saldo resultante es mayor que 0 (reapertura automática).\n\nErrores: 400 (monto ausente o con formato inválido), 404, 422.\n\nRoles permitidos: ADMIN, CAJERO.")
    public ResponseEntity<CuentasDTO> ajustarSaldo(
            @Parameter(description = "Id del registro en «Cuentas» (debe existir).", example = "1") @PathVariable Long id,
            @Parameter(description = "Valor a sumar (positivo) o restar (negativo) al saldo, en quetzales, con hasta 2 decimales.", example = "100.00") @RequestParam BigDecimal monto) {
        CuentasDTO actualizada = cuentasService.ajustarSaldo(id, monto);
        return ResponseEntity.ok(actualizada);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Operación realizada sin contenido en la respuesta"),
            @ApiResponse(responseCode = "400", description = "Algún parámetro de la ruta o de la consulta tiene un formato inválido (por ejemplo un id no numérico o una fecha mal escrita)"),
            @ApiResponse(responseCode = "404", description = "No existe la cuenta hospitalaria indicada"),
            @ApiResponse(responseCode = "409", description = "No se puede desactivar la cuenta hospitalaria porque contiene pagos registrados"),
            @ApiResponse(responseCode = "422", description = "La cuenta ya se encuentra desactivada")
    })
    @Operation(summary = "Cerrar / eliminar cuenta", description = "Desactiva una cuenta hospitalaria (borrado lógico).\n\nErrores: 404; 409 si la cuenta tiene pagos registrados; 422 si ya está desactivada.\n\nRoles permitidos: ADMIN.")
    public ResponseEntity<Void> delete(@Parameter(description = "Id del registro en «Cuentas» (debe existir).", example = "1") @PathVariable Long id) {
        cuentasService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
