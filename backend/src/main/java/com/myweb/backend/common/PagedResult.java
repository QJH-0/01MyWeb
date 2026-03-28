package com.myweb.backend.common;

import java.util.List;

/** 分页列表约定：与前端 {@code PagedResult} 字段对齐（list/total/page/limit）。 */
public record PagedResult<T>(
        List<T> list,
        long total,
        int page,
        int limit
) {
}

