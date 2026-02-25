package be.ephec.padel.backend.controller.contract;

import be.ephec.padel.backend.controller.MatchController;
import be.ephec.padel.backend.exception.NotFoundException;
import be.ephec.padel.backend.service.MatchPadelService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MatchController.class)
class MatchControllerContract404Test {

    @Autowired MockMvc mockMvc;
    @MockitoBean  MatchPadelService matchPadelService;

    @Test
    @WithMockUser // <-- ajout
    void getMatch_notFound_returns404_withApiErrorFormat() throws Exception {
        when(matchPadelService.getMatchDto(999L))
                .thenThrow(new NotFoundException("Match 999 introuvable"));

        mockMvc.perform(get("/api/v1/matchs/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Match 999 introuvable"))
                .andExpect(jsonPath("$.path").value("/api/v1/matchs/999"));
    }
}