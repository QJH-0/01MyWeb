package com.myweb.backend.security.rbac;

import java.util.Optional;

public enum RbacRole {
    USER("ROLE_USER"),
    ADMIN("ROLE_ADMIN");

    private final String authority;

    RbacRole(String authority) {
        this.authority = authority;
    }

    public String authority() {
        return authority;
    }

    public static Optional<RbacRole> fromAuthority(String authority) {
        if (authority == null || authority.isBlank()) {
            return Optional.empty();
        }
        for (RbacRole role : values()) {
            if (role.authority.equals(authority)) {
                return Optional.of(role);
            }
        }
        return Optional.empty();
    }
}

