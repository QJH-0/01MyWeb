package com.myweb.backend.common;

import java.util.Locale;

/** 评论挂载目标：入库统一小写，避免 blog/Blog 混用导致查询不到。 */
public final class CommentTargetType {
    public static final String BLOG = "blog";
    public static final String PROJECT = "project";

    private CommentTargetType() {
    }

    public static String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        return raw.trim().toLowerCase(Locale.ROOT);
    }

    public static boolean isKnown(String normalized) {
        return BLOG.equals(normalized) || PROJECT.equals(normalized);
    }
}
