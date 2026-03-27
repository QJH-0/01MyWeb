package com.myweb.backend.project;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myweb.backend.config.SecurityProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdminProjectControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private SecurityProperties securityProperties;

    @Test
    void shouldCreateUpdateAndSoftDeleteProject() throws Exception {
        String adminAccessToken = loginAsAdminAndGetAccessToken();

        MvcResult createResult = mockMvc.perform(post("/api/admin/projects")
                        .header("Authorization", "Bearer " + adminAccessToken)
                        .header("X-Admin-Token", securityProperties.admin().token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "P1",
                                  "summary": "S1",
                                  "description": "D1",
                                  "category": "web",
                                  "tags": ["Vue", "Spring"],
                                  "coverUrl": "https://example.com/cover.png",
                                  "projectUrl": "https://example.com",
                                  "sourceUrl": "https://example.com/repo",
                                  "sortOrder": 10,
                                  "visible": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Trace-Id"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").isNumber())
                .andExpect(jsonPath("$.data.tags").isArray())
                .andReturn();

        JsonNode createdBody = objectMapper.readTree(createResult.getResponse().getContentAsString());
        long id = createdBody.at("/data/id").asLong();

        mockMvc.perform(get("/api/admin/projects/" + id)
                        .header("Authorization", "Bearer " + adminAccessToken)
                        .header("X-Admin-Token", securityProperties.admin().token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("P1"))
                .andExpect(jsonPath("$.data.tags").isArray());

        mockMvc.perform(put("/api/admin/projects/" + id)
                        .header("Authorization", "Bearer " + adminAccessToken)
                        .header("X-Admin-Token", securityProperties.admin().token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "",
                                  "summary": "S1-updated",
                                  "description": "D1-updated",
                                  "category": "web",
                                  "tags": ["Java"],
                                  "visible": false
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));

        mockMvc.perform(put("/api/admin/projects/" + id)
                        .header("Authorization", "Bearer " + adminAccessToken)
                        .header("X-Admin-Token", securityProperties.admin().token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "P1-updated",
                                  "summary": "S1-updated",
                                  "description": "D1-updated",
                                  "category": "web",
                                  "tags": ["Java"],
                                  "coverUrl": null,
                                  "projectUrl": null,
                                  "sourceUrl": null,
                                  "sortOrder": 1,
                                  "visible": false
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.visible").value(false))
                .andExpect(jsonPath("$.data.tags[0]").value("Java"));

        mockMvc.perform(get("/api/projects/" + id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));

        mockMvc.perform(delete("/api/admin/projects/" + id)
                        .header("Authorization", "Bearer " + adminAccessToken)
                        .header("X-Admin-Token", securityProperties.admin().token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/api/admin/projects/" + id)
                        .header("Authorization", "Bearer " + adminAccessToken)
                        .header("X-Admin-Token", securityProperties.admin().token()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    private String loginAsAdminAndGetAccessToken() throws Exception {
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

