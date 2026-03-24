package com.myweb.common;

import java.time.Instant;

public class ApiResponse<T> {
    private final boolean success;
    private final String error;
    private final T data;
    private final Instant timestamp;
    private final String traceId;

    public ApiResponse(boolean success, T data, String error, Instant timestamp, String traceId) {
        this.success = success;
        this.data = data;
        this.error = error;
        this.timestamp = timestamp;
        this.traceId = traceId;
    }

    public static <T> ApiResponse<T> ok(T data, String traceId) {
        return new ApiResponse<>(true, data, null, Instant.now(), traceId);
    }

    public static <T> ApiResponse<T> fail(String error, String traceId) {
        return new ApiResponse<>(false, null, error, Instant.now(), traceId);
    }

    public static <T> ApiResponse<T> fail(String error, T data, String traceId) {
        return new ApiResponse<>(false, data, error, Instant.now(), traceId);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getError() {
        return error;
    }

    public T getData() {
        return data;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public String getTraceId() {
        return traceId;
    }
}

