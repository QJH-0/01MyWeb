package com.myweb.backend.dto.auth;

/** 登录/刷新成功：expiresIn 为 access token 秒级 TTL，供前端调度刷新。 */
public record AuthTokenResponse(
        String accessToken,
        String refreshToken,
        long expiresIn
) {
}
