package com.myweb.service;

import com.myweb.common.NotFoundException;
import com.myweb.dto.FileItemDTO;
import com.myweb.entity.ManagedFile;
import com.myweb.repository.ManagedFileRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {
    private static final long MAX_SIZE_BYTES = 10L * 1024 * 1024;

    private final ManagedFileRepository managedFileRepository;
    private final Path storageRoot;

    public FileStorageService(
        ManagedFileRepository managedFileRepository,
        @Value("${app.storage.local-dir:./data/uploads}") String localStorageDir
    ) {
        this.managedFileRepository = managedFileRepository;
        this.storageRoot = Paths.get(localStorageDir).toAbsolutePath().normalize();
    }

    @Transactional
    public FileItemDTO upload(MultipartFile file) {
        validate(file);
        try {
            Files.createDirectories(storageRoot);
            String original = StringUtils.cleanPath(file.getOriginalFilename() == null ? "unknown" : file.getOriginalFilename());
            String storageKey = UUID.randomUUID() + "-" + original;
            Path target = storageRoot.resolve(storageKey);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

            ManagedFile managedFile = new ManagedFile();
            managedFile.setFileName(original);
            managedFile.setContentType(file.getContentType() == null ? "application/octet-stream" : file.getContentType());
            managedFile.setSizeBytes(file.getSize());
            managedFile.setStorageKey(storageKey);
            return toDto(managedFileRepository.save(managedFile));
        } catch (IOException ex) {
            throw new IllegalStateException("文件上传失败");
        }
    }

    @Transactional(readOnly = true)
    public Page<FileItemDTO> list(int page, int limit) {
        return managedFileRepository.findAll(PageRequest.of(page, limit, Sort.by(Sort.Direction.DESC, "id"))).map(this::toDto);
    }

    @Transactional
    public void delete(Long id) {
        ManagedFile file = managedFileRepository.findById(id).orElseThrow(() -> new NotFoundException("文件不存在"));
        try {
            Files.deleteIfExists(storageRoot.resolve(file.getStorageKey()));
        } catch (IOException ignored) {
            // 文件可能已被手工清理；DB 记录仍应删除
        }
        managedFileRepository.delete(file);
    }

    @Transactional(readOnly = true)
    public ManagedFile findEntity(Long id) {
        return managedFileRepository.findById(id).orElseThrow(() -> new NotFoundException("文件不存在"));
    }

    @Transactional(readOnly = true)
    public Path resolveFilePath(ManagedFile managedFile) {
        return storageRoot.resolve(managedFile.getStorageKey());
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上传文件不能为空");
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new IllegalArgumentException("文件大小不能超过 10MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || (!contentType.startsWith("image/") && !contentType.startsWith("text/") && !"application/json".equals(contentType))) {
            throw new IllegalArgumentException("仅支持图片与文本类型文件");
        }
    }

    private FileItemDTO toDto(ManagedFile managedFile) {
        FileItemDTO dto = new FileItemDTO();
        dto.setId(managedFile.getId());
        dto.setFileName(managedFile.getFileName());
        dto.setContentType(managedFile.getContentType());
        dto.setSizeBytes(managedFile.getSizeBytes());
        dto.setUrl("/api/files/" + managedFile.getId() + "/download");
        dto.setCreatedAt(managedFile.getCreatedAt());
        return dto;
    }
}
