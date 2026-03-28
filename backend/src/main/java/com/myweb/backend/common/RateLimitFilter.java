package com.myweb.backend.common;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 限流过滤器，针对 /api/ai/** 和 /api/search 进行限流保护。
 * 限流键优先级：userId + path > IP + path
 * 滑动窗口：每分钟重置
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {
    private static final int DEFAULT_RATE_LIMIT_PER_MINUTE = 120;

    private final ConcurrentMap<String, RateLimitWindow> windows = new ConcurrentHashMap<>();
    private final int rateLimitPerMinute;

    public RateLimitFilter(
            @Value("${app.security.rate-limit-per-minute:${APP_SECURITY_RATE_LIMIT_PER_MINUTE:120}}") int rateLimitPerMinute
    ) {
        this.rateLimitPerMinute = rateLimitPerMinute > 0 ? rateLimitPerMinute : DEFAULT_RATE_LIMIT_PER_MINUTE;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String path = request.getRequestURI();

        if (!shouldApplyRateLimit(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        String limitKey = buildLimitKey(request, path);
        if (isRateLimited(limitKey)) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            String traceId = getTraceId(request);
            response.getWriter().write(buildRateLimitResponse(traceId));
            return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 判断是否需要对当前路径进行限流。
     * 仅对 /api/ai/** 和 /api/search 路径启用限流。
     *
     * @param path 请求路径
     * @return 是否需要限流
     */
    private boolean shouldApplyRateLimit(String path) {
        return path.startsWith("/api/ai/") || path.startsWith("/api/search");
    }

    /**
     * 构建限流键：优先使用用户ID，否则使用IP地址。
     *
     * @param request HTTP 请求
     * @param path    请求路径
     * @return 限流键字符串
     */
    private String buildLimitKey(HttpServletRequest request, String path) {
        String identity = extractIdentity(request);
        return identity + ":" + path;
    }

    /**
     * 提取请求身份标识：已登录用户使用 userId，匿名用户使用 IP 地址。
     *
     * @param request HTTP 请求
     * @return 身份标识字符串（格式：user:{userId} 或 ip:{ip}）
     */
    private String extractIdentity(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal())) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof com.myweb.backend.security.AuthenticatedUser user) {
                return "user:" + user.userId();
            }
            return "user:" + principal.toString();
        }

        String clientIp = extractClientIp(request);
        return "ip:" + clientIp;
    }

    /**
     * 提取客户端真实 IP 地址。
     * 优先从 X-Forwarded-For 和 X-Real-IP 头获取（支持反向代理），
     * 否则使用 remoteAddr。
     *
     * @param request HTTP 请求
     * @return 客户端 IP 地址
     */
    private String extractClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp;
        }
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "unknown";
    }

    /**
     * 检查当前限流键是否已超出限流阈值。
     * 使用滑动窗口算法，每分钟重置计数。
     *
     * @param limitKey 限流键
     * @return true 表示已限流，false 表示未限流
     */
    private boolean isRateLimited(String limitKey) {
        Instant now = Instant.now();
        RateLimitWindow window = windows.compute(limitKey, (key, current) -> {
            if (current == null || current.windowEnd().isBefore(now)) {
                return new RateLimitWindow(now.plusSeconds(60), 1);
            }
            return new RateLimitWindow(current.windowEnd(), current.count() + 1);
        });
        return window.count() > rateLimitPerMinute;
    }

    /**
     * 从请求属性中获取 traceId。
     *
     * @param request HTTP 请求
     * @return traceId 字符串
     */
    private String getTraceId(HttpServletRequest request) {
        Object traceAttr = request.getAttribute(TraceIdFilter.TRACE_ID_REQUEST_ATTR);
        return traceAttr == null ? "" : traceAttr.toString();
    }

    /**
     * 构建限流响应 JSON 字符串。
     *
     * @param traceId traceId
     * @return JSON 格式的错误响应
     */
    private String buildRateLimitResponse(String traceId) {
        return String.format(
                "{\"success\":false,\"data\":null,\"error\":\"RATE_LIMITED\",\"timestamp\":\"%s\",\"traceId\":\"%s\"}",
                java.time.Instant.now().toString(),
                traceId
        );
    }

    /**
     * 限流窗口记录：包含窗口结束时间和当前请求计数。
     */
    private record RateLimitWindow(Instant windowEnd, int count) {
    }
}
