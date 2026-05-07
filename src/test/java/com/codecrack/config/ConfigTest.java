package com.codecrack.config;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ConfigTest {

    @Test
    void openApiConfig_Bean_ReturnsOpenAPI() {
        OpenApiConfig config = new OpenApiConfig();
        OpenAPI openAPI = config.openAPI();
        assertNotNull(openAPI);
        assertNotNull(openAPI.getInfo());
        assertEquals("CodeCrack API", openAPI.getInfo().getTitle());
        assertEquals("1.0.0", openAPI.getInfo().getVersion());
    }

    @Test
    void openApiConfig_SecurityScheme_Configured() {
        OpenApiConfig config = new OpenApiConfig();
        OpenAPI openAPI = config.openAPI();
        assertNotNull(openAPI.getComponents());
        assertNotNull(openAPI.getComponents().getSecuritySchemes());
        assertTrue(openAPI.getComponents().getSecuritySchemes()
                .containsKey("Bearer Authentication"));
    }
}