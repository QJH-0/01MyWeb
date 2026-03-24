package com.myweb.controller;

import com.myweb.entity.ManagedFile;
import com.myweb.service.FileStorageService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/files")
public class FilePublicController {
    private final FileStorageService fileStorageService;

    public FilePublicController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable("id") Long id) {
        ManagedFile file = fileStorageService.findEntity(id);
        FileSystemResource resource = new FileSystemResource(fileStorageService.resolveFilePath(file));
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(file.getContentType()))
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + file.getFileName() + "\"")
            .body(resource);
    }
}
