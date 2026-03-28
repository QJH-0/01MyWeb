package com.myweb.backend.controller;

import com.myweb.backend.common.ApiResponse;
import com.myweb.backend.common.PagedResult;
import com.myweb.backend.common.TraceIdFilter;
import com.myweb.backend.dto.ProjectResponseDTO;
import com.myweb.backend.service.ProjectService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 访客可见项目列表与详情：仅返回 visible 且未软删的数据（由 Service 保证）。 */
@RestController
@RequestMapping("/api/projects")
public class ProjectPublicController {
    private final ProjectService projectService;

    public ProjectPublicController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping
    public ApiResponse<PagedResult<ProjectResponseDTO>> list(
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int limit,
            HttpServletRequest httpRequest
    ) {
        return ApiResponse.ok(projectService.listPublic(category, page, limit), traceId(httpRequest));
    }

    @GetMapping("/{id}")
    public ApiResponse<ProjectResponseDTO> detail(@PathVariable Long id, HttpServletRequest httpRequest) {
        return ApiResponse.ok(projectService.getPublic(id), traceId(httpRequest));
    }

    /**
     * 从请求属性中获取 traceId。
     *
     * @param request HTTP 请求
     * @return traceId 字符串，若不存在则返回空字符串
     */
    private String traceId(HttpServletRequest request) {
        Object traceAttr = request.getAttribute(TraceIdFilter.TRACE_ID_REQUEST_ATTR);
        return traceAttr == null ? "" : traceAttr.toString();
    }
}

