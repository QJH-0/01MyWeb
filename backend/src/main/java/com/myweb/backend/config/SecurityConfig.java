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

@Configuration
public class SecurityConfig {
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

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

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
