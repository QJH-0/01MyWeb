package com.myweb.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/** 管理端更新博客：PUT 全量提交可编辑字段。 */
public record BlogUpdateRequest(
        @NotBlank
        @Size(max = 160)
        String title,

        @NotBlank
        @Size(max = 200)
        @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "slug must be alphanumeric with _ or -")
        String slug,

        @NotBlank
        @Size(max = 500)
        String summary,

        @Size(max = 80)
        String category,

        @NotNull
        @Size(min = 1, max = 10)
        List<@NotBlank @Size(max = 60) String> tags,

        @NotBlank
        @Pattern(regexp = "(?s)^.*\\S.*$", message = "content must contain non-whitespace")
        String content,

        @Size(max = 1000)
        String coverUrl
) {
}
