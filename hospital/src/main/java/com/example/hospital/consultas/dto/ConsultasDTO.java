package com.example.hospital.consultas.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ConsultasDTO(
    @Schema(description = "Identificador del registro. Lo genera el servidor: no se envía al crear (se ignora).", example = "1")
    Long idConsulta,
    @Schema(description = "Fecha y hora de la consulta (no futura), yyyy-MM-ddTHH:mm:ss.", example = "2026-03-15T10:30:00")
    LocalDateTime fechaHora,
    @Schema(description = "Síntomas referidos por el paciente.", example = "Dolor de cabeza y fiebre de dos días")
    String sintomas,
    @Schema(description = "Peso en kilogramos.", example = "68.50")
    @DecimalMin(value = "0", inclusive = false, message = "El peso debe ser mayor a cero")
    @Digits(integer = 3, fraction = 2, message = "El peso admite hasta 3 enteros y 2 decimales")
    BigDecimal peso,
    @Schema(description = "Talla en centímetros.", example = "165.00")
    @DecimalMin(value = "0", inclusive = false, message = "La talla debe ser mayor a cero")
    @Digits(integer = 3, fraction = 2, message = "La talla admite hasta 3 enteros y 2 decimales")
    BigDecimal talla,
    @Schema(description = "Temperatura en grados Celsius (un decimal).", example = "37.5")
    @DecimalMin(value = "30.0", message = "La temperatura debe estar entre 30.0 y 45.0 °C")
    @DecimalMax(value = "45.0", message = "La temperatura debe estar entre 30.0 y 45.0 °C")
    @Digits(integer = 2, fraction = 1, message = "La temperatura admite como máximo 1 decimal")
    BigDecimal temperatura,
    @Schema(description = "Presión arterial sistólica/diastólica.", example = "120/80")
    @Size(max = 10, message = "La presión arterial no puede superar 10 caracteres (ej. 120/80)")
    String presionArterial,
    @Schema(description = "Latidos por minuto.", example = "78")
    @Min(value = 20, message = "La frecuencia cardíaca debe estar entre 20 y 250 lpm")
    @Max(value = 250, message = "La frecuencia cardíaca debe estar entre 20 y 250 lpm")
    Integer frecuenciaCardiaca,
    @Schema(description = "Id de la cita médica (debe existir y estar activa).", example = "1")
    @NotNull(message = "La cita médica es obligatoria")
    Long idCita,
    @Schema(description = "Indica si el registro está activo. Si se omite al crear queda activo; false equivale a un borrado lógico.", example = "true")
    Boolean activo
) {}
