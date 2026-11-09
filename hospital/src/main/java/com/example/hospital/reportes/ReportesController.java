package com.example.hospital.reportes;

import org.springframework.security.core.Authentication;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import com.example.hospital.reportes.dto.HistorialPacienteDTO;
import com.example.hospital.reportes.dto.ResumenDashboardDTO;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reportes")
@Tag(name = "Reportes", description = "**Nuevo.** Indicadores y vistas consolidadas para tableros y expedientes")
@ApiResponses({
        @ApiResponse(responseCode = "401", description = "Token ausente, inválido o expirado"),
        @ApiResponse(responseCode = "403", description = "El rol del usuario no tiene permiso")
})
public class ReportesController {

    private final ReportesService reportesService;

    public ReportesController(ReportesService reportesService) {
        this.reportesService = reportesService;
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO', 'ENFERMERO', 'RECEPCIONISTA', 'CAJERO')")
    @GetMapping("/resumen")
    @Operation(summary = "Resumen del hospital (dashboard)", description = "**Nuevo.** Devuelve en una sola llamada los indicadores principales del día: pacientes activos, citas de hoy, hospitalizaciones en curso, emergencias sin alta, habitaciones disponibles, medicamentos con bajo stock y facturas pendientes.\n\nRoles permitidos: ADMIN, MEDICO, ENFERMERO, RECEPCIONISTA, CAJERO.")
    public ResumenDashboardDTO resumen() {
        return reportesService.resumen();
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO', 'ENFERMERO')")
    @GetMapping("/paciente/{idPaciente}/historial")
    @Operation(summary = "Historial completo de un paciente", description = "Devuelve el expediente consolidado de un paciente: datos generales, citas, consultas de esas citas, diagnósticos asociados, tratamientos (y sus medicamentos), hospitalizaciones, ingresos a emergencia y, solo para ADMIN, cuentas y facturas.\n\n- `incluirAnulados` (por defecto `false`): incluye también los registros anulados (`activo=false`) en TODAS las secciones; sin él, todas se filtran con el mismo criterio.\n- Minimización de datos: para MEDICO y ENFERMERO las listas `cuentas` y `facturas` vienen vacías y `incluyeDatosFinancieros` es `false`.\n- Es un histórico: un paciente inactivo también devuelve su historial (`paciente.activo=false`). Responde 404 solo si el paciente no existe.\n\nRoles permitidos: ADMIN, MEDICO, ENFERMERO.")
    public HistorialPacienteDTO historialPaciente(@PathVariable Long idPaciente,
                                                  @RequestParam(required = false, defaultValue = "false") boolean incluirAnulados,
                                                  Authentication authentication) {
        boolean finanzas = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()) || "ROLE_CAJERO".equals(a.getAuthority()));
        return reportesService.historialPaciente(idPaciente, incluirAnulados, finanzas);
    }
}
