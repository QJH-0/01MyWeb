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

/**
 * 全局异常到 {@link ApiResponse} 的映射：校验/鉴权/业务/未预期分流，日志等级按可预期性区分。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 处理参数校验失败异常（MethodArgumentNotValidException、ConstraintViolationException）。
     *
     * @param ex      校验异常
     * @param request HTTP 请求
     * @return 400 Bad Request 响应
     */
    @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class})
    public ResponseEntity<ApiResponse<Void>> handleValidationException(Exception ex, HttpServletRequest request) {
        String traceId = traceId(request);
        log.debug("Validation failed, traceId={}, error={}", traceId, ex.getMessage());
        return ResponseEntity.badRequest().body(ApiResponse.fail(ErrorCodes.VALIDATION_ERROR, traceId));
    }

    /**
     * 处理请求参数缺失异常。
     *
     * @param ex      参数缺失异常
     * @param request HTTP 请求
     * @return 400 Bad Request 响应
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingParameter(MissingServletRequestParameterException ex, HttpServletRequest request) {
        String traceId = traceId(request);
        log.debug("Missing required parameter, traceId={}, param={}", traceId, ex.getParameterName());
        return ResponseEntity.badRequest().body(ApiResponse.fail(ErrorCodes.VALIDATION_ERROR, traceId));
    }

    /**
     * 处理自定义业务异常（ApiException）。
     * 5xx 错误记录 ERROR 级别日志，其他记录 DEBUG 级别。
     *
     * @param ex      业务异常
     * @param request HTTP 请求
     * @return 对应 HTTP 状态码的响应
     */
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

    /**
     * 处理认证失败异常（AuthenticationException）。
     *
     * @param ex      认证异常
     * @param request HTTP 请求
     * @return 401 Unauthorized 响应
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthenticationException(AuthenticationException ex, HttpServletRequest request) {
        String traceId = traceId(request);
        log.debug("Authentication failed, traceId={}, error={}", traceId, ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.fail(ErrorCodes.UNAUTHORIZED, traceId));
    }

    /**
     * 处理访问被拒绝异常（AccessDeniedException）。
     *
     * @param ex      访问拒绝异常
     * @param request HTTP 请求
     * @return 403 Forbidden 响应
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(AccessDeniedException ex, HttpServletRequest request) {
        String traceId = traceId(request);
        log.debug("Access denied, traceId={}, error={}", traceId, ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.fail(ErrorCodes.FORBIDDEN, traceId));
    }

    /**
     * 处理所有未预期的异常（兜底异常处理器）。
     * 记录 ERROR 级别日志并返回 500 Internal Server Error。
     *
     * @param ex      异常
     * @param request HTTP 请求
     * @return 500 Internal Server Error 响应
     */
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

    /**
     * 从请求属性中获取 traceId。
     *
     * @param request HTTP 请求
     * @return traceId 字符串，若不存在则返回空字符串
     */
    private String traceId(HttpServletRequest request) {
        Object value = request.getAttribute(TraceIdFilter.TRACE_ID_REQUEST_ATTR);
        return value == null ? "" : value.toString();
    }
}
