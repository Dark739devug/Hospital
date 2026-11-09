package com.example.hospital.enfermeros.dto;

import com.example.hospital.usuarios.dto.CredencialesGeneradasDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record EnfermerosDTO(
    @Schema(description = "Identificador del registro. Lo genera el servidor: no se envía al crear (se ignora).", example = "1")
    Long idEnfermero,
    @Schema(description = "Cédula profesional (única).", example = "ENF-2024-0153")
    @NotBlank(message = "La cédula profesional es obligatoria")
    @Size(max = 30, message = "La cédula profesional no puede superar los 30 caracteres")
    String cedulaProfesional,
    @Schema(description = "Nombres.", example = "María Fernanda")
    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
    String nombre,
    @Schema(description = "Apellido paterno.", example = "López")
    @NotBlank(message = "El apellido paterno es obligatorio")
    @Size(max = 100, message = "El apellido paterno no puede superar los 100 caracteres")
    String apellidoPaterno,
    @Schema(description = "Apellido materno.", example = "Ramírez")
    @Size(max = 100, message = "El apellido materno no puede superar los 100 caracteres")
    String apellidoMaterno,
    @Schema(description = "Teléfono de 8 dígitos.", example = "55123456")
    @Size(max = 20, message = "El teléfono no puede superar los 20 caracteres")
    String telefono,
    @Schema(description = "Id del departamento (debe existir y estar activo).", example = "1")
    @NotNull(message = "El departamento es obligatorio")
    Long idDepartamento,
    @Schema(description = "Indica si el registro está activo. Si se omite al crear queda activo; false equivale a un borrado lógico.", example = "true")
    Boolean activo,
    @Schema(description = "Credencial inicial generada automáticamente al crear. Solo viene en la respuesta de creación.", accessMode = Schema.AccessMode.READ_ONLY)
    CredencialesGeneradasDTO credenciales
) {
    public EnfermerosDTO(Long idEnfermero, String cedulaProfesional, String nombre, String apellidoPaterno, String apellidoMaterno, String telefono, Long idDepartamento, Boolean activo) {
        this(idEnfermero, cedulaProfesional, nombre, apellidoPaterno, apellidoMaterno, telefono, idDepartamento, activo, null);
    }
}
