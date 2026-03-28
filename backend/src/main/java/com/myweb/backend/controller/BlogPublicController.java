package com.myweb.backend.controller;

import com.myweb.backend.common.ApiResponse;
import com.myweb.backend.common.PagedResult;
import com.myweb.backend.common.TraceIdFilter;
import com.myweb.backend.dto.BlogResponseDTO;
import com.myweb.backend.service.BlogService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 访客博客列表与详情：仅 PUBLISHED 且未软删（由 Service 保证）。 */
@RestController
@RequestMapping("/api/blogs")
public class BlogPublicController {
    private final BlogService blogService;

    public BlogPublicController(BlogService blogService) {
        this.blogService = blogService;
    }

    @GetMapping
    public ApiResponse<PagedResult<BlogResponseDTO>> list(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String tag,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int limit,
            HttpServletRequest httpRequest
    ) {
        return ApiResponse.ok(blogService.listPublic(category, tag, page, limit), traceId(httpRequest));
    }

    @GetMapping("/slug/{slug}")
    public ApiResponse<BlogResponseDTO> bySlug(@PathVariable String slug, HttpServletRequest httpRequest) {
        return ApiResponse.ok(blogService.getPublicBySlug(slug), traceId(httpRequest));
    }

    @GetMapping("/{id}")
    public ApiResponse<BlogResponseDTO> detail(@PathVariable Long id, HttpServletRequest httpRequest) {
        return ApiResponse.ok(blogService.getPublic(id), traceId(httpRequest));
    }

    private String traceId(HttpServletRequest request) {
        Object traceAttr = request.getAttribute(TraceIdFilter.TRACE_ID_REQUEST_ATTR);
        return traceAttr == null ? "" : traceAttr.toString();
    }
}
