package com.myweb.backend.security.rbac;

import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 角色到权限的静态映射（代码内建）；DB 中的角色分配与此处集合共同决定最终权限集。
 */
@Component
public class RbacRolePermissionMapping {
    private final EnumMap<RbacRole, Set<RbacPermission>> mapping = new EnumMap<>(RbacRole.class);

    public RbacRolePermissionMapping() {
        mapping.put(RbacRole.ADMIN, EnumSet.of(
                RbacPermission.ADMIN_PANEL,
                RbacPermission.AI_ACCESS,
                RbacPermission.COMMENT_WRITE
        ));
        mapping.put(RbacRole.USER, EnumSet.of(
                RbacPermission.AI_ACCESS,
                RbacPermission.COMMENT_WRITE
        ));
    }

    /**
     * 将角色权限集合映射为细粒度权限集合。
     * 根据配置的角色-权限映射关系，计算用户实际拥有的所有权限。
     *
     * @param roleAuthorities 角色权限字符串集合（如 ["ROLE_ADMIN", "ROLE_USER"]）
     * @return 细粒度权限字符串集合（如 ["PERM_ADMIN_PANEL", "PERM_AI_ACCESS"]）
     */
    public Set<String> permissionsForRoles(Collection<String> roleAuthorities) {
        if (roleAuthorities == null || roleAuthorities.isEmpty()) {
            return Set.of();
        }

        return roleAuthorities.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .map(RbacRole::fromAuthority)
                .flatMap(Optional::stream)
                .flatMap(role -> mapping.getOrDefault(role, Set.of()).stream())
                .map(RbacPermission::authority)
                .collect(Collectors.toUnmodifiableSet());
    }
}

