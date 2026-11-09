package com.example.hospital.especialidadmedico.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EspecialidadMedicoDTO(
    @Schema(description = "Identificador del registro. Lo genera el servidor: no se envía al crear (se ignora).", example = "1")
    Long idEspecialidad,
    @Schema(description = "Nombre de la especialidad médica (único).", example = "Cardiología")
    @NotBlank(message = "El nombre de la especialidad es obligatorio")
    @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
    String nombre,
    @Schema(description = "Descripción.", example = "Diagnóstico y tratamiento de enfermedades del corazón")
    @Size(max = 255, message = "La descripción no puede superar los 255 caracteres")
    String descripcion,
    @Schema(description = "Indica si el registro está activo. Si se omite al crear queda activo; false equivale a un borrado lógico.", example = "true")
    Boolean activo
) {}
