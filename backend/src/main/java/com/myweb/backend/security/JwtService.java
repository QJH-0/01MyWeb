package com.myweb.backend.security;

import com.myweb.backend.config.SecurityProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.io.DecodingException;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Set;

@Service
public class JwtService {
    private final SecretKey key;
    private final long accessTokenTtlSeconds;

    public JwtService(SecurityProperties securityProperties) {
        this.key = Keys.hmacShaKeyFor(resolveSecretBytes(securityProperties.jwt().secret()));
        this.accessTokenTtlSeconds = securityProperties.jwt().accessTokenTtlSeconds();
    }

    private byte[] resolveSecretBytes(String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("JWT secret must not be blank.");
        }

        byte[] decodedBytes = tryDecodeBase64(secret);
        if (decodedBytes != null) {
            return ensureMinKeyLength(decodedBytes);
        }

        byte[] decodedUrlBytes = tryDecodeBase64Url(secret);
        if (decodedUrlBytes != null) {
            return ensureMinKeyLength(decodedUrlBytes);
        }

        return ensureMinKeyLength(secret.getBytes(StandardCharsets.UTF_8));
    }

    private byte[] tryDecodeBase64(String secret) {
        try {
            return Decoders.BASE64.decode(secret);
        } catch (IllegalArgumentException | DecodingException ignored) {
            return null;
        }
    }

    private byte[] tryDecodeBase64Url(String secret) {
        try {
            return Decoders.BASE64URL.decode(secret);
        } catch (IllegalArgumentException | DecodingException ignored) {
            return null;
        }
    }

    private byte[] ensureMinKeyLength(byte[] keyBytes) {
        if (keyBytes.length < 32) {
            throw new IllegalArgumentException("JWT secret must decode to at least 32 bytes.");
        }
        return keyBytes;
    }

    public String generateAccessToken(
            long userId,
            String username,
            Set<String> roles,
            Set<String> permissions
    ) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(accessTokenTtlSeconds);
        return Jwts.builder()
                .subject(username)
                .claim("uid", userId)
                .claim("roles", roles)
                .claim("permissions", permissions)
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(key)
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }
}
