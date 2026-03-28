package com.myweb.backend.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

/**
 * 认证用户实体类，实现 Spring Security 的 {@link UserDetails} 接口。
 * 用于承载已认证用户的信息，包括用户ID、用户名、密码哈希和权限集合。
 */
public class AuthenticatedUser implements UserDetails {
    /** 用户唯一标识 */
    private final long userId;
    /** 用户名 */
    private final String username;
    /** 密码哈希值（仅用于满足接口契约，实际认证后不再使用） */
    private final String passwordHash;
    /** 用户权限集合 */
    private final Collection<? extends GrantedAuthority> authorities;

    /**
     * 构造认证用户实例。
     *
     * @param userId       用户ID
     * @param username     用户名
     * @param passwordHash 密码哈希
     * @param authorities  权限集合
     */
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

    /**
     * 获取用户ID。
     *
     * @return 用户ID
     */
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
