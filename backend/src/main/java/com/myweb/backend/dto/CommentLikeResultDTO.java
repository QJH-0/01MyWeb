package com.myweb.backend.dto;

/** 点赞切换结果：与契约 `{ liked, likeCount }` 对齐。 */
public record CommentLikeResultDTO(boolean liked, int likeCount) {
}
