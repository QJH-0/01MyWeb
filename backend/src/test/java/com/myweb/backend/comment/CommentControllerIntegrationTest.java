package com.myweb.backend.comment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 评论列表/创建/回复/点赞：目标须为已发布博客；匿名写接口走 Security 拒绝。 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CommentControllerIntegrationTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Test
    void shouldRejectAnonymousCreateWithUnauthorized() throws Exception {
        mockMvc.perform(post("/api/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"targetType":"blog","targetId":1,"content":"hi"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }

    @Test
    void shouldCreateReplyAndToggleLike() throws Exception {
        Long blogId = insertPublishedBlog("CmtPost", "cmt-slug", Instant.parse("2026-02-01T00:00:00Z"));
        String token = loginAsAdmin();

        mockMvc.perform(get("/api/comments")
                        .param("targetType", "blog")
                        .param("targetId", String.valueOf(blogId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(0));

        MvcResult created = mockMvc.perform(post("/api/comments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("""
                                {"targetType":"blog","targetId":%d,"content":"  root  "}
                                """, blogId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").value("root"))
                .andExpect(jsonPath("$.data.likeCount").value(0))
                .andReturn();

        long commentId = objectMapper.readTree(created.getResponse().getContentAsString()).at("/data/id").asLong();

        mockMvc.perform(post("/api/comments/" + commentId + "/reply")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"child\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.parentId").value(commentId))
                .andExpect(jsonPath("$.data.content").value("child"));

        mockMvc.perform(post("/api/comments/" + commentId + "/like")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.liked").value(true))
                .andExpect(jsonPath("$.data.likeCount").value(1));

        mockMvc.perform(post("/api/comments/" + commentId + "/like")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.liked").value(false))
                .andExpect(jsonPath("$.data.likeCount").value(0));

        mockMvc.perform(get("/api/comments")
                        .param("targetType", "blog")
                        .param("targetId", String.valueOf(blogId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(2));
    }

    @Test
    void should404WhenBlogNotPublished() throws Exception {
        Long draftId = insertBlogDraft("D", "d2-slug");
        String token = loginAsAdmin();

        mockMvc.perform(post("/api/comments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("""
                                {"targetType":"blog","targetId":%d,"content":"x"}
                                """, draftId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));

        mockMvc.perform(get("/api/comments")
                        .param("targetType", "blog")
                        .param("targetId", String.valueOf(draftId)))
                .andExpect(status().isNotFound());
    }

    private Long insertPublishedBlog(String title, String slug, Instant publishedAt) {
        Timestamp publishedTs = Timestamp.from(publishedAt);
        jdbcTemplate.update("""
                        INSERT INTO blog
                        (title, slug, summary, content, category, status, cover_url, view_count, published_at,
                         created_at, updated_at, deleted_at)
                        VALUES (?, ?, ?, ?, ?, ?, NULL, 0, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL)
                        """,
                title, slug, "S", "body", "web", "PUBLISHED", publishedTs);
        return jdbcTemplate.queryForObject("SELECT id FROM blog WHERE slug = ?", Long.class, slug);
    }

    private Long insertBlogDraft(String title, String slug) {
        jdbcTemplate.update("""
                        INSERT INTO blog
                        (title, slug, summary, content, category, status, cover_url, view_count, published_at,
                         created_at, updated_at, deleted_at)
                        VALUES (?, ?, ?, ?, ?, ?, NULL, 0, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL)
                        """,
                title, slug, "S", "body", "web", "DRAFT");
        return jdbcTemplate.queryForObject("SELECT id FROM blog WHERE slug = ?", Long.class, slug);
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
