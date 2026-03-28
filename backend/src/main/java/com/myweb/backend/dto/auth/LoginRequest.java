package com.myweb.backend.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 登录入参：密码上限与哈希存储列兼容，防止超长 DoS。 */
public record LoginRequest(
        @NotBlank @Size(max = 50) String username,
        @NotBlank @Size(max = 128) String password
) {
}
