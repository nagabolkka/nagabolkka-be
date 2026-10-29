package com.nagabolkka.backend.global.security;

import com.nagabolkka.backend.domain.user.entity.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import java.util.Optional;

@Component
public class JwtProvider {

    private static final String ROLE_CLAIM = "role";

    private final SecretKey key;
    private final Duration accessTokenExpiration;

    public JwtProvider(JwtProperties jwtProperties) {
        // 32바이트(256bit) 미만이면 여기서 WeakKeyException → 앱이 뜨지 않는다
        this.key = Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpiration = jwtProperties.accessTokenExpiration();
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

    /** 서명 · 만료가 정상이면 userId, 아니면 empty (예외를 던지지 않는다) */
    public Optional<Long> getUserId(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Optional.of(Long.valueOf(claims.getSubject()));
        } catch (JwtException | IllegalArgumentException e) {
            // 만료(ExpiredJwtException) · 서명 불일치 · 형식 오류 · sub 가 숫자가 아님
            return Optional.empty();
        }
    }
}