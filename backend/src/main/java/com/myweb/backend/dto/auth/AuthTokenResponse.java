package com.myweb.backend.dto.auth;

public record AuthTokenResponse(
        String accessToken,
        String refreshToken,
        long expiresIn
) {
}
