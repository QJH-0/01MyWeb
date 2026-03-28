package com.myweb.backend.security.rbac;

import java.util.Optional;

/** 系统角色枚举；authority 字符串需与 JWT / GrantedAuthority 一致。 */
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

    /**
     * 根据角色权限字符串查找对应的角色枚举。
     *
     * @param authority 角色权限字符串（如 "ROLE_ADMIN"）
     * @return 对应的角色枚举，若未找到则返回空
     */
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
