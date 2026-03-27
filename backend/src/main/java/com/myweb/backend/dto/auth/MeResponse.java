package com.myweb.backend.dto.auth;

import java.util.Set;

public record MeResponse(
        long userId,
        String username,
        Set<String> roles,
        Set<String> permissions
) {
}
