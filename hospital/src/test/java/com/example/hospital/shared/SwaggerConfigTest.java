package com.example.hospital.shared;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.hospital.shared.config.SwaggerConfig;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.junit.jupiter.api.Test;

class SwaggerConfigTest {

    private OpenAPI documentoConUnaOperacion(String ruta, String... codigos) {
        Operation op = new Operation();
        ApiResponses rs = new ApiResponses();
        for (String c : codigos) {
            rs.addApiResponse(c, new ApiResponse().description("x"));
        }
        op.setResponses(rs);
        return new OpenAPI().paths(new Paths().addPathItem(ruta, new PathItem().get(op)));
    }

    @Test
    void respuestasDeError_recibenElEsquemaErrorResponseYEjemplos() {
        OpenAPI api = documentoConUnaOperacion("/api/diagnosticos/{id}", "200", "404", "422");

        new SwaggerConfig().respuestasDeErrorComunes().customise(api);

        ApiResponses rs = api.getPaths().get("/api/diagnosticos/{id}").getGet().getResponses();
        assertThat(api.getComponents().getSchemas()).containsKey("ErrorResponse");
        assertThat(rs.get("404").getContent().get("application/json").getSchema().get$ref())
                .isEqualTo("#/components/schemas/ErrorResponse");
        assertThat(rs.get("422").getContent().get("application/json").getExamples()).isNotEmpty();
        // la respuesta de éxito no se toca
        assertThat(rs.get("200").getContent()).isNull();
    }

    @Test
    void operacionesProtegidas_reciben401_peroElLoginNo() {
        OpenAPI api = documentoConUnaOperacion("/api/citas", "200");
        new SwaggerConfig().respuestasDeErrorComunes().customise(api);
        assertThat(api.getPaths().get("/api/citas").getGet().getResponses()).containsKey("401");

        OpenAPI login = documentoConUnaOperacion("/api/auth/login", "200");
        new SwaggerConfig().respuestasDeErrorComunes().customise(login);
        assertThat(login.getPaths().get("/api/auth/login").getGet().getResponses()).doesNotContainKey("401");
    }
}
