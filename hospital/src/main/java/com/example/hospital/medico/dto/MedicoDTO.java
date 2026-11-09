package com.example.hospital.medico.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record MedicoDTO(
    @Schema(description = "Identificador del registro. Lo genera el servidor: no se envía al crear (se ignora).", example = "1")
    Long idMedico,
    @Schema(description = "Número de colegiado (único).", example = "12345")
    @NotBlank(message = "El número de colegiado es obligatorio")
    @Size(max = 30, message = "El número de colegiado no puede superar los 30 caracteres")
    String noColegiado,
    @Schema(description = "Nombres.", example = "Carlos Alberto")
    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
    String nombre,
    @Schema(description = "Apellido paterno.", example = "Méndez")
    @NotBlank(message = "El apellido paterno es obligatorio")
    @Size(max = 100, message = "El apellido paterno no puede superar los 100 caracteres")
    String apellidoPaterno,
    @Schema(description = "Apellido materno.", example = "Pérez")
    @Size(max = 100, message = "El apellido materno no puede superar los 100 caracteres")
    String apellidoMaterno,
    @Schema(description = "Teléfono de 8 dígitos.", example = "55987654")
    @Size(min = 7, max = 20, message = "El teléfono debe contener entre 7 y 20 caracteres")
    String telefono,
    @Schema(description = "Correo electrónico.", example = "carlos.mendez@hospital.example")
    @Size(max = 150, message = "El correo no puede superar los 150 caracteres")
    @Pattern(regexp = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$", message = "El formato del correo electrónico no es válido")
    String email,
    @Schema(description = "Indica si el médico está activo.", example = "true")
    Boolean estado
) {}
