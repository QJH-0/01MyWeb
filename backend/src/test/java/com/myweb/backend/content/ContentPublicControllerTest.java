package com.myweb.backend.content;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 公开内容页：按 page_key 返回 JSON sections 与 traceId。 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ContentPublicControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldReturnHomeContent() throws Exception {
        mockMvc.perform(get("/api/content/home"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Trace-Id"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").isString())
                .andExpect(jsonPath("$.data.sections").isArray())
                .andExpect(jsonPath("$.data.updatedAt").isString());
    }

    @Test
    void shouldReturnAboutContent() throws Exception {
        mockMvc.perform(get("/api/content/about"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").isString())
                .andExpect(jsonPath("$.data.sections").isArray())
                .andExpect(jsonPath("$.data.updatedAt").isString());
    }

    @Test
    void shouldReturnExperienceContent() throws Exception {
        mockMvc.perform(get("/api/content/experience"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").isString())
                .andExpect(jsonPath("$.data.sections").isArray())
                .andExpect(jsonPath("$.data.updatedAt").isString());
    }

    @Test
    void shouldReturnNotFoundWhenContentMissing() throws Exception {
        jdbcTemplate.update("DELETE FROM content_page WHERE page_key = ?", "home");

        mockMvc.perform(get("/api/content/home"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("NOT_FOUND"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }
}

