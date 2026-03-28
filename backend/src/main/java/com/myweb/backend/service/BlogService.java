package com.myweb.backend.service;

import com.myweb.backend.common.ApiException;
import com.myweb.backend.common.BlogStatus;
import com.myweb.backend.common.ErrorCodes;
import com.myweb.backend.common.PagedResult;
import com.myweb.backend.dto.BlogCreateRequest;
import com.myweb.backend.dto.BlogResponseDTO;
import com.myweb.backend.dto.BlogUpdateRequest;
import com.myweb.backend.entity.BlogEntity;
import com.myweb.backend.entity.BlogTagEntity;
import com.myweb.backend.repository.BlogRepository;
import com.myweb.backend.repository.BlogTagRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 博客聚合：公开侧仅 PUBLISHED 且未删；管理端全状态；发布时首次写入 {@code publishedAt}。
 */
@Service
public class BlogService {
    private static final int MAX_LIMIT = 100;

    private final BlogRepository blogRepository;
    private final BlogTagRepository blogTagRepository;

    public BlogService(BlogRepository blogRepository, BlogTagRepository blogTagRepository) {
        this.blogRepository = blogRepository;
        this.blogTagRepository = blogTagRepository;
    }

    public PagedResult<BlogResponseDTO> listPublic(String category, String tag, int page, int limit) {
        Pageable pageable = toPageablePublic(page, limit);
        String cat = blankToNull(category);
        String t = blankToNull(tag);
        Page<BlogEntity> pageData = blogRepository.findPublished(cat, t, pageable);
        List<BlogResponseDTO> items = withTags(pageData.getContent());
        return new PagedResult<>(items, pageData.getTotalElements(), page, limit);
    }

