package com.myweb.backend.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.security")
public record SecurityProperties(
        @Valid
        Jwt jwt,
        @Valid
        Captcha captcha,
        @Valid
        Register register,
        @Valid
        Login login,
        @Valid
        Admin admin
) {
    public record Jwt(
            @NotBlank
            String secret,
            @Min(60)
            long accessTokenTtlSeconds,
            @Min(300)
            long refreshTokenTtlSeconds
    ) {
    }

    public record Captcha(
            boolean required,
            @Min(1)
            int minTokenLength
    ) {
    }

    public record Register(
            @Min(1)
            int ipLimitPerMinute
    ) {
    }

    public record Login(
            @Min(1)
            int ipLimitPerMinute
    ) {
    }

    public record Admin(
            @NotBlank
            String token,
            String bootstrapUsername,
            String bootstrapPassword
    ) {
    }
}
