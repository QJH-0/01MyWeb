package com.myweb.service;

import com.myweb.common.NotFoundException;
import com.myweb.dto.ProjectCreateRequest;
import com.myweb.dto.ProjectResponseDTO;
import com.myweb.dto.ProjectUpdateRequest;
import com.myweb.entity.Project;
import com.myweb.repository.ProjectRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;

    public ProjectService(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    @Transactional(readOnly = true)
    public Page<ProjectResponseDTO> listPublic(String category, int page, int limit) {
        Pageable pageable = PageRequest.of(page, limit, Sort.by(Sort.Direction.DESC, "id"));
        if (category != null && !category.isBlank()) {
            return projectRepository
                .findAllByVisibleTrueAndCategoryContainingIgnoreCase(category, pageable)
                .map(this::toDto);
        }
        return projectRepository.findAllByVisibleTrue(pageable).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public ProjectResponseDTO getPublic(Long id) {
        Project project = projectRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("项目不存在"));
        if (!project.isVisible()) {
            throw new NotFoundException("项目不可见");
        }
        return toDto(project);
    }

    @Transactional(readOnly = true)
    public Page<ProjectResponseDTO> listAdmin(String category, int page, int limit) {
        Pageable pageable = PageRequest.of(page, limit, Sort.by(Sort.Direction.DESC, "id"));
        if (category != null && !category.isBlank()) {
            return projectRepository
                .findAllByCategoryContainingIgnoreCase(category, pageable)
                .map(this::toDto);
        }
        return projectRepository.findAll(pageable).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public ProjectResponseDTO getAdmin(Long id) {
        Project project = projectRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("项目不存在"));
        return toDto(project);
    }

    @Transactional
    public ProjectResponseDTO createAdmin(ProjectCreateRequest request) {
        Project project = new Project();
        project.setTitle(request.getTitle());
        project.setSummary(request.getSummary());
        project.setCategory(request.getCategory());
        project.setTags(cleanTags(request.getTags()));
        project.setCoverUrl(request.getCoverUrl());
        project.setGithubUrl(request.getGithubUrl());
        project.setDemoUrl(request.getDemoUrl());
        project.setVisible(request.getVisible());

        return toDto(projectRepository.save(project));
    }

    @Transactional
    public ProjectResponseDTO updateAdmin(Long id, ProjectUpdateRequest request) {
        Project project = projectRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("项目不存在"));

        project.setTitle(request.getTitle());
        project.setSummary(request.getSummary());
        project.setCategory(request.getCategory());
        project.setTags(cleanTags(request.getTags()));
        project.setCoverUrl(request.getCoverUrl());
        project.setGithubUrl(request.getGithubUrl());
        project.setDemoUrl(request.getDemoUrl());
        project.setVisible(request.getVisible());

        return toDto(projectRepository.save(project));
    }

    @Transactional
    public void deleteAdmin(Long id) {
        Project project = projectRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("项目不存在"));
        project.setVisible(false);
        projectRepository.save(project);
    }

    private ProjectResponseDTO toDto(Project project) {
        ProjectResponseDTO dto = new ProjectResponseDTO();
        dto.setId(project.getId());
        dto.setTitle(project.getTitle());
        dto.setSummary(project.getSummary());
        dto.setCategory(project.getCategory());
        dto.setTags(new ArrayList<>(project.getTags()));
        dto.setCoverUrl(project.getCoverUrl());
        dto.setGithubUrl(project.getGithubUrl());
        dto.setDemoUrl(project.getDemoUrl());
        dto.setVisible(project.isVisible());
        dto.setCreatedAt(project.getCreatedAt());
        dto.setUpdatedAt(project.getUpdatedAt());
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

