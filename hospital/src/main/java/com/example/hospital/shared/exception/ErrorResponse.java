package com.example.hospital.shared.exception;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "ErrorResponse", description = "Cuerpo JSON estándar de todos los errores de la API (400, 401, 403, 404, 409, 422 y 500).")
public class ErrorResponse {

    @Schema(description = "Fecha y hora del servidor en que ocurrió el error (ISO-8601, sin zona).", example = "2026-03-15T10:30:00")
    private LocalDateTime timestamp;
    @Schema(description = "Código HTTP de la respuesta.", example = "422")
    private int status;
    @Schema(description = "Texto estándar del código HTTP.", example = "Unprocessable Entity")
    private String error;
    @Schema(description = "Explicación del error en español, lista para mostrarse al usuario.", example = "El diagnóstico con ID 5 se encuentra inactivo.")
    private String message;
    @Schema(description = "Ruta de la petición que produjo el error.", example = "/api/tratamiento")
    private String path;
    @Schema(description = "Solo en errores de validación (400): mensaje de cada campo inválido, indexado por el nombre del campo. En los demás errores no viene o es nulo.", example = "{\"descripcion\": \"La descripción del tratamiento es obligatoria\"}")
    private Map<String, String> detalles;
}
