package com.myweb.backend.project;

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

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ProjectPublicControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldListOnlyVisibleAndNotDeletedProjects() throws Exception {
        Long visibleId = insertProject("Visible", "visible summary", "web", true, null);
        insertTag(visibleId, "Vue");

        Long invisibleId = insertProject("Invisible", "invisible summary", "web", false, null);
        insertTag(invisibleId, "Hidden");

        insertProject("Deleted", "deleted summary", "web", true, Instant.parse("2026-01-01T00:00:00Z"));

        mockMvc.perform(get("/api/projects"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Trace-Id"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.list").isArray())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].title").value("Visible"))
                .andExpect(jsonPath("$.data.list[0].tags[0]").value("Vue"));
    }

    @Test
    void shouldReturnNotFoundForInvisibleOrDeletedProject() throws Exception {
        Long invisibleId = insertProject("Invisible", "invisible summary", "web", false, null);
        Long deletedId = insertProject("Deleted", "deleted summary", "web", true, Instant.parse("2026-01-01T00:00:00Z"));

        mockMvc.perform(get("/api/projects/" + invisibleId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));

        mockMvc.perform(get("/api/projects/" + deletedId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    private Long insertProject(String title, String summary, String category, boolean visible, Instant deletedAt) {
        Timestamp deletedAtTs = deletedAt == null ? null : Timestamp.from(deletedAt);
        jdbcTemplate.update("""
                INSERT INTO project
                (title, summary, description, category, cover_url, project_url, source_url, visible, sort_order, created_at, updated_at, deleted_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, ?)
                """, title, summary, null, category, null, null, null, visible ? 1 : 0, 0, deletedAtTs);
        return jdbcTemplate.queryForObject("SELECT id FROM project WHERE title = ?", Long.class, title);
    }

    private void insertTag(Long projectId, String tag) {
        jdbcTemplate.update("INSERT INTO project_tag (project_id, tag) VALUES (?, ?)", projectId, tag);
    }
}

