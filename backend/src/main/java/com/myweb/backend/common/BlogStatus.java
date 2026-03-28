package com.myweb.backend.common;

/** 博客状态常量：与契约文档 DRAFT/PUBLISHED 对齐，避免魔法字符串散落。 */
public final class BlogStatus {
    public static final String DRAFT = "DRAFT";
    public static final String PUBLISHED = "PUBLISHED";

    private BlogStatus() {
    }
}
