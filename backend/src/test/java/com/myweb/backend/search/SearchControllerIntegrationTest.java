package com.myweb.backend.search;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** GET /api/search：参数校验与测试 profile 下 ES 关闭时的错误码。 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SearchControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldRejectMissingQuery() throws Exception {
        mockMvc.perform(get("/api/search"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void shouldRejectBlankQuery() throws Exception {
        mockMvc.perform(get("/api/search").param("q", "   "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void shouldRejectInvalidType() throws Exception {
        mockMvc.perform(get("/api/search").param("q", "x").param("type", "bad"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void shouldReturn500WhenElasticsearchDisabledInTestProfile() throws Exception {
        mockMvc.perform(get("/api/search").param("q", "test").param("page", "0").param("limit", "10"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("INTERNAL_ERROR"));
    }
}
