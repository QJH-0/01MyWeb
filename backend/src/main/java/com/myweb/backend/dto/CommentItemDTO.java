package com.myweb.backend.dto;

import java.time.Instant;

/** 评论列表项：与 OpenAPI/前端契约字段名对齐（camelCase）。 */
public record CommentItemDTO(
        Long id,
        String targetType,
        Long targetId,
        String content,
        Long parentId,
        int likeCount,
        Instant createdAt
) {
}
