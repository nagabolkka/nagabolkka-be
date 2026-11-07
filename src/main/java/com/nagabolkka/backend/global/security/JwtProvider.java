package com.nagabolkka.backend.global.security;

import com.nagabolkka.backend.domain.user.entity.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;

@Component
public class JwtProvider {

    private static final String ROLE_CLAIM = "role";

    private final SecretKey key;
    private final Duration accessTokenExpiration;
    private final Duration refreshTokenExpiration;

    public JwtProvider(JwtProperties jwtProperties) {
        this.key = Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpiration = jwtProperties.accessTokenExpiration();
        this.refreshTokenExpiration = jwtProperties.refreshTokenExpiration();
    }

    public String createAccessToken(Long userId, Role role) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + accessTokenExpiration.toMillis());

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim(ROLE_CLAIM, role.name())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    public JwtResult parse(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return JwtResult.valid(Long.valueOf(claims.getSubject()));
        } catch (ExpiredJwtException e) {
            return JwtResult.expired();
        } catch (JwtException | IllegalArgumentException e) {
            // 서명 불일치 · 형식 오류 · sub 가 숫자가 아님 (NumberFormatException 도 IllegalArgumentException)
            return JwtResult.invalid();
        }
    }

    /** 응답의 accessTokenExpiresIn (초) */
    public long getAccessTokenExpiresIn() {
        return accessTokenExpiration.toSeconds();
    }

    public Duration getRefreshTokenExpiration() {
        return refreshTokenExpiration;
    }
}