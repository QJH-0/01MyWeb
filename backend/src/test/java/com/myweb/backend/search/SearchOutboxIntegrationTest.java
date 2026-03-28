package com.myweb.backend.search;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myweb.backend.dto.BlogCreateRequest;
import com.myweb.backend.dto.BlogResponseDTO;
import com.myweb.backend.service.BlogService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 博客发布时写入 outbox；管理端 consume 可处理 pending（测试环境 ES 关闭，索引为 no-op）。 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SearchOutboxIntegrationTest {

    @Autowired
    private BlogService blogService;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void publishShouldInsertOutboxRows() {
        String slug = "outbox-" + System.nanoTime();
        BlogResponseDTO created = blogService.create(new BlogCreateRequest(
                "Outbox",
                slug,
                "summary",
                "cat",
                List.of("t"),
                "content body here",
                null
        ));
        blogService.publish(created.id());
        Long n = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM search_outbox_event WHERE aggregate_type = 'blog' AND aggregate_id = ?",
                Long.class,
                created.id()
        );
        assertThat(n).isGreaterThanOrEqualTo(1);
    }

    @Test
    void adminConsumeReturnsProcessedCount() throws Exception {
        String token = loginAsAdmin();
        mockMvc.perform(post("/api/admin/search/consume")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Admin-Token", "test-admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.processed").exists());
    }

    private String loginAsAdmin() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"admin","password":"Admin12345"}
                                """))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        return body.at("/data/accessToken").asText();
    }
}
