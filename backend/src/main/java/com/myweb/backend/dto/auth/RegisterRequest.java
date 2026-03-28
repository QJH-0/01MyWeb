package com.myweb.backend.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 注册入参：`captchaToken` 是否必填由 {@link com.myweb.backend.config.SecurityProperties} 控制。 */
public record RegisterRequest(
        @NotBlank @Size(min = 3, max = 50) String username,
        @NotBlank @Size(min = 8, max = 128) String password,
        String captchaToken
) {
}
