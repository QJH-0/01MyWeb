package com.myweb.backend.common;

import java.util.List;

public record PagedResult<T>(
        List<T> list,
        long total,
        int page,
        int limit
) {
}

