package com.myweb.controller;

import com.myweb.common.ApiResponse;
import com.myweb.common.PagedResult;
import com.myweb.dto.FileItemDTO;
import com.myweb.service.FileStorageService;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/files")
public class AdminFileController {
    private final FileStorageService fileStorageService;

    public AdminFileController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<FileItemDTO>> upload(@RequestParam("file") MultipartFile file) {
        String traceId = MDC.get("traceId");
        return ResponseEntity.ok(ApiResponse.ok(fileStorageService.upload(file), traceId));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResult<FileItemDTO>>> list(
        @RequestParam(name = "page", defaultValue = "0") int page,
        @RequestParam(name = "limit", defaultValue = "10") int limit
    ) {
        String traceId = MDC.get("traceId");
        Page<FileItemDTO> files = fileStorageService.list(page, limit);
        return ResponseEntity.ok(ApiResponse.ok(new PagedResult<>(files.getContent(), files.getTotalElements(), page, limit), traceId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Object>> delete(@PathVariable("id") Long id) {
        String traceId = MDC.get("traceId");
        fileStorageService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok(null, traceId));
    }
}
