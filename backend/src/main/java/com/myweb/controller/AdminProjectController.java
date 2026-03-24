package com.myweb.controller;

import com.myweb.common.ApiResponse;
import com.myweb.common.PagedResult;
import com.myweb.dto.ProjectCreateRequest;
import com.myweb.dto.ProjectResponseDTO;
import com.myweb.dto.ProjectUpdateRequest;
import com.myweb.service.ProjectService;
import jakarta.validation.Valid;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.data.domain.Page;

@RestController
@RequestMapping("/api/admin/projects")
public class AdminProjectController {

    private final ProjectService projectService;

    public AdminProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResult<ProjectResponseDTO>>> list(
        @RequestParam(name = "category", required = false) String category,
        @RequestParam(name = "page", defaultValue = "0") int page,
        @RequestParam(name = "limit", defaultValue = "10") int limit
    ) {
        String traceId = MDC.get("traceId");
        Page<ProjectResponseDTO> result = projectService.listAdmin(category, page, limit);
        PagedResult<ProjectResponseDTO> paged = new PagedResult<>(result.getContent(), result.getTotalElements(), page, limit);
        return ResponseEntity.ok(ApiResponse.ok(paged, traceId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProjectResponseDTO>> get(@PathVariable("id") Long id) {
        String traceId = MDC.get("traceId");
        return ResponseEntity.ok(ApiResponse.ok(projectService.getAdmin(id), traceId));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ProjectResponseDTO>> create(
        @RequestBody @Valid ProjectCreateRequest request
    ) {
        String traceId = MDC.get("traceId");
        ProjectResponseDTO created = projectService.createAdmin(request);
        return ResponseEntity.ok(ApiResponse.ok(created, traceId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProjectResponseDTO>> update(
        @PathVariable("id") Long id,
        @RequestBody @Valid ProjectUpdateRequest request
    ) {
        String traceId = MDC.get("traceId");
        return ResponseEntity.ok(ApiResponse.ok(projectService.updateAdmin(id, request), traceId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Object>> delete(@PathVariable("id") Long id) {
        String traceId = MDC.get("traceId");
        projectService.deleteAdmin(id);
        return ResponseEntity.ok(ApiResponse.ok(null, traceId));
    }
}

