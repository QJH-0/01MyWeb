package com.myweb.backend.controller;

import com.myweb.backend.common.ApiResponse;
import com.myweb.backend.common.PagedResult;
import com.myweb.backend.common.TraceIdFilter;
import com.myweb.backend.dto.ProjectCreateRequest;
import com.myweb.backend.dto.ProjectResponseDTO;
import com.myweb.backend.dto.ProjectUpdateRequest;
import com.myweb.backend.service.ProjectService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 管理端项目 CRUD：需 JWT + 管理员权限 +（开发联调可选）管理 token，具体见 Security 配置。 */
@RestController
@RequestMapping("/api/admin/projects")
public class AdminProjectController {
    private final ProjectService projectService;

    public AdminProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping
    public ApiResponse<PagedResult<ProjectResponseDTO>> list(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Boolean visible,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int limit,
            HttpServletRequest httpRequest
    ) {
        return ApiResponse.ok(projectService.listAdmin(category, visible, page, limit), traceId(httpRequest));
    }

    @GetMapping("/{id}")
    public ApiResponse<ProjectResponseDTO> detail(@PathVariable Long id, HttpServletRequest httpRequest) {
        return ApiResponse.ok(projectService.getAdmin(id), traceId(httpRequest));
    }

    @PostMapping
    public ApiResponse<ProjectResponseDTO> create(@Valid @RequestBody ProjectCreateRequest request, HttpServletRequest httpRequest) {
        return ApiResponse.ok(projectService.create(request), traceId(httpRequest));
    }

    @PutMapping("/{id}")
    public ApiResponse<ProjectResponseDTO> update(
            @PathVariable Long id,
            @Valid @RequestBody ProjectUpdateRequest request,
            HttpServletRequest httpRequest
    ) {
        return ApiResponse.ok(projectService.update(id, request), traceId(httpRequest));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id, HttpServletRequest httpRequest) {
        projectService.delete(id);
        return ApiResponse.ok(null, traceId(httpRequest));
    }

    /**
     * 从请求属性中获取 traceId。
     *
     * @param request HTTP 请求
     * @return traceId 字符串，若不存在则返回空字符串
     */
    private String traceId(HttpServletRequest request) {
        Object value = request.getAttribute(TraceIdFilter.TRACE_ID_REQUEST_ATTR);
        return value == null ? "" : value.toString();
    }
}
