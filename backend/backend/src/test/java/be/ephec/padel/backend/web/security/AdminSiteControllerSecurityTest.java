package be.ephec.padel.backend.web.security;

import be.ephec.padel.backend.config.SecurityConfig;
import be.ephec.padel.backend.controller.AdminSiteController;
import be.ephec.padel.backend.service.AdminSiteService;
import be.ephec.padel.backend.service.AdminSiteStatsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AdminSiteController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
        "app.security.admin.global.username=adminGlobal",
        "app.security.admin.global.password=test123",
        "app.security.admin.site.password=test123",
        "app.security.admin.site.users=adminSite1:1,adminSite2:2"
})
class AdminSiteControllerSecurityTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    AdminSiteService adminSiteService;

    @MockitoBean
    AdminSiteStatsService adminSiteStatsService;

    @Test
    void sansAuth_401() throws Exception {
        mockMvc.perform(get("/api/v1/admin/sites/1/joueurs"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "adminSite1", roles = {"ADMIN_SITE"})
    void adminSite_auth_200_mapping_ok() throws Exception {
        mockMvc.perform(get("/api/v1/admin/sites/1/joueurs"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "adminGlobal", roles = {"ADMIN_GLOBAL"})
    void adminGlobal_auth_200() throws Exception {
        mockMvc.perform(get("/api/v1/admin/sites/2/joueurs"))
                .andExpect(status().isOk());
    }
}
