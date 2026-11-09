package com.example.hospital.paciente.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record PacienteRequest(
        @Schema(example = "2345678901234")
        @NotBlank(message = "El DPI es obligatorio")
        @Pattern(regexp = "\\d{13}", message = "El DPI debe tener 13 dígitos")
        String dpiPaciente,

        @Schema(example = "Ana")
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100)
        String nombre,

        @Schema(description = "Apellido paterno.", example = "García")
        @NotBlank(message = "El apellido paterno es obligatorio")
        @Size(max = 100)
        String apellidoPaterno,

        @Schema(description = "Apellido materno.", example = "Hernández")
        @NotBlank(message = "El apellido materno es obligatorio")
        @Size(max = 100)
        String apellidoMaterno,

        @Schema(description = "Fecha de nacimiento yyyy-MM-dd (no futura).", example = "1990-05-20")
        @Past(message = "La fecha de nacimiento debe ser pasada")
        LocalDate fechaNacimiento,

        @Schema(example = "F", description = "M o F")
        @NotBlank(message = "El sexo es obligatorio")
        @Pattern(regexp = "[MF]", message = "El sexo debe ser M o F")
        String sexo,

        @Schema(description = "Teléfono de 8 dígitos.", example = "55123456")
        @NotBlank(message = "El teléfono es obligatorio")
        @Size(max = 20)
        String telefono,

        @Schema(description = "Correo electrónico.", example = "ana.garcia@example.com")
        @Email(message = "Correo electrónico inválido")
        @Size(max = 150)
        String correoElectronico,

        @Schema(description = "Dirección de residencia.", example = "6a avenida 5-20 zona 1, Guatemala")
        @Size(max = 255)
        String direccion,

        @Schema(description = "Id de la alergia (catálogo /api/alergias). Opcional.", example = "1")
        Long idAlergias,
        @Schema(description = "Id del tipo de sangre (catálogo /api/tipo-sangre; 1 = A+, 7 = O+). Opcional.", example = "7")
        Long idTipoDeSangre
) {}
