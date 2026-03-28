package com.myweb.backend.service;

import com.myweb.backend.common.ApiException;
import com.myweb.backend.common.ErrorCodes;
import com.myweb.backend.common.PagedResult;
import com.myweb.backend.config.FileStorageProperties;
import com.myweb.backend.dto.FileItemDTO;
import com.myweb.backend.entity.ManagedFileEntity;
import com.myweb.backend.repository.ManagedFileRepository;
import com.myweb.backend.security.AuthenticatedUser;
import com.myweb.backend.storage.FileStoragePort;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class FileManagementService {
    private static final Logger log = LoggerFactory.getLogger(FileManagementService.class);
    private static final int MAX_LIMIT = 100;
    private static final Pattern MIME_TYPE_PATTERN =
            Pattern.compile("^[a-z0-9][a-z0-9._+-]+/[a-z0-9][a-z0-9._+-]+$");

    private final ManagedFileRepository managedFileRepository;
    private final FileStoragePort fileStorage;
    private final FileStorageProperties fileStorageProperties;

    public FileManagementService(
            ManagedFileRepository managedFileRepository,
            FileStoragePort fileStorage,
            FileStorageProperties fileStorageProperties
    ) {
        this.managedFileRepository = managedFileRepository;
        this.fileStorage = fileStorage;
        this.fileStorageProperties = fileStorageProperties;
    }

    @Transactional
    public FileItemDTO upload(MultipartFile file, String folder, HttpServletRequest request) {
        validateUpload(file);
        String normalizedType = normalizeContentType(file.getContentType());
        if (!allowedMime(normalizedType)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCodes.VALIDATION_ERROR, "unsupported content type");
        }
        String safeFolder = sanitizeFolder(folder);
        String safeName = safeOriginalFilename(file.getOriginalFilename());
        String storageKey = buildStorageKey(safeFolder, safeName);

        try (InputStream in = file.getInputStream()) {
            fileStorage.putObject(storageKey, in, file.getSize(), normalizedType);
        } catch (IOException e) {
            log.error("storage put failed key={}", storageKey, e);
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCodes.INTERNAL_ERROR, "storage write failed");
        }

        ManagedFileEntity row = new ManagedFileEntity();
        row.setFileName(safeName);
        row.setFileType(normalizedType);
        row.setFileSize(file.getSize());
        row.setStorageKey(storageKey);
        row.setUploadedBy(currentUsername());
        try {
            row = managedFileRepository.save(row);
            row.setAccessUrl(buildAccessUrl(request, row.getId()));
            row = managedFileRepository.save(row);
            return toDto(row);
        } catch (RuntimeException e) {
            try {
                fileStorage.removeObject(storageKey);
            } catch (Exception cleanup) {
                log.warn("storage rollback failed key={}", storageKey, cleanup);
            }
            throw e;
        }
    }

    public PagedResult<FileItemDTO> list(String fileType, int page, int limit) {
        Pageable pageable = toPageable(page, limit);
        String filter = normalizeFileTypeFilter(fileType);
        Page<ManagedFileEntity> data = managedFileRepository.pageActive(filter, pageable);
        List<FileItemDTO> list = data.getContent().stream().map(this::toDto).collect(Collectors.toList());
        return new PagedResult<>(list, data.getTotalElements(), page, limit);
    }

    @Transactional
    public void softDelete(long id) {
        ManagedFileEntity row = managedFileRepository.findActiveById(id).orElseThrow(
                () -> new ApiException(HttpStatus.NOT_FOUND, ErrorCodes.NOT_FOUND, "file not found")
        );
        row.setDeletedAt(Instant.now());
        managedFileRepository.save(row);
        if (fileStorageProperties.physicalDeleteAfterSoftDelete()) {
            try {
                fileStorage.removeObject(row.getStorageKey());
            } catch (Exception e) {
                log.warn("physical delete failed key={}", row.getStorageKey(), e);
            }
        }
    }

    public OpenedDownload openDownload(long id) {
        if (id <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCodes.VALIDATION_ERROR, "invalid id");
        }
        ManagedFileEntity row = managedFileRepository.findActiveById(id).orElseThrow(
                () -> new ApiException(HttpStatus.NOT_FOUND, ErrorCodes.NOT_FOUND, "file not found")
        );
        InputStream in;
        try {
            in = fileStorage.getObject(row.getStorageKey());
        } catch (IOException e) {
            log.error("storage get failed id={} key={}", id, row.getStorageKey(), e);
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCodes.INTERNAL_ERROR, "storage read failed");
        }
        return new OpenedDownload(in, row.getFileSize(), row.getFileName(), row.getFileType());
    }

    public void validateListFileType(String fileType) {
        if (fileType == null || fileType.isBlank()) {
            return;
        }
        if (!MIME_TYPE_PATTERN.matcher(fileType.trim().toLowerCase(Locale.ROOT)).matches()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCodes.VALIDATION_ERROR, "invalid fileType");
        }
    }

    private void validateUpload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCodes.VALIDATION_ERROR, "file required");
        }
        String rawType = file.getContentType();
        if (rawType == null || rawType.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCodes.VALIDATION_ERROR, "content type required");
        }
        if (file.getSize() > fileStorageProperties.maxFileSizeBytes()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCodes.VALIDATION_ERROR, "file too large");
        }
    }

    private static String normalizeFileTypeFilter(String fileType) {
        if (fileType == null || fileType.isBlank()) {
            return null;
        }
        return fileType.trim().toLowerCase(Locale.ROOT);
    }

    private static Pageable toPageable(int page, int limit) {
        if (page < 0 || limit <= 0 || limit > MAX_LIMIT) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCodes.VALIDATION_ERROR, "invalid pagination");
        }
        return PageRequest.of(page, limit, Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    private String buildAccessUrl(HttpServletRequest request, long id) {
        String configured = fileStorageProperties.publicApiBaseUrl();
        if (configured != null && !configured.isBlank()) {
            return configured.replaceAll("/+$", "") + "/api/files/" + id + "/download";
        }
        String scheme = Optional.ofNullable(request.getHeader("X-Forwarded-Proto")).orElse(request.getScheme());
        String hostHeader = request.getHeader("X-Forwarded-Host");
        String host;
        if (hostHeader != null && !hostHeader.isBlank()) {
            host = hostHeader.trim();
        } else {
            int port = request.getServerPort();
            host = request.getServerName() + formatPortIfNonDefault(scheme, port);
        }
        return scheme + "://" + host + "/api/files/" + id + "/download";
    }

    private static String formatPortIfNonDefault(String scheme, int port) {
        if (("http".equalsIgnoreCase(scheme) && port == 80) || ("https".equalsIgnoreCase(scheme) && port == 443)) {
            return "";
        }
        if (port <= 0) {
            return "";
        }
        return ":" + port;
    }

    private static String normalizeContentType(String contentType) {
        String ct = contentType.trim();
        int semi = ct.indexOf(';');
        if (semi > 0) {
            ct = ct.substring(0, semi).trim();
        }
        return ct.toLowerCase(Locale.ROOT);
    }

    private static boolean allowedMime(String normalized) {
        if (normalized.startsWith("image/")) {
            return true;
        }
        return "text/markdown".equals(normalized)
                || "text/x-markdown".equals(normalized)
                || "text/plain".equals(normalized)
                || "application/pdf".equals(normalized);
    }

    private static String sanitizeFolder(String folder) {
        if (folder == null || folder.isBlank()) {
            return "";
        }
        String t = folder.trim();
        if (t.contains("..") || t.startsWith("/") || t.contains("\\")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCodes.VALIDATION_ERROR, "invalid folder");
        }
        return t.replaceAll("^/+", "").replaceAll("/+$", "");
    }

    private static String safeOriginalFilename(String original) {
        if (original == null || original.isBlank()) {
            return "file";
        }
        String name = original.replace('\\', '/');
        int slash = name.lastIndexOf('/');
        if (slash >= 0 && slash < name.length() - 1) {
            name = name.substring(slash + 1);
        }
        String cleaned = name.replaceAll("[^a-zA-Z0-9._-]", "_");
        if (cleaned.isBlank()) {
            cleaned = "file";
        }
        return cleaned.length() > 180 ? cleaned.substring(0, 180) : cleaned;
    }

    private static String buildStorageKey(String folder, String safeName) {
        String prefix = folder.isBlank() ? "" : folder + "/";
        return prefix + UUID.randomUUID() + "-" + safeName;
    }

    private static String currentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof AuthenticatedUser u)) {
            return null;
        }
        return u.getUsername();
    }

    private FileItemDTO toDto(ManagedFileEntity m) {
        return new FileItemDTO(
                m.getId(),
                m.getFileName(),
                m.getFileType(),
                m.getFileSize(),
                m.getStorageKey(),
                m.getAccessUrl(),
                m.getUploadedBy(),
                m.getCreatedAt(),
                m.getDeletedAt()
        );
    }

    public record OpenedDownload(InputStream stream, long contentLength, String fileName, String contentType) {
    }
}
