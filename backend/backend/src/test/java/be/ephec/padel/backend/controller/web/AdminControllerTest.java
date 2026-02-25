package be.ephec.padel.backend.controller.web;

import be.ephec.padel.backend.config.SecurityConfig;
import be.ephec.padel.backend.controller.AdminController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AdminController.class)
@Import(SecurityConfig.class)
class AdminControllerTest {

    @Autowired
    MockMvc mvc;

    @Test
    void adminInfo_sansAuth_401() throws Exception {
        mvc.perform(get("/api/v1/admin/info"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminInfo_avecAuth_200_et_json() throws Exception {
        mvc.perform(get("/api/v1/admin/info")
                        .with(httpBasic("admin", "admin123")))
                .andExpect(status().isOk())
                .andExpect(content().contentType(org.springframework.http.MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("ok"));
    }
    @Test
    void adminInfo_mauvaisPassword_401() throws Exception {
        mvc.perform(get("/api/v1/admin/info")
                        .with(httpBasic("admin", "wrong")))
                .andExpect(status().isUnauthorized());
    }
}