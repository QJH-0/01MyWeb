package com.myweb.backend.blog;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myweb.backend.config.SecurityProperties;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 管理端博客 API：JWT + admin token、发布流与 slug 冲突。 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdminBlogControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private SecurityProperties securityProperties;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private EntityManager entityManager;

    @Test
    void shouldCreatePublishUnpublishAndSoftDelete() throws Exception {
        String token = loginAsAdmin();

        MvcResult createResult = mockMvc.perform(post("/api/admin/blogs")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Admin-Token", securityProperties.admin().token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Hello",
                                  "slug": "hello-post",
                                  "summary": "S",
                                  "category": "dev",
                                  "tags": ["Java"],
                                  "content": " # Title\\nbody",
                                  "coverUrl": "https://example.com/c.png"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andReturn();

        long id = objectMapper.readTree(createResult.getResponse().getContentAsString()).at("/data/id").asLong();

        mockMvc.perform(get("/api/blogs/" + id))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/admin/blogs/" + id + "/publish")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Admin-Token", securityProperties.admin().token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.data.publishedAt").isNotEmpty());

        mockMvc.perform(get("/api/blogs/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Hello"));

        mockMvc.perform(post("/api/admin/blogs/" + id + "/unpublish")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Admin-Token", securityProperties.admin().token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DRAFT"));

        mockMvc.perform(delete("/api/admin/blogs/" + id)
                        .header("Authorization", "Bearer " + token)
                        .header("X-Admin-Token", securityProperties.admin().token()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/admin/blogs/" + id)
                        .header("Authorization", "Bearer " + token)
                        .header("X-Admin-Token", securityProperties.admin().token()))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldRejectDuplicateSlugOnCreate() throws Exception {
        String token = loginAsAdmin();

        mockMvc.perform(post("/api/admin/blogs")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Admin-Token", securityProperties.admin().token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "A",
                                  "slug": "dup",
                                  "summary": "S",
                                  "category": "c",
                                  "tags": ["t"],
                                  "content": "x"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/admin/blogs")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Admin-Token", securityProperties.admin().token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "B",
                                  "slug": "dup",
                                  "summary": "S",
                                  "category": "c",
                                  "tags": ["t"],
                                  "content": "y"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void shouldRejectPublishWhenContentBlank() throws Exception {
        String token = loginAsAdmin();

        MvcResult createResult = mockMvc.perform(post("/api/admin/blogs")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Admin-Token", securityProperties.admin().token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "C",
                                  "slug": "c-blank",
                                  "summary": "S",
                                  "category": "c",
                                  "tags": ["t"],
                                  "content": "initial"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();
        long id = objectMapper.readTree(createResult.getResponse().getContentAsString()).at("/data/id").asLong();

        mockMvc.perform(put("/api/admin/blogs/" + id)
                        .header("Authorization", "Bearer " + token)
                        .header("X-Admin-Token", securityProperties.admin().token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "C",
                                  "slug": "c-blank",
                                  "summary": "S",
                                  "category": "c",
                                  "tags": ["t"],
                                  "content": "   "
                                }
                                """))
                .andExpect(status().isBadRequest());

        jdbcTemplate.update("UPDATE blog SET content = '' WHERE id = ?", id);
        entityManager.clear();

        mockMvc.perform(post("/api/admin/blogs/" + id + "/publish")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Admin-Token", securityProperties.admin().token()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    private String loginAsAdmin() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username":"admin",
                                  "password":"Admin12345"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        return body.at("/data/accessToken").asText();
    }
}
