package be.ephec.padel.backend.integration;

import be.ephec.padel.backend.support.SqlServerTestContainerConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "app.security.admin.global.username=adminGlobal",
        "app.security.admin.global.password=test123",
        "app.security.admin.site.users=",
        "app.security.admin.site.password=test123"
})
class OpenApiSecurityIntegrationTest extends SqlServerTestContainerConfig {

    @Autowired
    MockMvc mvc;

    @Test
    void openapi_expose_bearer_auth_globalement_et_laisse_auth_public() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.type").value("http"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.bearerFormat").value("JWT"))
                .andExpect(jsonPath("$.security[0].bearerAuth").exists())
                .andExpect(jsonPath("$['paths']['/api/v1/auth/login']['post']['security']").isArray())
                .andExpect(jsonPath("$['paths']['/api/v1/auth/login']['post']['security']").isEmpty())
                .andExpect(jsonPath("$['paths']['/api/v1/auth/register']['post']['security']").isArray())
                .andExpect(jsonPath("$['paths']['/api/v1/auth/register']['post']['security']").isEmpty());
    }

    @Test
    void openapi_fait_heriter_la_securite_globale_aux_endpoints_sites_terrains_et_fermetures() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$['paths']['/api/v1/sites']['get']['security']").doesNotExist())
                .andExpect(jsonPath("$['paths']['/api/v1/sites/{id}']['get']['security']").doesNotExist())
                .andExpect(jsonPath("$['paths']['/api/v1/terrains']['get']['security']").doesNotExist())
                .andExpect(jsonPath("$['paths']['/api/v1/terrains/{id}']['get']['security']").doesNotExist())
                .andExpect(jsonPath("$['paths']['/api/v1/fermetures-globales']['get']['security']").doesNotExist());
    }
}