    @Transactional
    public BlogResponseDTO getPublic(Long id) {
        BlogEntity entity = blogRepository.findByIdAndDeletedAtIsNullAndStatus(id, BlogStatus.PUBLISHED)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ErrorCodes.NOT_FOUND, "Blog not found"));
        incrementViewCount(entity);
        List<String> tags = loadSortedTags(entity.getId());
        return toDTO(entity, tags);
    }

    @Transactional
    public BlogResponseDTO getPublicBySlug(String slug) {
        String normalized = normalizeSlug(slug);
        BlogEntity entity = blogRepository.findBySlugAndDeletedAtIsNullAndStatus(normalized, BlogStatus.PUBLISHED)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ErrorCodes.NOT_FOUND, "Blog not found"));
        incrementViewCount(entity);
        List<String> tags = loadSortedTags(entity.getId());
        return toDTO(entity, tags);
    }

    public PagedResult<BlogResponseDTO> listAdmin(String status, String category, int page, int limit) {
        Pageable pageable = toPageableAdmin(page, limit);
        String st = blankToNull(status);
        if (st != null && !BlogStatus.DRAFT.equals(st) && !BlogStatus.PUBLISHED.equals(st)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCodes.VALIDATION_ERROR, "Invalid status filter");
        }
        String cat = blankToNull(category);
        Page<BlogEntity> pageData = blogRepository.findAdminVisible(st, cat, pageable);
        List<BlogResponseDTO> items = withTags(pageData.getContent());
        return new PagedResult<>(items, pageData.getTotalElements(), page, limit);
    }

    public BlogResponseDTO getAdmin(Long id) {
        BlogEntity entity = blogRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ErrorCodes.NOT_FOUND, "Blog not found"));
        return toDTO(entity, loadSortedTags(entity.getId()));
    }

    @Transactional
    public BlogResponseDTO create(BlogCreateRequest request) {
        String slug = normalizeSlug(request.slug());
        assertSlugAvailable(slug, null);

        BlogEntity entity = new BlogEntity();
        applyWritableFields(entity, request.title(), slug, request.summary(), request.category(),
                request.content(), request.coverUrl());
        entity.setStatus(BlogStatus.DRAFT);
        entity.setViewCount(0);

        BlogEntity saved = blogRepository.save(entity);
        replaceTags(saved.getId(), request.tags());
        return getAdmin(saved.getId());
    }

    @Transactional
    public BlogResponseDTO update(Long id, BlogUpdateRequest request) {
        BlogEntity entity = blogRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ErrorCodes.NOT_FOUND, "Blog not found"));
        String slug = normalizeSlug(request.slug());
        if (!slug.equals(entity.getSlug())) {
            assertSlugAvailable(slug, id);
        }
        applyWritableFields(entity, request.title(), slug, request.summary(), request.category(),
                request.content(), request.coverUrl());
        blogRepository.save(entity);
        replaceTags(entity.getId(), request.tags());
        return getAdmin(entity.getId());
    }

    @Transactional
    public void delete(Long id) {
        BlogEntity entity = blogRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ErrorCodes.NOT_FOUND, "Blog not found"));
        entity.setDeletedAt(Instant.now());
        blogRepository.save(entity);
    }

    @Transactional
    public BlogResponseDTO publish(Long id) {
        BlogEntity entity = blogRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ErrorCodes.NOT_FOUND, "Blog not found"));
        String body = entity.getContent();
        if (body == null || body.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCodes.VALIDATION_ERROR, "Content required before publish");
        }
        entity.setStatus(BlogStatus.PUBLISHED);
        if (entity.getPublishedAt() == null) {
            entity.setPublishedAt(Instant.now());
        }
        blogRepository.save(entity);
        return getAdmin(id);
    }

    @Transactional
    public BlogResponseDTO unpublish(Long id) {
        BlogEntity entity = blogRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ErrorCodes.NOT_FOUND, "Blog not found"));
        entity.setStatus(BlogStatus.DRAFT);
        blogRepository.save(entity);
        return getAdmin(id);
    }

    private void incrementViewCount(BlogEntity entity) {
        entity.setViewCount(entity.getViewCount() + 1);
        blogRepository.save(entity);
    }

    private void assertSlugAvailable(String slug, Long excludeId) {
        boolean taken = excludeId == null
                ? blogRepository.existsBySlugAndDeletedAtIsNull(slug)
                : blogRepository.existsBySlugAndDeletedAtIsNullAndIdNot(slug, excludeId);
        if (taken) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCodes.VALIDATION_ERROR, "Slug already exists");
        }
    }

    private static String normalizeSlug(String raw) {
        return raw.trim().toLowerCase(Locale.ROOT);
    }

    private static void applyWritableFields(
            BlogEntity entity,
            String title,
            String slug,
            String summary,
            String category,
            String content,
            String coverUrl
    ) {
        entity.setTitle(title.trim());
        entity.setSlug(slug);
        entity.setSummary(summary.trim());
        entity.setCategory(blankToNull(category));
        entity.setContent(content.trim());
        entity.setCoverUrl(validateOptionalUrl(blankToNull(coverUrl), "coverUrl"));
    }

    private Pageable toPageablePublic(int page, int limit) {
        if (page < 0 || limit <= 0 || limit > MAX_LIMIT) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCodes.VALIDATION_ERROR, "Invalid pagination params");
        }
        Sort sort = Sort.by(Sort.Order.desc("publishedAt"), Sort.Order.desc("id"));
        return PageRequest.of(page, limit, sort);
    }

    private Pageable toPageableAdmin(int page, int limit) {
        if (page < 0 || limit <= 0 || limit > MAX_LIMIT) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCodes.VALIDATION_ERROR, "Invalid pagination params");
        }
        Sort sort = Sort.by(Sort.Order.desc("updatedAt"), Sort.Order.desc("id"));
        return PageRequest.of(page, limit, sort);
    }

    private List<BlogResponseDTO> withTags(Collection<BlogEntity> blogs) {
        if (blogs.isEmpty()) {
            return List.of();
        }
        List<Long> ids = blogs.stream().map(BlogEntity::getId).filter(Objects::nonNull).toList();
        Map<Long, List<String>> tagsByBlogId = blogTagRepository.findAllByBlogIdIn(ids).stream()
                .filter(row -> row.getBlogId() != null && row.getTag() != null)
                .collect(Collectors.groupingBy(
                        BlogTagEntity::getBlogId,
                        Collectors.mapping(BlogTagEntity::getTag, Collectors.toList())
                ));

        return blogs.stream()
                .map(blog -> {
                    List<String> tagList = tagsByBlogId.getOrDefault(blog.getId(), List.of()).stream()
                            .filter(Objects::nonNull)
                            .map(String::trim)
                            .filter(s -> !s.isEmpty())
                            .distinct()
                            .sorted()
                            .toList();
                    return toDTO(blog, tagList);
                })
                .toList();
    }

    private List<String> loadSortedTags(Long blogId) {
        return blogTagRepository.findAllByBlogId(blogId).stream()
                .map(BlogTagEntity::getTag)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(t -> !t.isEmpty())
                .distinct()
                .sorted()
                .toList();
    }

    private void replaceTags(Long blogId, List<String> tags) {
        blogTagRepository.deleteAllByBlogId(blogId);
        List<BlogTagEntity> rows = tags.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(tag -> !tag.isEmpty())
                .distinct()
                .map(tag -> new BlogTagEntity(blogId, tag))
                .toList();
        blogTagRepository.saveAll(rows);
    }

    private static BlogResponseDTO toDTO(BlogEntity entity, List<String> tags) {
        return new BlogResponseDTO(
                entity.getId(),
                entity.getTitle(),
                entity.getSlug(),
                entity.getSummary(),
                entity.getContent(),
                entity.getCategory(),
                tags,
                entity.getStatus(),
                entity.getCoverUrl(),
                entity.getViewCount(),
                entity.getPublishedAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getDeletedAt()
        );
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String validateOptionalUrl(String url, String fieldName) {
        if (url == null) {
            return null;
        }
        if (url.startsWith("http://") || url.startsWith("https://")) {
            return url;
        }
        throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCodes.VALIDATION_ERROR, "Invalid url field: " + fieldName);
    }
}
