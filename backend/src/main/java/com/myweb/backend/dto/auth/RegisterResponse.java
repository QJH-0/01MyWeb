package com.myweb.backend.dto.auth;

/** 注册成功片段：不含 token，客户端需再走 login 或后续流程取凭证。 */
public record RegisterResponse(
        long userId,
        String username
) {
}
