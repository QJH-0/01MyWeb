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

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
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
            if (!(uidValue instanceof Number uidNumber) || !(rolesValue instanceof List<?> rolesRaw)) {
                filterChain.doFilter(request, response);
                return;
            }
            List<SimpleGrantedAuthority> authorities = rolesRaw.stream()
                    .map(String::valueOf)
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
