package com.myweb.backend.dto.auth;

import jakarta.validation.constraints.NotBlank;

/** 刷新令牌：由 AuthService 校验轮换表项是否仍有效。 */
public record RefreshRequest(@NotBlank String refreshToken) {
}
