package com.myweb.backend.dto;

import java.time.Instant;
import java.util.List;

/** 对外博客视图：聚合标签；{@code content} 列表场景可由调用方按需省略（当前接口统一返回）。 */
public record BlogResponseDTO(
        Long id,
        String title,
        String slug,
        String summary,
        String content,
        String category,
        List<String> tags,
        String status,
        String coverUrl,
        Integer viewCount,
        Instant publishedAt,
        Instant createdAt,
        Instant updatedAt,
        Instant deletedAt
) {
}
