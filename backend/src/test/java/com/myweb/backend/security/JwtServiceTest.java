package com.myweb.backend.security;

import com.myweb.backend.config.SecurityProperties;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtServiceTest {

    @Test
    void shouldGenerateAndParseTokenWhenSecretIsPlainText() {
        JwtService jwtService = new JwtService(buildProperties("this_is_a_plain_text_secret_key_32chars!!0000"));
        String token = jwtService.generateAccessToken(
                1L,
                "demo",
                Set.of("ROLE_USER"),
                Set.of("PERM_AI_ACCESS")
        );
        Claims claims = jwtService.parse(token);

        assertEquals("demo", claims.getSubject());
        assertEquals(1, claims.get("uid", Integer.class));
        assertIterableEquals(List.of("ROLE_USER"), claims.get("roles", List.class));
        assertIterableEquals(List.of("PERM_AI_ACCESS"), claims.get("permissions", List.class));
    }

    @Test
    void shouldGenerateAndParseTokenWhenSecretIsBase64() {
        String base64Secret = Base64.getEncoder()
                .encodeToString("this_is_a_plain_text_secret_key_32chars!!0000".getBytes(StandardCharsets.UTF_8));
        JwtService jwtService = new JwtService(buildProperties(base64Secret));

        String token = jwtService.generateAccessToken(
                2L,
                "base64-user",
                Set.of("ROLE_USER"),
                Set.of("PERM_COMMENT_WRITE")
        );
        Claims claims = jwtService.parse(token);

        assertEquals("base64-user", claims.getSubject());
        assertEquals(2, claims.get("uid", Integer.class));
        assertIterableEquals(List.of("ROLE_USER"), claims.get("roles", List.class));
        assertIterableEquals(List.of("PERM_COMMENT_WRITE"), claims.get("permissions", List.class));
    }

    @Test
    void shouldGenerateAndParseTokenWhenSecretIsBase64Url() {
        String base64UrlSecret = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("this_is_a_plain_text_secret_key_32chars!!0000".getBytes(StandardCharsets.UTF_8));
        JwtService jwtService = new JwtService(buildProperties(base64UrlSecret));

        String token = jwtService.generateAccessToken(
                3L,
                "base64url-user",
                Set.of("ROLE_ADMIN"),
                Set.of("PERM_ADMIN_PANEL")
        );
        Claims claims = jwtService.parse(token);

        assertEquals("base64url-user", claims.getSubject());
        assertEquals(3, claims.get("uid", Integer.class));
        assertIterableEquals(List.of("ROLE_ADMIN"), claims.get("roles", List.class));
        assertIterableEquals(List.of("PERM_ADMIN_PANEL"), claims.get("permissions", List.class));
    }

    @Test
    void shouldThrowWhenSecretIsTooShortAndNotBase64() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new JwtService(buildProperties("short-secret"))
        );

        assertEquals(
                "JWT secret must decode to at least 32 bytes.",
                exception.getMessage()
        );
    }

    @Test
    void shouldThrowWhenSecretIsBlank() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new JwtService(buildProperties("   "))
        );

        assertEquals("JWT secret must not be blank.", exception.getMessage());
    }

    @Test
    void shouldThrowWhenDecodedBase64SecretIsTooShort() {
        String shortDecoded = Base64.getEncoder().encodeToString("short-secret".getBytes(StandardCharsets.UTF_8));
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new JwtService(buildProperties(shortDecoded))
        );

        assertEquals("JWT secret must decode to at least 32 bytes.", exception.getMessage());
    }

    private SecurityProperties buildProperties(String secret) {
        return new SecurityProperties(
                new SecurityProperties.Jwt(secret, 3600, 7200),
                new SecurityProperties.Captcha(false, 1),
                new SecurityProperties.Register(5),
                new SecurityProperties.Login(20),
                new SecurityProperties.Admin("admin-token", "", "")
        );
    }
}
