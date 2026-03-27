package com.myweb.backend.service;

import com.myweb.backend.common.ApiException;
import com.myweb.backend.common.ErrorCodes;
import com.myweb.backend.config.SecurityProperties;
import com.myweb.backend.dto.auth.AuthTokenResponse;
import com.myweb.backend.dto.auth.LoginRequest;
import com.myweb.backend.dto.auth.MeResponse;
import com.myweb.backend.dto.auth.RefreshRequest;
import com.myweb.backend.dto.auth.RegisterRequest;
import com.myweb.backend.dto.auth.RegisterResponse;
import com.myweb.backend.entity.UserAccountEntity;
import com.myweb.backend.repository.UserAccountRepository;
import com.myweb.backend.security.AuthenticatedUser;
import com.myweb.backend.security.JwtService;
import jakarta.annotation.PostConstruct;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

@Service
public class AuthService {
    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final SecurityProperties securityProperties;
    private final RbacAuthorizationService rbacAuthorizationService;
    private final ConcurrentMap<String, RefreshTokenRecord> refreshTokens = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, RegisterWindow> registerWindows = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, RegisterWindow> loginWindows = new ConcurrentHashMap<>();

    public AuthService(
            UserAccountRepository userAccountRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            SecurityProperties securityProperties,
            RbacAuthorizationService rbacAuthorizationService
    ) {
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.securityProperties = securityProperties;
        this.rbacAuthorizationService = rbacAuthorizationService;
    }

    @PostConstruct
    public void bootstrapAdminAccount() {
        String username = securityProperties.admin().bootstrapUsername();
        String password = securityProperties.admin().bootstrapPassword();
        if (username != null && !username.isBlank() && password != null && !password.isBlank()) {
            saveAdminIfAbsent(username, passwordEncoder.encode(password));
        }
    }

    @Transactional
    public RegisterResponse register(RegisterRequest request, String sourceIp) {
        validateCaptcha(request.captchaToken());
        enforceRegisterRateLimit(sourceIp);
        enforceStrongPassword(request.password());
        UserAccountEntity account = saveUser(request.username(), passwordEncoder.encode(request.password()));
        return new RegisterResponse(account.getUserId(), account.getUsername());
    }

    public AuthTokenResponse login(LoginRequest request, String sourceIp) {
        enforceLoginRateLimit(sourceIp);
        UserAccountEntity account = userAccountRepository.findByUsername(request.username())
                .orElseThrow(() -> authUnauthorized(ErrorCodes.AUTH_INVALID_CREDENTIALS, "invalid credentials"));
        if (!passwordEncoder.matches(request.password(), account.getPasswordHash())) {
            throw authUnauthorized(ErrorCodes.AUTH_INVALID_CREDENTIALS, "invalid credentials");
        }
        return issueTokens(account);
    }

    public AuthTokenResponse refresh(RefreshRequest request) {
        RefreshTokenRecord record = refreshTokens.remove(request.refreshToken());
        if (record == null || record.expiresAt().isBefore(Instant.now())) {
            throw authUnauthorized(ErrorCodes.AUTH_INVALID_REFRESH_TOKEN, "invalid refresh token");
        }
        UserAccountEntity account = userAccountRepository.findById(record.userId())
                .orElseThrow(() -> authUnauthorized(ErrorCodes.AUTH_INVALID_REFRESH_TOKEN, "invalid refresh token"));
        return issueTokens(account);
    }

    public void logout(String refreshToken) {
        refreshTokens.remove(refreshToken);
    }

    public MeResponse me(AuthenticatedUser user) {
        Set<String> roles = user.getAuthorities().stream()
                .map(Objects::toString)
                .filter(auth -> auth.startsWith("ROLE_"))
                .collect(Collectors.toUnmodifiableSet());

        Set<String> permissions = user.getAuthorities().stream()
                .map(Objects::toString)
                .filter(auth -> auth.startsWith("PERM_"))
                .collect(Collectors.toUnmodifiableSet());
        return new MeResponse(
                user.userId(),
                user.getUsername(),
                roles,
                permissions
        );
    }

    private AuthTokenResponse issueTokens(UserAccountEntity account) {
        RbacAuthorizationService.UserRbacAuthorities authorities =
                rbacAuthorizationService.loadAuthorities(account.getUserId());

        if (authorities.roleAuthorities().isEmpty() || authorities.permissionAuthorities().isEmpty()) {
            throw new ApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    ErrorCodes.INTERNAL_ERROR,
                    "rbac authorities not configured"
            );
        }

