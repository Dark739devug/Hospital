package com.example.hospital.habitacion.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record HabitacionDTO(
    @Schema(description = "Identificador del registro. Lo genera el servidor: no se envía al crear (se ignora).", example = "1")
    Long idHabitacion,
    @Schema(description = "Número de la habitación (único, máximo 10 caracteres).", example = "301")
    @NotBlank(message = "El número de habitación es obligatorio")
    @Size(max = 10, message = "El número no puede superar 10 caracteres")
    String numero,
    @Schema(description = "Piso donde está la habitación.", example = "3")
    @NotNull(message = "El piso es obligatorio")
    Integer piso,
    @Schema(description = "Cantidad máxima de pacientes (mayor a cero).", example = "2")
    @NotNull(message = "La capacidad es obligatoria")
    @Positive(message = "La capacidad debe ser mayor que cero")
    Integer capacidad,
    @Schema(description = "Id del tipo de habitación (1 = Individual, 2 = Doble, 4 = UCI...). Opcional.", example = "1")
    @NotNull(message = "El tipo de habitación es obligatorio")
    Long idTipoHabitacion,
    @Schema(description = "Id del estado de la habitación (1 = Disponible, 2 = Ocupada, 3 = Mantenimiento).", example = "1")
    @NotNull(message = "El estado de la habitación es obligatorio")
    Long idEstadoHabitacion,
    @Schema(description = "Indica si el registro está activo. Si se omite al crear queda activo; false equivale a un borrado lógico.", example = "true")
    Boolean activo
) {}
