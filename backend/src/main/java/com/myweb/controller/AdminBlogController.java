package com.myweb.controller;

import com.myweb.common.ApiResponse;
import com.myweb.common.PagedResult;
import com.myweb.dto.BlogCreateRequest;
import com.myweb.dto.BlogResponseDTO;
import com.myweb.dto.BlogUpdateRequest;
import com.myweb.service.BlogService;
import jakarta.validation.Valid;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
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

@RestController
@RequestMapping("/api/admin/blogs")
public class AdminBlogController {

    private final BlogService blogService;

    public AdminBlogController(BlogService blogService) {
        this.blogService = blogService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResult<BlogResponseDTO>>> list(
        @RequestParam(name = "status", required = false) String status,
        @RequestParam(name = "category", required = false) String category,
        @RequestParam(name = "page", defaultValue = "0") int page,
        @RequestParam(name = "limit", defaultValue = "10") int limit
    ) {
        String traceId = MDC.get("traceId");
        Page<BlogResponseDTO> result;
        if (status != null && !status.isBlank()) {
            result = blogService.listAdminByStatus(status, category, page, limit);
        } else {
            result = blogService.listAdmin(category, page, limit);
        }
        PagedResult<BlogResponseDTO> paged = new PagedResult<>(result.getContent(), result.getTotalElements(), page, limit);
        return ResponseEntity.ok(ApiResponse.ok(paged, traceId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BlogResponseDTO>> get(@PathVariable("id") Long id) {
        String traceId = MDC.get("traceId");
        return ResponseEntity.ok(ApiResponse.ok(blogService.getAdmin(id), traceId));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<BlogResponseDTO>> create(@RequestBody @Valid BlogCreateRequest request) {
        String traceId = MDC.get("traceId");
        return ResponseEntity.ok(ApiResponse.ok(blogService.createAdmin(request), traceId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<BlogResponseDTO>> update(
        @PathVariable("id") Long id,
        @RequestBody @Valid BlogUpdateRequest request
    ) {
        String traceId = MDC.get("traceId");
        return ResponseEntity.ok(ApiResponse.ok(blogService.updateAdmin(id, request), traceId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Object>> delete(@PathVariable("id") Long id) {
        String traceId = MDC.get("traceId");
        blogService.deleteAdmin(id);
        return ResponseEntity.ok(ApiResponse.ok(null, traceId));
    }

    @PostMapping("/{id}/publish")
    public ResponseEntity<ApiResponse<BlogResponseDTO>> publish(@PathVariable("id") Long id) {
        String traceId = MDC.get("traceId");
        return ResponseEntity.ok(ApiResponse.ok(blogService.publishAdmin(id), traceId));
    }

    @PostMapping("/{id}/unpublish")
    public ResponseEntity<ApiResponse<BlogResponseDTO>> unpublish(@PathVariable("id") Long id) {
        String traceId = MDC.get("traceId");
        return ResponseEntity.ok(ApiResponse.ok(blogService.unpublishAdmin(id), traceId));
    }
}
