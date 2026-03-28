package com.myweb.backend.common;

import java.time.Instant;

/**
 * 统一 API 信封：前端与集成方只依赖 success/error/data/traceId，避免各控制器自定义形态。
 */
public record ApiResponse<T>(
        boolean success,
        T data,
        String error,
        Instant timestamp,
        String traceId
) {
    public static <T> ApiResponse<T> ok(T data, String traceId) {
        return new ApiResponse<>(true, data, null, Instant.now(), traceId);
    }

    public static <T> ApiResponse<T> fail(String error, String traceId) {
        return new ApiResponse<>(false, null, error, Instant.now(), traceId);
    }
}
