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

    private boolean shouldApplyRateLimit(String path) {
        return path.startsWith("/api/ai/") || path.startsWith("/api/search");
    }

    private String buildLimitKey(HttpServletRequest request, String path) {
        String identity = extractIdentity(request);
        return identity + ":" + path;
    }

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

    private String getTraceId(HttpServletRequest request) {
        Object traceAttr = request.getAttribute(TraceIdFilter.TRACE_ID_REQUEST_ATTR);
        return traceAttr == null ? "" : traceAttr.toString();
    }

    private String buildRateLimitResponse(String traceId) {
        return String.format(
                "{\"success\":false,\"data\":null,\"error\":\"RATE_LIMITED\",\"timestamp\":\"%s\",\"traceId\":\"%s\"}",
                java.time.Instant.now().toString(),
                traceId
        );
    }

    private record RateLimitWindow(Instant windowEnd, int count) {
    }
}
