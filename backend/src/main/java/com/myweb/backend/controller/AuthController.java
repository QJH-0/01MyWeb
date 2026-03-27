package com.myweb.backend.controller;

import com.myweb.backend.common.ApiResponse;
import com.myweb.backend.common.TraceIdFilter;
import com.myweb.backend.dto.auth.AuthTokenResponse;
import com.myweb.backend.dto.auth.LoginRequest;
import com.myweb.backend.dto.auth.LogoutRequest;
import com.myweb.backend.dto.auth.MeResponse;
import com.myweb.backend.dto.auth.RefreshRequest;
import com.myweb.backend.dto.auth.RegisterRequest;
import com.myweb.backend.dto.auth.RegisterResponse;
import com.myweb.backend.security.AuthenticatedUser;
import com.myweb.backend.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ApiResponse<RegisterResponse> register(@RequestBody @Valid RegisterRequest request, HttpServletRequest httpRequest) {
        String traceId = traceId(httpRequest);
        String sourceIp = httpRequest.getRemoteAddr() == null ? "unknown" : httpRequest.getRemoteAddr();
        return ApiResponse.ok(authService.register(request, sourceIp), traceId);
    }

    @PostMapping("/login")
    public ApiResponse<AuthTokenResponse> login(@RequestBody @Valid LoginRequest request, HttpServletRequest httpRequest) {
        String sourceIp = httpRequest.getRemoteAddr() == null ? "unknown" : httpRequest.getRemoteAddr();
        return ApiResponse.ok(authService.login(request, sourceIp), traceId(httpRequest));
    }

    @PostMapping("/refresh")
    public ApiResponse<AuthTokenResponse> refresh(@RequestBody @Valid RefreshRequest request, HttpServletRequest httpRequest) {
        return ApiResponse.ok(authService.refresh(request), traceId(httpRequest));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestBody @Valid LogoutRequest request, HttpServletRequest httpRequest) {
        authService.logout(request.refreshToken());
        return ApiResponse.ok(null, traceId(httpRequest));
    }

    @GetMapping("/me")
    public ApiResponse<MeResponse> me(@AuthenticationPrincipal AuthenticatedUser principal, HttpServletRequest httpRequest) {
        return ApiResponse.ok(authService.me(principal), traceId(httpRequest));
    }

    private String traceId(HttpServletRequest request) {
        Object traceAttr = request.getAttribute(TraceIdFilter.TRACE_ID_REQUEST_ATTR);
        return traceAttr == null ? "" : traceAttr.toString();
    }
}
