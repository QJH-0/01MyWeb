package com.myweb.backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myweb.backend.common.ApiResponse;
import com.myweb.backend.common.RateLimitFilter;
import com.myweb.backend.common.TraceIdFilter;
import com.myweb.backend.security.AdminTokenFilter;
import com.myweb.backend.security.JwtAuthenticationFilter;
import com.myweb.backend.security.rbac.RbacPermission;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;

/**
 * Spring Security 主链：无状态 JWT + 管理端补充头过滤；高风险路径单独 RBAC 与 AI 限流链顺序在 DSL 中固定。
 */
@Configuration
public class SecurityConfig {
    /**
     * 配置安全过滤器链。
     * 包括 CSRF 禁用、会话管理、请求授权和异常处理。
     *
     * @param http                   HTTP 安全配置
     * @param jwtAuthenticationFilter JWT 认证过滤器
     * @param adminTokenFilter        管理员令牌过滤器
     * @param rateLimitFilter         限流过滤器
     * @param objectMapper            JSON 对象映射器
     * @return 配置好的安全过滤器链
     * @throws Exception 如果配置失败
     */
    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            AdminTokenFilter adminTokenFilter,
            RateLimitFilter rateLimitFilter,
            ObjectMapper objectMapper
    ) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(cfg -> cfg.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(headers -> headers
                        .frameOptions(frameOptions -> frameOptions.deny())
                        .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'"))
                        .httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(31536000))
                        .referrerPolicy(referrer -> referrer.policy(org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                        .cacheControl(cache -> cache.disable())
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/api/health", "/api/auth/register", "/api/auth/login", "/api/auth/refresh").permitAll()
                        .requestMatchers("/api/auth/me", "/api/auth/logout").authenticated()
                        .requestMatchers("/api/test/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/content/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/projects/**", "/api/blogs/**", "/api/search/**", "/api/comments/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/comments/**").hasAuthority(RbacPermission.COMMENT_WRITE.authority())
                        .requestMatchers(HttpMethod.PUT, "/api/comments/**").hasAuthority(RbacPermission.COMMENT_WRITE.authority())
                        .requestMatchers(HttpMethod.DELETE, "/api/comments/**").hasAuthority(RbacPermission.COMMENT_WRITE.authority())
                        .requestMatchers("/api/ai/**").hasAuthority(RbacPermission.AI_ACCESS.authority())
                        .requestMatchers("/api/admin/**").hasAuthority(RbacPermission.ADMIN_PANEL.authority())
                        .anyRequest().denyAll()
                )
                .exceptionHandling(cfg -> cfg
                        .authenticationEntryPoint((req, res, ex) -> writeAuthError(res, req, objectMapper, HttpServletResponse.SC_UNAUTHORIZED, "UNAUTHORIZED"))
                        .accessDeniedHandler((req, res, ex) -> writeAuthError(res, req, objectMapper, HttpServletResponse.SC_FORBIDDEN, "FORBIDDEN"))
                )
                .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(adminTokenFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    /**
     * 创建密码编码器 Bean。
     * 使用 BCrypt 算法进行密码哈希。
     *
     * @return BCrypt 密码编码器
     */
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 写入认证错误响应。
     * 用于处理认证失败和访问被拒绝的情况。
     *
     * @param response     HTTP 响应
     * @param request      HTTP 请求
     * @param objectMapper JSON 对象映射器
     * @param status       HTTP 状态码
     * @param code         错误代码
     * @throws IOException 如果写入响应失败
     */
    private void writeAuthError(
            HttpServletResponse response,
            HttpServletRequest request,
            ObjectMapper objectMapper,
            int status,
            String code
    ) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        String traceId = request.getAttribute(TraceIdFilter.TRACE_ID_REQUEST_ATTR) == null
                ? ""
                : request.getAttribute(TraceIdFilter.TRACE_ID_REQUEST_ATTR).toString();
        objectMapper.writeValue(response.getWriter(), ApiResponse.fail(code, traceId));
    }
}
