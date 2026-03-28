package com.myweb.backend.controller;

import com.myweb.backend.common.ApiResponse;
import com.myweb.backend.common.PagedResult;
import com.myweb.backend.common.TraceIdFilter;
import com.myweb.backend.dto.BlogCreateRequest;
import com.myweb.backend.dto.BlogResponseDTO;
import com.myweb.backend.dto.BlogUpdateRequest;
import com.myweb.backend.service.BlogService;
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

/** 管理端博客 CRUD + 发布/取消发布：需 JWT 管理员 + X-Admin-Token。 */
@RestController
@RequestMapping("/api/admin/blogs")
public class AdminBlogController {
    private final BlogService blogService;

    public AdminBlogController(BlogService blogService) {
        this.blogService = blogService;
    }

    @GetMapping
    public ApiResponse<PagedResult<BlogResponseDTO>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int limit,
            HttpServletRequest httpRequest
    ) {
        return ApiResponse.ok(blogService.listAdmin(status, category, page, limit), traceId(httpRequest));
    }

    @GetMapping("/{id}")
    public ApiResponse<BlogResponseDTO> detail(@PathVariable Long id, HttpServletRequest httpRequest) {
        return ApiResponse.ok(blogService.getAdmin(id), traceId(httpRequest));
    }

    @PostMapping
    public ApiResponse<BlogResponseDTO> create(@Valid @RequestBody BlogCreateRequest request, HttpServletRequest httpRequest) {
        return ApiResponse.ok(blogService.create(request), traceId(httpRequest));
    }

    @PutMapping("/{id}")
    public ApiResponse<BlogResponseDTO> update(
            @PathVariable Long id,
            @Valid @RequestBody BlogUpdateRequest request,
            HttpServletRequest httpRequest
    ) {
        return ApiResponse.ok(blogService.update(id, request), traceId(httpRequest));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id, HttpServletRequest httpRequest) {
        blogService.delete(id);
        return ApiResponse.ok(null, traceId(httpRequest));
    }

    @PostMapping("/{id}/publish")
    public ApiResponse<BlogResponseDTO> publish(@PathVariable Long id, HttpServletRequest httpRequest) {
        return ApiResponse.ok(blogService.publish(id), traceId(httpRequest));
    }

    @PostMapping("/{id}/unpublish")
    public ApiResponse<BlogResponseDTO> unpublish(@PathVariable Long id, HttpServletRequest httpRequest) {
        return ApiResponse.ok(blogService.unpublish(id), traceId(httpRequest));
    }

    private String traceId(HttpServletRequest request) {
        Object value = request.getAttribute(TraceIdFilter.TRACE_ID_REQUEST_ATTR);
        return value == null ? "" : value.toString();
    }
}
