package com.example.hospital.paciente.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record PacienteResponse(
        @Schema(description = "Identificador del registro. Lo genera el servidor: no se envía al crear (se ignora).", example = "1")
        Long idPaciente,
        @Schema(description = "DPI de 13 dígitos.", example = "2547896320101")
        String dpiPaciente,
        @Schema(description = "Nombres.", example = "Ana María")
        String nombre,
        @Schema(description = "Apellido paterno.", example = "García")
        String apellidoPaterno,
        @Schema(description = "Apellido materno.", example = "Hernández")
        String apellidoMaterno,
        @Schema(description = "Fecha de nacimiento yyyy-MM-dd.", example = "1990-05-20")
        LocalDate fechaNacimiento,
        @Schema(description = "Sexo: M o F.", example = "F")
        String sexo,
        @Schema(description = "Teléfono de 8 dígitos.", example = "55123456")
        String telefono,
        @Schema(description = "Correo electrónico.", example = "ana.garcia@example.com")
        String correoElectronico,
        @Schema(description = "Dirección de residencia.", example = "6a avenida 5-20 zona 1, Guatemala")
        String direccion,
        @Schema(description = "Fecha y hora de registro en el sistema.", example = "2026-03-15T10:30:00")
        LocalDateTime fechaRegistro,
        @Schema(description = "Id de la alergia (catálogo /api/alergias). Opcional.", example = "1")
        Long idAlergias,
        @Schema(description = "Id del tipo de sangre (catálogo /api/tipo-sangre; 1 = A+, 7 = O+). Opcional.", example = "7")
        Long idTipoDeSangre,
        @Schema(description = "Indica si el paciente está activo.", example = "true")
        Boolean activo
) {}
