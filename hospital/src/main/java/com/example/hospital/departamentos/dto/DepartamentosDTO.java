package com.example.hospital.departamentos.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DepartamentosDTO(
    @Schema(description = "Identificador del registro. Lo genera el servidor: no se envía al crear (se ignora).", example = "1")
    Long idDepartamento,
    @Schema(description = "Nombre del departamento (único).", example = "Pediatría")
    @NotBlank(message = "El nombre del departamento es obligatorio")
    @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
    String nombre,
    @Schema(description = "Descripción del departamento.", example = "Atención médica de niños y adolescentes")
    @Size(max = 255, message = "La descripción no puede superar los 255 caracteres")
    String descripcion,
    @Schema(description = "Teléfono de 8 dígitos.", example = "22345679")
    @Size(max = 20, message = "El teléfono no puede superar los 20 caracteres")
    String telefono,
    @Schema(description = "Indica si el departamento está activo.", example = "true")
    Boolean estado
) {}
