package com.myweb.backend.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myweb.backend.common.ApiResponse;
import com.myweb.backend.common.TraceIdFilter;
import com.myweb.backend.config.SecurityProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 管理路径双因子：在已 JWT 登录前提下校验 {@code X-Admin-Token} 与环境配置一致，防止仅靠用户角色误操作生产数据。
 */
@Component
public class AdminTokenFilter extends OncePerRequestFilter {
    private static final String ADMIN_TOKEN_HEADER = "X-Admin-Token";
    private final SecurityProperties securityProperties;
    private final ObjectMapper objectMapper;

    public AdminTokenFilter(SecurityProperties securityProperties, ObjectMapper objectMapper) {
        this.securityProperties = securityProperties;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String path = request.getRequestURI();
        if (!path.startsWith("/api/admin/")) {
            filterChain.doFilter(request, response);
            return;
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            String expected = securityProperties.admin().token();
            String incoming = request.getHeader(ADMIN_TOKEN_HEADER);
            if (incoming == null || incoming.isBlank() || !incoming.equals(expected)) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                Object traceId = request.getAttribute(TraceIdFilter.TRACE_ID_REQUEST_ATTR);
                objectMapper.writeValue(
                        response.getWriter(),
                        ApiResponse.fail("FORBIDDEN", traceId == null ? "" : traceId.toString())
                );
                return;
            }
        }
        filterChain.doFilter(request, response);
    }
}
