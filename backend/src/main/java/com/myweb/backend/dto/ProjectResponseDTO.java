package com.myweb.backend.dto;

import java.time.Instant;
import java.util.List;

/** 对外项目视图：聚合标签列表，不暴露内部主键以外的敏感列。 */
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

