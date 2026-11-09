package com.example.hospital.paciente.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Datos MÍNIMOS de un paciente para identificarlo (p. ej. en caja al emitir facturas o abrir cuentas).
 * Deliberadamente NO incluye teléfono, correo, dirección, fecha de nacimiento, alergias, tipo de sangre ni datos clínicos.
 */
@Schema(description = "Resumen mínimo de un paciente: solo identificación (sin teléfono, correo, dirección ni datos clínicos)")
public record PacienteResumenDTO(
        @Schema(description = "Identificador del registro. Lo genera el servidor: no se envía al crear (se ignora).", example = "1")
        Long idPaciente,
        @Schema(description = "Nombre y apellidos", example = "Ana Prueba López García")
        String nombreCompleto,
        @Schema(description = "DPI de 13 dígitos.", example = "2547896320101")
        String dpiPaciente,
        @Schema(description = "false si el paciente está desactivado (solo aparece en /{id}/resumen)")
        Boolean activo
) {}
