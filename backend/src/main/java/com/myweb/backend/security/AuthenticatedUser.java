package com.myweb.backend.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

public class AuthenticatedUser implements UserDetails {
    private final long userId;
    private final String username;
    private final String passwordHash;
    private final Collection<? extends GrantedAuthority> authorities;

    public AuthenticatedUser(
            long userId,
            String username,
            String passwordHash,
            Collection<? extends GrantedAuthority> authorities
    ) {
        this.userId = userId;
        this.username = username;
        this.passwordHash = passwordHash;
        this.authorities = authorities;
    }

    public long userId() {
        return userId;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return username;
    }
}
