package com.reactur.backend.integration;

import com.reactur.backend.client.StructureApiClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import software.amazon.awssdk.services.s3.S3Client;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies the OpenAPI/Swagger documentation surface added in {@code OpenApiConfig} is actually
 * reachable without authentication (per {@code SecurityConfig}'s {@code permitAll()} on these
 * specific paths), while confirming this doesn't accidentally widen access to any real API
 * endpoint. Also verifies the generated OpenAPI description is genuinely generated from the real
 * running application (not a static fixture) by asserting on content that only exists if
 * springdoc actually introspected the real controllers/DTOs at startup.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "seaweedfs.s3.endpoint=http://localhost:8333",
                "seaweedfs.s3.access-key=integration-test-access-key",
                "seaweedfs.s3.secret-key=integration-test-secret-key",
                "external-api.reactur.base-url=http://localhost:8000"
        })
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OpenApiDocsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private StructureApiClient structureApiClient;

    @MockitoBean
    private S3Client s3Client;

    @Test
    void apiDocsJson_isReachable_withoutAuthentication() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("ReacTUr Backend API"))
                // Proves this is generated from the real running app, not a hand-written/static
                // file: /api/reactant only exists because springdoc introspected ReactantEndpoint.
                .andExpect(jsonPath("$.paths['/api/reactant']").exists());
    }

    @Test
    void swaggerUi_isReachable_withoutAuthentication() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());
    }

    @Test
    void apiDocsYaml_isReachable_withoutAuthentication() throws Exception {
        mockMvc.perform(get("/v3/api-docs.yaml"))
                .andExpect(status().isOk());
    }

    @Test
    void realApiEndpoint_stillRequiresAuthentication_despiteDocsBeingOpen() throws Exception {
        mockMvc.perform(get("/api/reactant"))
                .andExpect(status().isUnauthorized());
    }
}
