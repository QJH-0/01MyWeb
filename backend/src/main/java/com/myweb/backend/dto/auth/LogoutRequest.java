package com.myweb.backend.dto.auth;

import jakarta.validation.constraints.NotBlank;

/** 登出：使服务端持有的 refresh 记录失效（与内存实现一致）。 */
public record LogoutRequest(@NotBlank String refreshToken) {
}
