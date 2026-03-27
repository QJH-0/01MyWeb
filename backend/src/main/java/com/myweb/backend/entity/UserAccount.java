package com.myweb.backend.entity;

import java.util.Set;

public record UserAccount(
        long userId,
        String username,
        String passwordHash,
        Set<String> roles
) {
}
