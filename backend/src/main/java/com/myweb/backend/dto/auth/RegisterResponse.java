package com.myweb.backend.dto.auth;

public record RegisterResponse(
        long userId,
        String username
) {
}
