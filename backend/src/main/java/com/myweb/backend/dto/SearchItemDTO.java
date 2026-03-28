package com.myweb.backend.dto;

import java.util.List;

/** 公开搜索命中项：与契约 SearchItemDTO 字段对齐。 */
public record SearchItemDTO(
        String sourceType,
        long sourceId,
        String title,
        String url,
        String summary,
        List<String> highlights
) {
}
