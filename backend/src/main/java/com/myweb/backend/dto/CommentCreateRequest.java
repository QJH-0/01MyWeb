package com.myweb.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** 顶层评论创建体：目标在发表前必须对访客可见（已发布博客 / 可见项目）。 */
public record CommentCreateRequest(
        @NotBlank @Size(max = 20) String targetType,
        @NotNull @Positive Long targetId,
        @NotBlank @Size(max = 8000) String content
) {
}
