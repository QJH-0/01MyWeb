package com.myweb.backend.dto;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;

/** 内容页 API 负载：`sections` 已解析为 JSON 节点，便于前端直接渲染结构化块。 */
public record ContentPageDTO(
        String title,
        String summary,
        JsonNode sections,
        Instant updatedAt
) {
}

