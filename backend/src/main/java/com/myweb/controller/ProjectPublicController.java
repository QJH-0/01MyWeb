package com.myweb.controller;

import com.myweb.common.ApiResponse;
import com.myweb.common.PagedResult;
import com.myweb.dto.ProjectResponseDTO;
import com.myweb.service.ProjectService;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ProjectPublicController {

    private final ProjectService projectService;

    public ProjectPublicController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping("/projects")
    public ResponseEntity<ApiResponse<PagedResult<ProjectResponseDTO>>> list(
        @RequestParam(name = "category", required = false) String category,
        @RequestParam(name = "page", defaultValue = "0") int page,
        @RequestParam(name = "limit", defaultValue = "10") int limit
    ) {
        String traceId = MDC.get("traceId");

        Page<ProjectResponseDTO> result = projectService.listPublic(category, page, limit);
        PagedResult<ProjectResponseDTO> paged = new PagedResult<>(result.getContent(), result.getTotalElements(), page, limit);
        return ResponseEntity.ok(ApiResponse.ok(paged, traceId));
    }

    @GetMapping("/projects/{id}")
    public ResponseEntity<ApiResponse<ProjectResponseDTO>> get(@PathVariable("id") Long id) {
        String traceId = MDC.get("traceId");
        return ResponseEntity.ok(ApiResponse.ok(projectService.getPublic(id), traceId));
    }
}

