package com.myweb.backend.controller;

import com.myweb.backend.common.ApiResponse;
import com.myweb.backend.common.PagedResult;
import com.myweb.backend.common.TraceIdFilter;
import com.myweb.backend.dto.FileItemDTO;
import com.myweb.backend.service.FileManagementService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** 后台文件上传、分页列表与软删：写操作需 JWT 管理员 + X-Admin-Token。 */
@RestController
@RequestMapping("/api/admin/files")
public class AdminFileController {
    private final FileManagementService fileManagementService;

    public AdminFileController(FileManagementService fileManagementService) {
        this.fileManagementService = fileManagementService;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<FileItemDTO> upload(
            @RequestPart("file") MultipartFile file,
            @RequestParam(required = false) String folder,
            HttpServletRequest request
    ) {
        return ApiResponse.ok(fileManagementService.upload(file, folder, request), traceId(request));
    }

    @GetMapping
    public ApiResponse<PagedResult<FileItemDTO>> list(
            @RequestParam(required = false) String fileType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int limit,
            HttpServletRequest request
    ) {
        fileManagementService.validateListFileType(fileType);
        return ApiResponse.ok(fileManagementService.list(fileType, page, limit), traceId(request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable long id, HttpServletRequest request) {
        fileManagementService.softDelete(id);
        return ApiResponse.ok(null, traceId(request));
    }

    private String traceId(HttpServletRequest request) {
        Object value = request.getAttribute(TraceIdFilter.TRACE_ID_REQUEST_ATTR);
        return value == null ? "" : value.toString();
    }
}
