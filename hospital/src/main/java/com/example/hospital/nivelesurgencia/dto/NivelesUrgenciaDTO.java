package com.example.hospital.nivelesurgencia.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record NivelesUrgenciaDTO(
    @Schema(description = "Identificador del registro. Lo genera el servidor: no se envía al crear (se ignora).", example = "1")
    Long idNivelUrgencia,
    @Schema(description = "Código del nivel: N seguido de un número (N1 a N5).", example = "N3")
    @NotBlank(message = "El código es obligatorio")
    @Size(max = 5, message = "El código no puede superar los 5 caracteres")
    @Pattern(regexp = "(?i)N[0-9]+", message = "El código debe tener el formato N1, N2, N3...")
    String codigo,
    @Schema(description = "Descripción del nivel.", example = "Urgencia - Requiere atención pronta")
    @NotBlank(message = "La descripción es obligatoria")
    @Size(max = 100, message = "La descripción no puede superar los 100 caracteres")
    String descripcion,
    @Schema(description = "Tiempo máximo de espera en minutos (0 o más).", example = "30")
    @NotNull(message = "El tiempo máximo de espera es obligatorio")
    @PositiveOrZero(message = "El tiempo máximo de espera debe ser mayor o igual a 0")
    Integer tiempoMaxEsperaMin,
    @Schema(description = "Color asociado al nivel.", example = "Amarillo")
    @Size(max = 20, message = "El color no puede superar los 20 caracteres")
    String color,
    @Schema(description = "Indica si el registro está activo. Si se omite al crear queda activo; false equivale a un borrado lógico.", example = "true")
    Boolean activo
) {}
