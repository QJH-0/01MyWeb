package com.myweb.backend.security.rbac;

/** 细粒度权限标识；前端菜单与后端 {@code hasAuthority} 依赖同一 authority 字面量。 */
public enum RbacPermission {
    ADMIN_PANEL("PERM_ADMIN_PANEL"),
    AI_ACCESS("PERM_AI_ACCESS"),
    COMMENT_WRITE("PERM_COMMENT_WRITE");

    private final String authority;

    RbacPermission(String authority) {
        this.authority = authority;
    }

    public String authority() {
        return authority;
    }
}

