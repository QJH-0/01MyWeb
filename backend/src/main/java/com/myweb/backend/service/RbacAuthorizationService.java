package com.myweb.backend.service;

import com.myweb.backend.common.ApiException;
import com.myweb.backend.common.ErrorCodes;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
public class RbacAuthorizationService {
    private final JdbcTemplate jdbcTemplate;

    public RbacAuthorizationService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public UserRbacAuthorities loadAuthorities(long userId) {
        List<String> roleAuthoritiesRaw = jdbcTemplate.query(
                """
                        SELECT r.authority
                        FROM rbac_user_roles ur
                        JOIN rbac_roles r ON ur.role_id = r.role_id
                        WHERE ur.user_id = ?
                        """,
                (rs, rowNum) -> rs.getString("authority"),
                userId
        );
        Set<String> roleAuthorities = Set.copyOf(roleAuthoritiesRaw);

        List<String> permissionAuthoritiesRaw = jdbcTemplate.query(
                """
                        SELECT DISTINCT p.authority
                        FROM rbac_user_roles ur
                        JOIN rbac_role_permissions rp ON ur.role_id = rp.role_id
                        JOIN rbac_permissions p ON rp.permission_id = p.permission_id
                        WHERE ur.user_id = ?
                        """,
                (rs, rowNum) -> rs.getString("authority"),
                userId
        );
        Set<String> permissionAuthorities = Set.copyOf(permissionAuthoritiesRaw);

        return new UserRbacAuthorities(roleAuthorities, permissionAuthorities);
    }

    @Transactional
    public void assignRoleToUser(long userId, String roleAuthority) {
        Integer roleCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM rbac_roles WHERE authority = ?",
                Integer.class,
                roleAuthority
        );
        if (roleCount == null || roleCount == 0) {
            throw new ApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    ErrorCodes.INTERNAL_ERROR,
                    "rbac role authority not configured"
            );
        }

        jdbcTemplate.update(
                """
                        INSERT INTO rbac_user_roles(user_id, role_id)
                        SELECT ?, r.role_id
                        FROM rbac_roles r
                        WHERE r.authority = ?
                          AND NOT EXISTS (
                              SELECT 1
                              FROM rbac_user_roles ur
                              WHERE ur.user_id = ?
                                AND ur.role_id = r.role_id
                          )
                        """,
                userId,
                roleAuthority,
                userId
        );
    }

    public record UserRbacAuthorities(Set<String> roleAuthorities, Set<String> permissionAuthorities) {
    }
}

