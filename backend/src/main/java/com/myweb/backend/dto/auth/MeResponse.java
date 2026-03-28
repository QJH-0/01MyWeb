package com.myweb.backend.dto.auth;

import java.util.Set;

/** 当前用户画像：角色为 `ROLE_*`，权限为 `PERM_*`，与 JWT claims 同源策略。 */
public record MeResponse(
        long userId,
        String username,
        Set<String> roles,
        Set<String> permissions
) {
}
