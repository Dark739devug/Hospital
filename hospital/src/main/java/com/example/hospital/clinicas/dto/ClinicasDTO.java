package com.example.hospital.clinicas.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ClinicasDTO(
    @Schema(description = "Identificador del registro. Lo genera el servidor: no se envía al crear (se ignora).", example = "1")
    Long idClinica,
    @Schema(description = "Nombre de la clínica.", example = "Clínica de Medicina General")
    @NotBlank(message = "El nombre de la clínica es obligatorio")
    @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
    String nombre,
    @Schema(description = "Descripción de la clínica.", example = "Atención de consulta externa general")
    @Size(max = 255, message = "La descripción no puede superar los 255 caracteres")
    String descripcion,
    @Schema(description = "Ubicación dentro del hospital.", example = "Edificio A, segundo nivel")
    @Size(max = 255, message = "La ubicación no puede superar los 255 caracteres")
    String ubicacion,
    @Schema(description = "Teléfono de 8 dígitos.", example = "22345678")
    @Size(max = 20, message = "El teléfono no puede superar los 20 caracteres")
    String telefono,
    @Schema(description = "Indica si la clínica está activa.", example = "true")
    Boolean estadoClinica,
    @Schema(description = "Id del departamento (debe existir y estar activo).", example = "1")
    @NotNull(message = "El departamento es obligatorio")
    Long idDepartamento,
    @Schema(description = "Id de la especialidad (debe existir y estar activa).", example = "1")
    Long idEspecialidad
) {}
