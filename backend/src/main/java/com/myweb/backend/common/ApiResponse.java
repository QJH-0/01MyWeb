package com.myweb.backend.common;

import java.time.Instant;

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
