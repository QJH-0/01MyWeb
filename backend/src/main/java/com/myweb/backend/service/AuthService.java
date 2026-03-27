package com.myweb.backend.service;

import com.myweb.backend.common.ApiException;
import com.myweb.backend.config.SecurityProperties;
import com.myweb.backend.dto.auth.AuthTokenResponse;
import com.myweb.backend.dto.auth.LoginRequest;
import com.myweb.backend.dto.auth.MeResponse;
import com.myweb.backend.dto.auth.RefreshRequest;
import com.myweb.backend.dto.auth.RegisterRequest;
import com.myweb.backend.dto.auth.RegisterResponse;
import com.myweb.backend.entity.UserAccount;
import com.myweb.backend.repository.UserAccountRepository;
import com.myweb.backend.security.AuthenticatedUser;
import com.myweb.backend.security.JwtService;
import jakarta.annotation.PostConstruct;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class AuthService {
    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final SecurityProperties securityProperties;
    private final ConcurrentMap<String, RefreshTokenRecord> refreshTokens = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, RegisterWindow> registerWindows = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, RegisterWindow> loginWindows = new ConcurrentHashMap<>();

    public AuthService(
            UserAccountRepository userAccountRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            SecurityProperties securityProperties
    ) {
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.securityProperties = securityProperties;
    }

    @PostConstruct
    public void bootstrapAdminAccount() {
        String username = securityProperties.admin().bootstrapUsername();
        String password = securityProperties.admin().bootstrapPassword();
        if (username != null && !username.isBlank() && password != null && !password.isBlank()) {
            userAccountRepository.saveAdminIfAbsent(username, passwordEncoder.encode(password));
        }
    }

    public RegisterResponse register(RegisterRequest request, String sourceIp) {
        validateCaptcha(request.captchaToken());
        enforceRegisterRateLimit(sourceIp);
        enforceStrongPassword(request.password());
        UserAccount account = userAccountRepository.save(request.username(), passwordEncoder.encode(request.password()));
        return new RegisterResponse(account.userId(), account.username());
    }

    public AuthTokenResponse login(LoginRequest request, String sourceIp) {
        enforceLoginRateLimit(sourceIp);
        UserAccount account = userAccountRepository.findByUsername(request.username())
                .orElseThrow(() -> unauthorized("invalid credentials"));
        if (!passwordEncoder.matches(request.password(), account.passwordHash())) {
            throw unauthorized("invalid credentials");
        }
        return issueTokens(account);
    }

    public AuthTokenResponse refresh(RefreshRequest request) {
        RefreshTokenRecord record = refreshTokens.remove(request.refreshToken());
        if (record == null || record.expiresAt().isBefore(Instant.now())) {
            throw unauthorized("invalid refresh token");
        }
        UserAccount account = userAccountRepository.findById(record.userId())
                .orElseThrow(() -> unauthorized("invalid refresh token"));
        return issueTokens(account);
    }

    public void logout(String refreshToken) {
        refreshTokens.remove(refreshToken);
    }

    public MeResponse me(AuthenticatedUser user) {
        return new MeResponse(
                user.userId(),
                user.getUsername(),
                user.getAuthorities().stream().map(Objects::toString).collect(java.util.stream.Collectors.toUnmodifiableSet())
        );
    }

    private AuthTokenResponse issueTokens(UserAccount account) {
        String accessToken = jwtService.generateAccessToken(account.userId(), account.username(), account.roles());
        String refreshToken = UUID.randomUUID().toString();
        long expiresIn = securityProperties.jwt().accessTokenTtlSeconds();
        refreshTokens.put(
                refreshToken,
                new RefreshTokenRecord(account.userId(), Instant.now().plusSeconds(securityProperties.jwt().refreshTokenTtlSeconds()))
        );
        return new AuthTokenResponse(accessToken, refreshToken, expiresIn);
    }

    private void enforceStrongPassword(String password) {
        if (password.length() < 8 || !password.matches(".*[A-Za-z].*") || !password.matches(".*\\d.*")) {
            throw validation("weak password");
        }
    }

    private void validateCaptcha(String captchaToken) {
        if (!securityProperties.captcha().required()) {
            return;
        }
        if (captchaToken == null || captchaToken.isBlank() || captchaToken.length() < securityProperties.captcha().minTokenLength()) {
            throw validation("captcha token invalid");
        }
    }

    private void enforceRegisterRateLimit(String sourceIp) {
        enforceRateLimit(registerWindows, sourceIp, securityProperties.register().ipLimitPerMinute(), "register rate limited");
    }

    private void enforceLoginRateLimit(String sourceIp) {
        enforceRateLimit(loginWindows, sourceIp, securityProperties.login().ipLimitPerMinute(), "login rate limited");
    }

    private void enforceRateLimit(
            ConcurrentMap<String, RegisterWindow> windows,
            String sourceIp,
            int maxPerMinute,
            String errorMessage
    ) {
        Instant now = Instant.now();
        RegisterWindow updated = windows.compute(sourceIp, (ip, current) -> {
            RegisterWindow next = current;
            if (next == null || next.windowEnd().isBefore(now)) {
                return new RegisterWindow(now.plusSeconds(60), 1);
            }
            return new RegisterWindow(next.windowEnd(), next.count() + 1);
        });
        if (updated != null && updated.count() > maxPerMinute) {
            throw validation(errorMessage);
        }
    }

    private ApiException unauthorized(String message) {
        return new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", message);
    }

    private ApiException validation(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message);
    }

    private record RefreshTokenRecord(long userId, Instant expiresAt) {
    }

    private record RegisterWindow(Instant windowEnd, int count) {
    }
}
