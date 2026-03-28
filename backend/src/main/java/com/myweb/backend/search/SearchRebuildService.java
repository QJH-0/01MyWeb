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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/** 全量重建索引：删索引后按当前库态重灌（仅已发布博客与可见项目）。 */
@Service
public class SearchRebuildService {

    private static final int PAGE = 100;

    private final SearchIndexOperations searchIndex;
    private final BlogRepository blogRepository;
    private final BlogTagRepository blogTagRepository;
    private final ProjectRepository projectRepository;
    private final ProjectTagRepository projectTagRepository;

    public SearchRebuildService(
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

    @Transactional(readOnly = true)
    public void rebuildAll() {
        searchIndex.deleteIndex();
        searchIndex.ensureIndex();
        int page = 0;
        Page<BlogEntity> blogs;
        do {
            blogs = blogRepository.findAllByDeletedAtIsNullAndStatus(
                    BlogStatus.PUBLISHED,
                    PageRequest.of(page, PAGE, Sort.by("id"))
            );
            for (BlogEntity blog : blogs.getContent()) {
                List<String> tags = loadBlogTags(blog.getId());
                searchIndex.indexBlogDocument(
                        blog.getId(),
                        blog.getTitle(),
                        blog.getSummary(),
                        blog.getContent() == null ? "" : blog.getContent(),
                        blog.getCategory(),
                        tags,
                        "/blog/" + blog.getId(),
                        blog.getPublishedAt(),
                        blog.getCreatedAt(),
                        blog.getUpdatedAt()
                );
            }
            page++;
        } while (blogs.hasNext());

        page = 0;
        Page<ProjectEntity> projects;
        do {
            projects = projectRepository.findAllByDeletedAtIsNullAndVisibleTrue(
                    PageRequest.of(page, PAGE, Sort.by("id"))
            );
            for (ProjectEntity project : projects.getContent()) {
                List<String> tags = loadProjectTags(project.getId());
                searchIndex.indexProjectDocument(
                        project.getId(),
                        project.getTitle(),
                        project.getSummary(),
                        project.getDescription() == null ? "" : project.getDescription(),
                        project.getCategory(),
                        tags,
                        "/projects/" + project.getId(),
                        project.getCreatedAt(),
                        project.getUpdatedAt()
                );
            }
            page++;
        } while (projects.hasNext());
    }

    private List<String> loadBlogTags(long blogId) {
        return blogTagRepository.findAllByBlogId(blogId).stream()
                .map(BlogTagEntity::getTag)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .sorted()
                .toList();
    }

    private List<String> loadProjectTags(long projectId) {
        return projectTagRepository.findAllByProjectId(projectId).stream()
                .map(ProjectTagEntity::getTag)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .sorted()
                .toList();
    }
}
