package com.myweb.backend.security;

import com.myweb.backend.common.TraceIdFilter;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import java.io.IOException;
import java.util.Set;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * JWT 认证过滤器：从请求头解析 Bearer Token，构建认证主体并写入 SecurityContext。
 * 过滤器顺序在 TraceIdFilter 之后（Ordered.LOWEST_PRECEDENCE - 100）。
 */
@Component
@Order(200)
public class JwtAuthenticationFilter extends OncePerRequestFilter implements Ordered {
    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private final JwtService jwtService;

    /**
     * 构造 JWT 认证过滤器。
     *
     * @param jwtService JWT 服务
     */
    public JwtAuthenticationFilter(
            JwtService jwtService
    ) {
        this.jwtService = jwtService;
    }

    @Override
    public int getOrder() {
        // 过滤器顺序，值越小越先执行
        return 200;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);
        try {
            Claims claims = jwtService.parse(token);
            Object uidValue = claims.get("uid");
            Object rolesValue = claims.get("roles");
                Object permissionsValue = claims.get("permissions");
                if (!(uidValue instanceof Number uidNumber)) {
                filterChain.doFilter(request, response);
                return;
            }

            Set<String> roleAuthorities = (rolesValue instanceof List<?> rolesRaw)
                    ? rolesRaw.stream()
                    .map(String::valueOf)
                    .map(String::trim)
                    .filter(v -> !v.isBlank())
                    .collect(Collectors.toUnmodifiableSet())
                    : Set.of();

            Set<String> permissionAuthorities = (permissionsValue instanceof List<?> permissionsRaw)
                    ? permissionsRaw.stream()
                    .map(String::valueOf)
                    .map(String::trim)
                    .filter(v -> !v.isBlank())
                    .collect(Collectors.toUnmodifiableSet())
                    : Set.of();

            Set<String> allAuthorities = Stream.concat(roleAuthorities.stream(), permissionAuthorities.stream())
                    .collect(Collectors.toUnmodifiableSet());

            List<SimpleGrantedAuthority> authorities = allAuthorities.stream()
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());
            AuthenticatedUser principal = new AuthenticatedUser(
                    uidNumber.longValue(),
                    claims.getSubject(),
                    "",
                    authorities
            );
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    principal,
                    null,
                    authorities
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (JwtException ignored) {
            SecurityContextHolder.clearContext();
            Object traceId = request.getAttribute(TraceIdFilter.TRACE_ID_REQUEST_ATTR);
            log.debug("JWT parse failed, traceId={}", traceId == null ? "" : traceId.toString());
        }
        filterChain.doFilter(request, response);
    }
}
