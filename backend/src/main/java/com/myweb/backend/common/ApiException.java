package com.myweb.backend.common;

import org.springframework.http.HttpStatus;

/**
 * 受控业务异常：携带 HTTP 状态与稳定机器可读 {@code errorCode}，由 {@link GlobalExceptionHandler} 映射为 {@link ApiResponse}。
 */
public class ApiException extends RuntimeException {
    private final HttpStatus status;
    private final String errorCode;

    public ApiException(HttpStatus status, String errorCode, String message) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