        String accessToken = jwtService.generateAccessToken(
                account.getUserId(),
                account.getUsername(),
                authorities.roleAuthorities(),
                authorities.permissionAuthorities()
        );
        String refreshToken = UUID.randomUUID().toString();
        long expiresIn = securityProperties.jwt().accessTokenTtlSeconds();
        refreshTokens.put(
                refreshToken,
                new RefreshTokenRecord(account.getUserId(), Instant.now().plusSeconds(securityProperties.jwt().refreshTokenTtlSeconds()))
        );
        return new AuthTokenResponse(accessToken, refreshToken, expiresIn);
    }

    private UserAccountEntity saveUser(String username, String passwordHash) {
        UserAccountEntity entity = new UserAccountEntity();
        entity.setUsername(username);
        entity.setPasswordHash(passwordHash);
        try {
            UserAccountEntity saved = userAccountRepository.saveAndFlush(entity);
            rbacAuthorizationService.assignRoleToUser(saved.getUserId(), "ROLE_USER");
            return saved;
        } catch (DataIntegrityViolationException ex) {
            throw authValidation(ErrorCodes.AUTH_USERNAME_EXISTS, "username already exists");
        }
    }

    @Transactional
    private UserAccountEntity saveAdminIfAbsent(String username, String passwordHash) {
        Optional<UserAccountEntity> existing = userAccountRepository.findByUsername(username);
        if (existing.isPresent()) {
            UserAccountEntity account = existing.get();
            rbacAuthorizationService.assignRoleToUser(account.getUserId(), "ROLE_ADMIN");
            rbacAuthorizationService.assignRoleToUser(account.getUserId(), "ROLE_USER");
            return account;
        }
        UserAccountEntity entity = new UserAccountEntity();
        entity.setUsername(username);
        entity.setPasswordHash(passwordHash);
        try {
            UserAccountEntity saved = userAccountRepository.saveAndFlush(entity);
            rbacAuthorizationService.assignRoleToUser(saved.getUserId(), "ROLE_ADMIN");
            rbacAuthorizationService.assignRoleToUser(saved.getUserId(), "ROLE_USER");
            return saved;
        } catch (DataIntegrityViolationException ex) {
            return userAccountRepository.findByUsername(username)
                    .orElseThrow(() -> new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCodes.INTERNAL_ERROR, "failed to persist user"));
        }
    }

    private void enforceStrongPassword(String password) {
        if (password.length() < 8 || !password.matches(".*[A-Za-z].*") || !password.matches(".*\\d.*")) {
            throw authValidation(ErrorCodes.AUTH_WEAK_PASSWORD, "weak password");
        }
    }

    private void validateCaptcha(String captchaToken) {
        if (!securityProperties.captcha().required()) {
            return;
        }
        if (captchaToken == null || captchaToken.isBlank() || captchaToken.length() < securityProperties.captcha().minTokenLength()) {
            throw authValidation(ErrorCodes.AUTH_CAPTCHA_INVALID, "captcha token invalid");
        }
    }

    private void enforceRegisterRateLimit(String sourceIp) {
        enforceRateLimit(
                registerWindows,
                sourceIp,
                securityProperties.register().ipLimitPerMinute(),
                ErrorCodes.AUTH_REGISTER_RATE_LIMITED,
                "register rate limited"
        );
    }

    private void enforceLoginRateLimit(String sourceIp) {
        enforceRateLimit(
                loginWindows,
                sourceIp,
                securityProperties.login().ipLimitPerMinute(),
                ErrorCodes.AUTH_LOGIN_RATE_LIMITED,
                "login rate limited"
        );
    }

    private void enforceRateLimit(
            ConcurrentMap<String, RegisterWindow> windows,
            String sourceIp,
            int maxPerMinute,
            String errorCode,
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
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, errorCode, errorMessage);
        }
    }

    private ApiException authUnauthorized(String errorCode, String message) {
        return new ApiException(HttpStatus.UNAUTHORIZED, errorCode, message);
    }

    private ApiException authValidation(String errorCode, String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, errorCode, message);
    }

    private record RefreshTokenRecord(long userId, Instant expiresAt) {
    }

    private record RegisterWindow(Instant windowEnd, int count) {
    }
}
