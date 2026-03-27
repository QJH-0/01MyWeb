package com.myweb.backend.security;

import com.myweb.backend.config.SecurityProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.Set;

@Service
public class JwtService {
    private final SecretKey key;
    private final long accessTokenTtlSeconds;

    public JwtService(SecurityProperties securityProperties) {
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(securityProperties.jwt().secret()));
        this.accessTokenTtlSeconds = securityProperties.jwt().accessTokenTtlSeconds();
    }

    public String generateAccessToken(long userId, String username, Set<String> roles) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(accessTokenTtlSeconds);
        return Jwts.builder()
                .subject(username)
                .claim("uid", userId)
                .claim("roles", roles)
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(key)
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }
}
