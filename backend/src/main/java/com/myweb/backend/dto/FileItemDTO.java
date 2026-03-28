package com.myweb.backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record FileItemDTO(
        long id,
        String fileName,
        String fileType,
        long fileSize,
        String storageKey,
        String accessUrl,
        String uploadedBy,
        Instant createdAt,
        Instant deletedAt
) {
}
