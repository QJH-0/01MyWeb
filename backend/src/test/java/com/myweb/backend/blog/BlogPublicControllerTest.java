package com.myweb.backend.blog;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 公开博客列表/详情：仅 PUBLISHED；草稿与软删对访客不可见。 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class BlogPublicControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldListOnlyPublishedAndNotDeleted() throws Exception {
        Long pubId = insertBlog("Pub", "pub-slug", "SUM", "web", "PUBLISHED", null, Instant.parse("2026-02-01T00:00:00Z"));
        insertTag(pubId, "Vue");

        insertBlog("Draft", "draft-slug", "SUM", "web", "DRAFT", null, null);
        insertBlog("Del", "del-slug", "SUM", "web", "PUBLISHED", Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-02-02T00:00:00Z"));

        mockMvc.perform(get("/api/blogs"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Trace-Id"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].title").value("Pub"))
                .andExpect(jsonPath("$.data.list[0].tags[0]").value("Vue"))
                .andExpect(jsonPath("$.data.list[0].status").value("PUBLISHED"));
    }

    @Test
    void shouldFilterByTag() throws Exception {
        Long a = insertBlog("A", "a-slug", "S", "web", "PUBLISHED", null, Instant.parse("2026-02-01T00:00:00Z"));
        insertTag(a, "Alpha");
        Long b = insertBlog("B", "b-slug", "S", "web", "PUBLISHED", null, Instant.parse("2026-01-02T00:00:00Z"));
        insertTag(b, "Beta");

        mockMvc.perform(get("/api/blogs").param("tag", "Beta"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].title").value("B"));
    }

    @Test
    void shouldReturnSlugDetailAndIncrementViews() throws Exception {
        Long id = insertBlog("T", "nice-slug", "S", "web", "PUBLISHED", null, Instant.parse("2026-02-01T00:00:00Z"));
        jdbcTemplate.update("UPDATE blog SET content = 'Hello', view_count = 3 WHERE id = ?", id);

        mockMvc.perform(get("/api/blogs/slug/nice-slug"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.slug").value("nice-slug"))
                .andExpect(jsonPath("$.data.content").value("Hello"))
                .andExpect(jsonPath("$.data.viewCount").value(4));
    }

    @Test
    void should404ForDraftOrDeleted() throws Exception {
        Long draftId = insertBlog("D", "d-slug", "S", "web", "DRAFT", null, null);
        Long delId = insertBlog("X", "x-slug", "S", "web", "PUBLISHED", Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-01-03T00:00:00Z"));

        mockMvc.perform(get("/api/blogs/" + draftId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));

        mockMvc.perform(get("/api/blogs/" + delId))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/blogs/slug/d-slug"))
                .andExpect(status().isNotFound());
    }

    private Long insertBlog(
            String title,
            String slug,
            String summary,
            String category,
            String status,
            Instant deletedAt,
            Instant publishedAt
    ) {
        Timestamp deletedTs = deletedAt == null ? null : Timestamp.from(deletedAt);
        Timestamp publishedTs = publishedAt == null ? null : Timestamp.from(publishedAt);
        jdbcTemplate.update("""
                        INSERT INTO blog
                        (title, slug, summary, content, category, status, cover_url, view_count, published_at,
                         created_at, updated_at, deleted_at)
                        VALUES (?, ?, ?, ?, ?, ?, NULL, 0, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, ?)
                        """,
                title, slug, summary, "body", category, status, publishedTs, deletedTs);
        return jdbcTemplate.queryForObject("SELECT id FROM blog WHERE slug = ?", Long.class, slug);
    }

    private void insertTag(Long blogId, String tag) {
        jdbcTemplate.update("INSERT INTO blog_tag (blog_id, tag) VALUES (?, ?)", blogId, tag);
    }
}
