package com.myweb.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 回复体：目标从父评论继承，避免客户端篡改 target。 */
public record CommentReplyRequest(
        @NotBlank @Size(max = 8000) String content
) {
}
