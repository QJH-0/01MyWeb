package com.myweb.backend.security.rbac;

import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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

