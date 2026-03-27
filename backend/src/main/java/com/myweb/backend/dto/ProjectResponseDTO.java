package com.myweb.backend.dto;

import java.time.Instant;
import java.util.List;

public record ProjectResponseDTO(
        Long id,
        String title,
        String summary,
        String description,
        String category,
        List<String> tags,
        String coverUrl,
        String projectUrl,
        String sourceUrl,
        boolean visible,
        Integer sortOrder,
        Instant createdAt,
        Instant updatedAt,
        Instant deletedAt
) {
}

