package com.myweb.backend.common;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class})
    public ResponseEntity<ApiResponse<Void>> handleValidationException(Exception ex, HttpServletRequest request) {
        String traceId = traceId(request);
        log.debug("Validation failed, traceId={}, error={}", traceId, ex.getMessage());
        return ResponseEntity.badRequest().body(ApiResponse.fail(ErrorCodes.VALIDATION_ERROR, traceId));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingParameter(MissingServletRequestParameterException ex, HttpServletRequest request) {
        String traceId = traceId(request);
        log.debug("Missing required parameter, traceId={}, param={}", traceId, ex.getParameterName());
        return ResponseEntity.badRequest().body(ApiResponse.fail(ErrorCodes.VALIDATION_ERROR, traceId));
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResponse<Void>> handleApiException(ApiException ex, HttpServletRequest request) {
        String traceId = traceId(request);
        if (ex.getStatus().is5xxServerError()) {
            log.error("API exception, traceId={}, code={}, status={}", traceId, ex.getErrorCode(), ex.getStatus(), ex);
        } else {
            log.debug("API exception, traceId={}, code={}, status={}", traceId, ex.getErrorCode(), ex.getStatus());
        }
        MDC.put("traceId", traceId);
        try {
            return ResponseEntity.status(ex.getStatus())
                    .body(ApiResponse.fail(ex.getErrorCode(), traceId));
        } finally {
            MDC.remove("traceId");
        }
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthenticationException(AuthenticationException ex, HttpServletRequest request) {
        String traceId = traceId(request);
        log.debug("Authentication failed, traceId={}, error={}", traceId, ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.fail(ErrorCodes.UNAUTHORIZED, traceId));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(AccessDeniedException ex, HttpServletRequest request) {
        String traceId = traceId(request);
        log.debug("Access denied, traceId={}, error={}", traceId, ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.fail(ErrorCodes.FORBIDDEN, traceId));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpectedException(Exception ex, HttpServletRequest request) {
        String traceId = traceId(request);
        log.error("Unexpected exception, traceId={}", traceId, ex);
        MDC.put("traceId", traceId);
        try {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail(ErrorCodes.INTERNAL_ERROR, traceId));
        } finally {
            MDC.remove("traceId");
        }
    }

    private static String traceId(HttpServletRequest request) {
        Object value = request.getAttribute(TraceIdFilter.TRACE_ID_REQUEST_ATTR);
        return value == null ? "" : value.toString();
    }
}
