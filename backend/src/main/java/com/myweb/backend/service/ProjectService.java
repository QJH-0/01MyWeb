package com.myweb.backend.service;

import com.myweb.backend.common.ApiException;
import com.myweb.backend.common.ErrorCodes;
import com.myweb.backend.common.PagedResult;
import com.myweb.backend.dto.ProjectCreateRequest;
import com.myweb.backend.dto.ProjectResponseDTO;
import com.myweb.backend.dto.ProjectUpdateRequest;
import com.myweb.backend.entity.ProjectEntity;
import com.myweb.backend.entity.ProjectTagEntity;
import com.myweb.backend.repository.ProjectRepository;
import com.myweb.backend.repository.ProjectTagRepository;
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
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class ProjectService {
    private static final int MAX_LIMIT = 100;
    private final ProjectRepository projectRepository;
    private final ProjectTagRepository projectTagRepository;

    public ProjectService(ProjectRepository projectRepository, ProjectTagRepository projectTagRepository) {
        this.projectRepository = projectRepository;
        this.projectTagRepository = projectTagRepository;
    }

    public PagedResult<ProjectResponseDTO> listPublic(String category, int page, int limit) {
        Pageable pageable = toPageable(page, limit);
        Page<ProjectEntity> pageData = category == null || category.isBlank()
                ? projectRepository.findAllByDeletedAtIsNullAndVisibleTrue(pageable)
                : projectRepository.findAllByDeletedAtIsNullAndVisibleTrueAndCategory(category.trim(), pageable);

        List<ProjectResponseDTO> items = withTags(pageData.getContent());
        return new PagedResult<>(items, pageData.getTotalElements(), page, limit);
    }

    public ProjectResponseDTO getPublic(Long id) {
        ProjectEntity entity = projectRepository.findByIdAndDeletedAtIsNullAndVisibleTrue(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ErrorCodes.NOT_FOUND, "Project not found"));
        List<String> tags = projectTagRepository.findAllByProjectId(entity.getId()).stream()
                .map(ProjectTagEntity::getTag)
                .filter(Objects::nonNull)
                .sorted()
                .toList();
        return toDTO(entity, tags);
    }

    public PagedResult<ProjectResponseDTO> listAdmin(String category, Boolean visible, int page, int limit) {
        Pageable pageable = toPageable(page, limit);
        String normalizedCategory = category == null || category.isBlank() ? null : category.trim();

        Page<ProjectEntity> pageData;
        if (visible == null && normalizedCategory == null) {
            pageData = projectRepository.findAllByDeletedAtIsNull(pageable);
        } else if (visible == null) {
            pageData = projectRepository.findAllByDeletedAtIsNullAndCategory(normalizedCategory, pageable);
        } else if (normalizedCategory == null) {
            pageData = projectRepository.findAllByDeletedAtIsNullAndVisible(visible, pageable);
        } else {
            pageData = projectRepository.findAllByDeletedAtIsNullAndCategoryAndVisible(normalizedCategory, visible, pageable);
        }

        List<ProjectResponseDTO> items = withTags(pageData.getContent());
        return new PagedResult<>(items, pageData.getTotalElements(), page, limit);
    }

    public ProjectResponseDTO getAdmin(Long id) {
        ProjectEntity entity = projectRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ErrorCodes.NOT_FOUND, "Project not found"));
        List<String> tags = projectTagRepository.findAllByProjectId(entity.getId()).stream()
                .map(ProjectTagEntity::getTag)
                .filter(Objects::nonNull)
                .sorted()
                .toList();
        return toDTO(entity, tags);
    }

    @Transactional
    public ProjectResponseDTO create(ProjectCreateRequest request) {
        ProjectEntity entity = new ProjectEntity();
        entity.setTitle(request.title().trim());
        entity.setSummary(request.summary().trim());
        entity.setDescription(blankToNull(request.description()));
        entity.setCategory(blankToNull(request.category()));
        entity.setCoverUrl(validateOptionalUrl(blankToNull(request.coverUrl()), "coverUrl"));
        entity.setProjectUrl(validateOptionalUrl(blankToNull(request.projectUrl()), "projectUrl"));
        entity.setSourceUrl(validateOptionalUrl(blankToNull(request.sourceUrl()), "sourceUrl"));
        entity.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());
        entity.setVisible(Boolean.TRUE.equals(request.visible()));

        ProjectEntity saved = projectRepository.save(entity);
        replaceTags(saved.getId(), request.tags());
        return getAdmin(saved.getId());
    }

    @Transactional
    public ProjectResponseDTO update(Long id, ProjectUpdateRequest request) {
        ProjectEntity entity = projectRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ErrorCodes.NOT_FOUND, "Project not found"));

        entity.setTitle(request.title().trim());
        entity.setSummary(request.summary().trim());
        entity.setDescription(blankToNull(request.description()));
        entity.setCategory(blankToNull(request.category()));
        entity.setCoverUrl(validateOptionalUrl(blankToNull(request.coverUrl()), "coverUrl"));
        entity.setProjectUrl(validateOptionalUrl(blankToNull(request.projectUrl()), "projectUrl"));
        entity.setSourceUrl(validateOptionalUrl(blankToNull(request.sourceUrl()), "sourceUrl"));
        entity.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());
        entity.setVisible(Boolean.TRUE.equals(request.visible()));

        projectRepository.save(entity);
        replaceTags(entity.getId(), request.tags());
        return getAdmin(entity.getId());
    }

    @Transactional
    public void delete(Long id) {
        ProjectEntity entity = projectRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ErrorCodes.NOT_FOUND, "Project not found"));
        entity.setDeletedAt(Instant.now());
        projectRepository.save(entity);
    }

    private Pageable toPageable(int page, int limit) {
        if (page < 0 || limit <= 0 || limit > MAX_LIMIT) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCodes.VALIDATION_ERROR, "Invalid pagination params");
        }
        Sort sort = Sort.by(
                Sort.Order.desc("sortOrder"),
                Sort.Order.desc("updatedAt"),
                Sort.Order.desc("id")
        );
        return PageRequest.of(page, limit, sort);
    }

    private List<ProjectResponseDTO> withTags(Collection<ProjectEntity> projects) {
        if (projects.isEmpty()) {
            return List.of();
        }
        List<Long> ids = projects.stream().map(ProjectEntity::getId).filter(Objects::nonNull).toList();
        Map<Long, List<String>> tagsByProjectId = projectTagRepository.findAllByProjectIdIn(ids).stream()
                .filter(tag -> tag.getProjectId() != null && tag.getTag() != null)
                .collect(Collectors.groupingBy(
                        ProjectTagEntity::getProjectId,
                        Collectors.mapping(ProjectTagEntity::getTag, Collectors.toList())
                ));

        return projects.stream()
                .map(project -> {
                    List<String> tags = tagsByProjectId.getOrDefault(project.getId(), List.of()).stream()
                            .filter(Objects::nonNull)
                            .map(String::trim)
                            .filter(t -> !t.isEmpty())
                            .distinct()
                            .sorted()
                            .toList();
                    return toDTO(project, tags);
                })
                .toList();
    }

    private void replaceTags(Long projectId, List<String> tags) {
        projectTagRepository.deleteAllByProjectId(projectId);
        List<ProjectTagEntity> entities = tags.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(tag -> !tag.isEmpty())
                .distinct()
                .map(tag -> new ProjectTagEntity(projectId, tag))
                .toList();
        projectTagRepository.saveAll(entities);
    }

    private static ProjectResponseDTO toDTO(ProjectEntity entity, List<String> tags) {
        return new ProjectResponseDTO(
                entity.getId(),
                entity.getTitle(),
                entity.getSummary(),
                entity.getDescription(),
                entity.getCategory(),
                tags,
                entity.getCoverUrl(),
                entity.getProjectUrl(),
                entity.getSourceUrl(),
                entity.isVisible(),
                entity.getSortOrder(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getDeletedAt()
        );
    }

    private static String blankToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String validateOptionalUrl(String url, String fieldName) {
        if (url == null) return null;
        if (url.startsWith("http://") || url.startsWith("https://")) {
            return url;
        }
        throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCodes.VALIDATION_ERROR, "Invalid url field: " + fieldName);
    }
}

