package com.myweb.backend.search;

import com.myweb.backend.common.BlogStatus;
import com.myweb.backend.entity.BlogEntity;
import com.myweb.backend.entity.BlogTagEntity;
import com.myweb.backend.entity.ProjectEntity;
import com.myweb.backend.entity.ProjectTagEntity;
import com.myweb.backend.repository.BlogRepository;
import com.myweb.backend.repository.BlogTagRepository;
import com.myweb.backend.repository.ProjectRepository;
import com.myweb.backend.repository.ProjectTagRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

/** 将单条 outbox 映射为 ES 删除或按当前库态重建索引。 */
@Component
public class SearchOutboxProcessor {

    private final SearchIndexOperations searchIndex;
    private final BlogRepository blogRepository;
    private final BlogTagRepository blogTagRepository;
    private final ProjectRepository projectRepository;
    private final ProjectTagRepository projectTagRepository;

    public SearchOutboxProcessor(
            SearchIndexOperations searchIndex,
            BlogRepository blogRepository,
            BlogTagRepository blogTagRepository,
            ProjectRepository projectRepository,
            ProjectTagRepository projectTagRepository
    ) {
        this.searchIndex = searchIndex;
        this.blogRepository = blogRepository;
        this.blogTagRepository = blogTagRepository;
        this.projectRepository = projectRepository;
        this.projectTagRepository = projectTagRepository;
    }

    public void process(SearchOutboxEntity row) {
        String agg = row.getAggregateType();
        long id = row.getAggregateId();
        if ("deleted".equals(row.getEventType())) {
            searchIndex.deleteDocument(agg, id);
            return;
        }
        if ("blog".equals(agg)) {
            syncBlog(id);
        } else if ("project".equals(agg)) {
            syncProject(id);
        }
    }

    private void syncBlog(long id) {
        BlogEntity blog = blogRepository.findByIdAndDeletedAtIsNull(id).orElse(null);
        if (blog == null || blog.getDeletedAt() != null) {
            searchIndex.deleteDocument("blog", id);
            return;
        }
        if (!BlogStatus.PUBLISHED.equals(blog.getStatus())) {
            searchIndex.deleteDocument("blog", id);
            return;
        }
        List<String> tags = blogTagRepository.findAllByBlogId(id).stream()
                .map(BlogTagEntity::getTag)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .sorted()
                .toList();
        String url = "/blog/" + blog.getId();
        searchIndex.indexBlogDocument(
                blog.getId(),
                blog.getTitle(),
                blog.getSummary(),
                blog.getContent() == null ? "" : blog.getContent(),
                blog.getCategory(),
                tags,
                url,
                blog.getPublishedAt(),
                blog.getCreatedAt(),
                blog.getUpdatedAt()
        );
    }

    private void syncProject(long id) {
        ProjectEntity project = projectRepository.findByIdAndDeletedAtIsNull(id).orElse(null);
        if (project == null || project.getDeletedAt() != null) {
            searchIndex.deleteDocument("project", id);
            return;
        }
        if (!project.isVisible()) {
            searchIndex.deleteDocument("project", id);
            return;
        }
        List<String> tags = projectTagRepository.findAllByProjectId(id).stream()
                .map(ProjectTagEntity::getTag)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .sorted()
                .toList();
        String url = "/projects/" + project.getId();
        searchIndex.indexProjectDocument(
                project.getId(),
                project.getTitle(),
                project.getSummary(),
                project.getDescription() == null ? "" : project.getDescription(),
                project.getCategory(),
                tags,
                url,
                project.getCreatedAt(),
                project.getUpdatedAt()
        );
    }
}
