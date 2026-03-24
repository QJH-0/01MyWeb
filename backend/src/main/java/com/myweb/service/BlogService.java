package com.myweb.service;

import com.myweb.common.NotFoundException;
import com.myweb.dto.BlogCreateRequest;
import com.myweb.dto.BlogResponseDTO;
import com.myweb.dto.BlogUpdateRequest;
import com.myweb.entity.Blog;
import com.myweb.entity.BlogStatus;
import com.myweb.repository.BlogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class BlogService {

    private static final Logger log = LoggerFactory.getLogger(BlogService.class);
    private final BlogRepository blogRepository;
    private final BlogSearchSyncPublisher blogSearchSyncPublisher;

    public BlogService(BlogRepository blogRepository, BlogSearchSyncPublisher blogSearchSyncPublisher) {
        this.blogRepository = blogRepository;
        this.blogSearchSyncPublisher = blogSearchSyncPublisher;
    }

    @Transactional(readOnly = true)
    public Page<BlogResponseDTO> listPublic(String category, int page, int limit) {
        Pageable pageable = PageRequest.of(page, limit, Sort.by(Sort.Direction.DESC, "id"));
        if (category != null && !category.isBlank()) {
            return blogRepository
                .findAllByStatusAndCategoryContainingIgnoreCase(BlogStatus.PUBLISHED, category, pageable)
                .map(this::toDto);
        }
        return blogRepository.findAllByStatus(BlogStatus.PUBLISHED, pageable).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public BlogResponseDTO getPublic(Long id) {
        Blog blog = blogRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("博客不存在"));
        if (blog.getStatus() != BlogStatus.PUBLISHED) {
            throw new NotFoundException("博客未发布");
        }
        return toDto(blog);
    }

    @Transactional(readOnly = true)
    public Page<BlogResponseDTO> listAdmin(String category, int page, int limit) {
        Pageable pageable = PageRequest.of(page, limit, Sort.by(Sort.Direction.DESC, "id"));
        if (category != null && !category.isBlank()) {
            return blogRepository.findAllByCategoryContainingIgnoreCase(category, pageable).map(this::toDto);
        }
        return blogRepository.findAll(pageable).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public Page<BlogResponseDTO> listAdminByStatus(String status, String category, int page, int limit) {
        Pageable pageable = PageRequest.of(page, limit, Sort.by(Sort.Direction.DESC, "id"));
        BlogStatus blogStatus = BlogStatus.valueOf(status);
        if (category != null && !category.isBlank()) {
            return blogRepository.findAllByStatusAndCategoryContainingIgnoreCase(blogStatus, category, pageable).map(this::toDto);
        }
        return blogRepository.findAllByStatus(blogStatus, pageable).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public BlogResponseDTO getAdmin(Long id) {
        Blog blog = blogRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("博客不存在"));
        return toDto(blog);
    }

    @Transactional
    public BlogResponseDTO createAdmin(BlogCreateRequest request) {
        Blog blog = new Blog();
        blog.setTitle(request.getTitle());
        blog.setSlug(request.getSlug());
        blog.setSummary(request.getSummary());
        blog.setCategory(request.getCategory());
        blog.setTags(cleanTags(request.getTags()));
        blog.setContent(request.getContent());
        blog.setStatus(BlogStatus.DRAFT);
        blog.setPublishedAt(null);
        Blog saved = blogRepository.save(blog);
        log.info("blog_op=create, blogId={}", saved.getId());
        blogSearchSyncPublisher.publishUpsert(saved.getId());
        return toDto(saved);
    }

    @Transactional
    public BlogResponseDTO updateAdmin(Long id, BlogUpdateRequest request) {
        Blog blog = blogRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("博客不存在"));
        blog.setTitle(request.getTitle());
        blog.setSlug(request.getSlug());
        blog.setSummary(request.getSummary());
        blog.setCategory(request.getCategory());
        blog.setTags(cleanTags(request.getTags()));
        blog.setContent(request.getContent());
        Blog saved = blogRepository.save(blog);
        log.info("blog_op=update, blogId={}", saved.getId());
        blogSearchSyncPublisher.publishUpsert(saved.getId());
        return toDto(saved);
    }

    @Transactional
    public void deleteAdmin(Long id) {
        Blog blog = blogRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("博客不存在"));
        blogRepository.delete(blog);
        log.info("blog_op=delete, blogId={}", id);
        blogSearchSyncPublisher.publishDelete(id);
    }

    @Transactional
    public BlogResponseDTO publishAdmin(Long id) {
        Blog blog = blogRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("博客不存在"));
        blog.setStatus(BlogStatus.PUBLISHED);
        if (blog.getPublishedAt() == null) {
            blog.setPublishedAt(OffsetDateTime.now());
        }
        Blog saved = blogRepository.save(blog);
        log.info("blog_op=publish, blogId={}", id);
        blogSearchSyncPublisher.publishUpsert(saved.getId());
        return toDto(saved);
    }

    @Transactional
    public BlogResponseDTO unpublishAdmin(Long id) {
        Blog blog = blogRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("博客不存在"));
        blog.setStatus(BlogStatus.DRAFT);
        Blog saved = blogRepository.save(blog);
        log.info("blog_op=unpublish, blogId={}", id);
        blogSearchSyncPublisher.publishDelete(id);
        return toDto(saved);
    }

    private BlogResponseDTO toDto(Blog blog) {
        BlogResponseDTO dto = new BlogResponseDTO();
        dto.setId(blog.getId());
        dto.setTitle(blog.getTitle());
        dto.setSlug(blog.getSlug());
        dto.setSummary(blog.getSummary());
        dto.setCategory(blog.getCategory());
        dto.setTags(new ArrayList<>(blog.getTags()));
        dto.setContent(blog.getContent());
        dto.setStatus(blog.getStatus());
        dto.setPublishedAt(blog.getPublishedAt());
        dto.setCreatedAt(blog.getCreatedAt());
        dto.setUpdatedAt(blog.getUpdatedAt());
        return dto;
    }

    private List<String> cleanTags(List<String> tags) {
        if (tags == null) return List.of();
        List<String> cleaned = new ArrayList<>();
        for (String t : tags) {
            if (t == null) continue;
            String trimmed = t.trim();
            if (trimmed.isBlank()) continue;
            cleaned.add(trimmed);
        }
        return cleaned;
    }
}
