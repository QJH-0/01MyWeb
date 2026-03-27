package com.myweb.backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myweb.backend.common.ApiResponse;
import com.myweb.backend.common.TraceIdFilter;
import com.myweb.backend.security.AdminTokenFilter;
import com.myweb.backend.security.JwtAuthenticationFilter;
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
            ObjectMapper objectMapper
    ) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(cfg -> cfg.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/api/health", "/api/auth/register", "/api/auth/login", "/api/auth/refresh").permitAll()
                        .requestMatchers("/api/auth/me", "/api/auth/logout").authenticated()
                        .requestMatchers("/api/test/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/projects/**", "/api/blogs/**", "/api/search/**", "/api/comments/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/comments/**").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/comments/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/comments/**").authenticated()
                        .requestMatchers("/api/ai/**").authenticated()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .anyRequest().denyAll()
                )
                .exceptionHandling(cfg -> cfg
                        .authenticationEntryPoint((req, res, ex) -> writeAuthError(res, req, objectMapper, HttpServletResponse.SC_UNAUTHORIZED, "UNAUTHORIZED"))
                        .accessDeniedHandler((req, res, ex) -> writeAuthError(res, req, objectMapper, HttpServletResponse.SC_FORBIDDEN, "FORBIDDEN"))
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(adminTokenFilter, JwtAuthenticationFilter.class)
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
