package be.ephec.padel.backend.controller;

import be.ephec.padel.backend.config.SecurityConfig;
import be.ephec.padel.backend.service.SiteService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = SiteController.class)
@Import(SecurityConfig.class)
class SiteControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SiteService siteService;

    @Test
    void whoami_sansAuthentification_retourne200_etBodyAttendu() throws Exception {
        mockMvc.perform(get("/api/v1/sites/_whoami"))
                .andExpect(status().isOk())
                .andExpect(content().string("SITE_CONTROLLER_V2"));
    }
}