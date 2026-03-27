package com.myweb.backend.dto;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;

public record ContentPageDTO(
        String title,
        String summary,
        JsonNode sections,
        Instant updatedAt
) {
}

