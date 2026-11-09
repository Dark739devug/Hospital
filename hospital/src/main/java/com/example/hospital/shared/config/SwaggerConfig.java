package com.example.hospital.shared.config;

import com.example.hospital.shared.exception.ErrorResponse;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.LinkedHashMap;
import java.util.Map;

@Configuration
@OpenAPIDefinition(
        info = @Info(title = "Hospital API", version = "1.0", description = """
                API REST para la gestión de un hospital: pacientes, citas, consultas, emergencias, \
                hospitalizaciones, farmacia y facturación. Está dividida en 8 módulos: Autenticación, \
                Administración, Pacientes, Clínica, Emergencias, Hospitalización, Farmacia y Facturación \
                (más Reportes).

                ## Cómo autenticarse
                1. Llama a `POST /api/auth/login` con `{"username": "...", "password": "..."}`.
                2. Copia el valor de `token` de la respuesta.
                3. Pulsa el botón **Authorize** (candado, arriba a la derecha), pega el token (sin la palabra \
                `Bearer`) y confirma.
                4. Desde ese momento Swagger envía `Authorization: Bearer <token>` en cada prueba (**Try it out**).

                Todos los endpoints exigen token salvo el login. El token dura 1 hora y no se renueva: al vencer \
                hay que iniciar sesión otra vez. Cada endpoint indica en su descripción qué roles pueden usarlo \
                (`ADMIN`, `MEDICO`, `ENFERMERO`, `RECEPCIONISTA`, `FARMACEUTICO`, `CAJERO`).

                ## Formato de errores
                Todos los errores devuelven el mismo JSON (esquema `ErrorResponse`): \
                `timestamp`, `status`, `error`, `message`, `path` y, solo en validaciones, `detalles` \
                (mensaje por campo).

                | Código | Significado |
                |---|---|
                | 400 | Datos inválidos: campo obligatorio, formato, tamaño, rango, JSON ilegible o parámetro con tipo incorrecto. |
                | 401 | Falta el token, es inválido, venció o el usuario está desactivado. |
                | 403 | El rol del usuario no tiene permiso para la operación. |
                | 404 | El recurso indicado en la URL no existe. |
                | 409 | Conflicto: duplicado o la operación choca con el estado actual o con registros dependientes. |
                | 422 | Regla de negocio incumplida (referencia inexistente o inactiva, estado o fechas incoherentes, stock o saldo insuficiente...). |

                ## Convenciones
                - Fechas `yyyy-MM-dd`, fecha y hora `yyyy-MM-ddTHH:mm:ss`, hora `HH:mm:ss`. Montos en quetzales (GTQ) \
                con dos decimales.
                - `DELETE` desactiva el registro (borrado lógico) y responde `204` sin cuerpo.
                - Los ejemplos de cada esquema usan ids de catálogos que existen tras ejecutar `Hosp_postgres.sql` \
                (por ejemplo `1`); para crear registros que dependen de otros (cita, consulta, factura...) crea primero \
                el registro del que dependen y usa su id.
                """),
        security = @SecurityRequirement(name = "Bearer Authentication"),
        // Servidor relativo: Swagger UI llama a la API en el mismo dominio y protocolo desde el que se abrio
        // (https en produccion, http://localhost:8080 en local), asi no hay contenido mixto.
        servers = @Server(url = "/", description = "Servidor actual")
)
@SecurityScheme(
        name = "Bearer Authentication",
        type = SecuritySchemeType.HTTP,
        bearerFormat = "JWT",
        scheme = "bearer",
        description = "Token JWT obtenido en POST /api/auth/login. Se pega sin el prefijo Bearer."
)
public class SwaggerConfig {

    private static final String REF_ERROR = "#/components/schemas/ErrorResponse";

    /**
     * Completa las respuestas de error de todas las operaciones: registra el esquema ErrorResponse, le da
     * contenido y ejemplos a cada respuesta 4xx/5xx declarada con @ApiResponse (que solo trae el texto) y
     * agrega el 401 en las operaciones protegidas que aún no lo declaran. Solo modifica la documentación.
     */
    @Bean
    public OpenApiCustomizer respuestasDeErrorComunes() {
        return openApi -> {
            if (openApi.getComponents() == null) {
                openApi.setComponents(new Components());
            }
            Map<String, Schema> esquemas = ModelConverters.getInstance().read(ErrorResponse.class);
            esquemas.forEach((nombre, esquema) -> openApi.getComponents().addSchemas(nombre, esquema));

            if (openApi.getPaths() == null) {
                return;
            }
            openApi.getPaths().forEach((ruta, item) -> item.readOperations().forEach(op -> {
                if (op.getResponses() == null) {
                    op.setResponses(new ApiResponses());
                }
                boolean login = ruta.endsWith("/auth/login");
                if (!login && !op.getResponses().containsKey("401")) {
                    op.getResponses().addApiResponse("401", new ApiResponse().description("Token ausente, inválido o expirado"));
                }
                op.getResponses().forEach((codigo, respuesta) -> {
                    if (codigo.startsWith("4") || codigo.startsWith("5")) {
                        respuesta.setContent(contenidoDeError(codigo, ruta));
                    }
                });
            }));
        };
    }

    private static Content contenidoDeError(String codigo, String ruta) {
        Map<String, Object> ejemplo = new LinkedHashMap<>();
        ejemplo.put("timestamp", "2026-03-15T10:30:00");
        ejemplo.put("status", Integer.parseInt(codigo));
        switch (codigo) {
            case "400" -> {
                ejemplo.put("error", "Bad Request");
                ejemplo.put("message", "Datos inválidos");
                Map<String, String> detalles = new LinkedHashMap<>();
                detalles.put("descripcion", "La descripción es obligatoria");
                ejemplo.put("path", ruta);
                ejemplo.put("detalles", detalles);
            }
            case "401" -> {
                ejemplo.put("error", "Unauthorized");
                ejemplo.put("message", "Token ausente, inválido o expirado");
                ejemplo.put("path", ruta);
            }
            case "403" -> {
                ejemplo.put("error", "Forbidden");
                ejemplo.put("message", "No tiene permisos para realizar esta acción");
                ejemplo.put("path", ruta);
            }
            case "404" -> {
                ejemplo.put("error", "Not Found");
                ejemplo.put("message", "No existe el registro con ID: 999");
                ejemplo.put("path", ruta);
            }
            case "409" -> {
                ejemplo.put("error", "Conflict");
                ejemplo.put("message", "Ya existe un registro con ese valor");
                ejemplo.put("path", ruta);
            }
            case "422" -> {
                ejemplo.put("error", "Unprocessable Entity");
                ejemplo.put("message", "El registro relacionado se encuentra inactivo");
                ejemplo.put("path", ruta);
            }
            default -> {
                ejemplo.put("error", "Internal Server Error");
                ejemplo.put("message", "Error interno del servidor");
                ejemplo.put("path", ruta);
            }
        }
        MediaType media = new MediaType().schema(new Schema<>().$ref(REF_ERROR));
        media.addExamples("Ejemplo", new Example().summary("Ejemplo de error " + codigo).value(ejemplo));
        return new Content().addMediaType("application/json", media);
    }
}
