package com.myweb.common;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidation(
        MethodArgumentNotValidException ex,
        HttpServletRequest request
    ) {
        String traceId = request.getHeader(TraceIdFilter.TRACE_ID_HEADER);
        if (traceId == null || traceId.isBlank()) {
            traceId = MDC.get("traceId");
        }
        String message = ex.getBindingResult().getFieldErrors().stream()
            .map(err -> err.getField() + " " + err.getDefaultMessage())
            .collect(Collectors.joining("; "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.fail(message, traceId));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Object>> handleIllegalArgument(
        IllegalArgumentException ex,
        HttpServletRequest request
    ) {
        String traceId = request.getHeader(TraceIdFilter.TRACE_ID_HEADER);
        if (traceId == null || traceId.isBlank()) {
            traceId = MDC.get("traceId");
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.fail(ex.getMessage(), traceId));
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleNotFound(
        NotFoundException ex,
        HttpServletRequest request
    ) {
        String traceId = request.getHeader(TraceIdFilter.TRACE_ID_HEADER);
        if (traceId == null || traceId.isBlank()) {
            traceId = MDC.get("traceId");
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.fail(ex.getMessage(), traceId));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleUnknown(
        Exception ex,
        HttpServletRequest request
    ) {
        String traceId = request.getHeader(TraceIdFilter.TRACE_ID_HEADER);
        if (traceId == null || traceId.isBlank()) {
            traceId = MDC.get("traceId");
        }
        log.error("Request failed, traceId={}", traceId, ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.fail("服务内部错误，请稍后重试", traceId));
    }
}

