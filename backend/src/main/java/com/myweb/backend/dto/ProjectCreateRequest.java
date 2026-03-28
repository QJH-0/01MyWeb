package com.myweb.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/** 管理端创建项目请求体：约束与 {@link com.myweb.backend.entity.ProjectEntity} 列长一致。 */
public record ProjectCreateRequest(
        @NotBlank
        @Size(max = 200)
        String title,

        @NotBlank
        @Size(max = 500)
        String summary,

        String description,

        @Size(max = 80)
        String category,

        @NotNull
        @Size(min = 1, max = 10)
        List<@NotBlank @Size(max = 60) String> tags,

        @Size(max = 1000)
        String coverUrl,

        @Size(max = 1000)
        String projectUrl,

        @Size(max = 1000)
        String sourceUrl,

        Integer sortOrder,

        @NotNull
        Boolean visible
) {
}

