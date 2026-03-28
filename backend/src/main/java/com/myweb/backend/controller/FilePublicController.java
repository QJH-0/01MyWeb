package com.myweb.backend.controller;

import com.myweb.backend.service.FileManagementService;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

/** 公开文件下载：通过后端转发 MinIO 流，不暴露内部 storage_key。 */
@RestController
@RequestMapping("/api/files")
public class FilePublicController {
    private final FileManagementService fileManagementService;

    public FilePublicController(FileManagementService fileManagementService) {
        this.fileManagementService = fileManagementService;
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable long id) {
        FileManagementService.OpenedDownload payload = fileManagementService.openDownload(id);
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(payload.fileName(), StandardCharsets.UTF_8)
                .build();
        InputStreamResource body = new InputStreamResource(payload.stream());
        ResponseEntity.BodyBuilder builder = ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.parseMediaType(payload.contentType()));
        if (payload.contentLength() > 0) {
            builder.contentLength(payload.contentLength());
        }
        return builder.body(body);
    }
}
