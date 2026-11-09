package com.example.hospital.tiposangre.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record TipoSangreDTO(
    @Schema(description = "Identificador del registro. Lo genera el servidor: no se envía al crear (se ignora).", example = "1")
    Long idTipoDeSangre,
    @Schema(description = "Tipo de sangre (único).", example = "O+")
    @NotBlank(message = "El nombre del tipo de sangre es obligatorio")
    @Size(max = 10, message = "El nombre no puede superar los 10 caracteres")
    @Pattern(regexp = "(?i)(A|B|AB|O)[+-]", message = "El tipo de sangre debe ser A+, A-, B+, B-, AB+, AB-, O+ u O-")
    String nombre,
    @Schema(description = "Descripción.", example = "O positivo")
    @Size(max = 200, message = "La descripción no puede superar los 200 caracteres")
    String descripcion,
    @Schema(description = "Indica si el registro está activo. Si se omite al crear queda activo; false equivale a un borrado lógico.", example = "true")
    Boolean activo
) {}
