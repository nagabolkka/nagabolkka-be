package com.nagabolkka.backend.domain.auth.service;

import com.nagabolkka.backend.domain.auth.dto.TokenResponse;
import com.nagabolkka.backend.domain.auth.entity.RefreshToken;
import com.nagabolkka.backend.domain.auth.repository.RefreshTokenRepository;
import com.nagabolkka.backend.domain.user.entity.User;
import com.nagabolkka.backend.global.crypto.HashUtil;
import com.nagabolkka.backend.global.exception.CustomException;
import com.nagabolkka.backend.global.exception.ErrorCode;
import com.nagabolkka.backend.global.security.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class TokenService {

    private static final int REFRESH_TOKEN_BYTES = 32;

    private final JwtProvider jwtProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final HashUtil hashUtil;
    private final SecureRandom secureRandom = new SecureRandom();

    /** 로그인 · 재발급 공통 : Access + 새 Refresh */
    @Transactional
    public TokenResponse issue(User user) {
        String accessToken = jwtProvider.createAccessToken(user.getId(), user.getRole());
        String refreshToken = newRefreshToken();

        LocalDateTime expiresAt = LocalDateTime.now().plus(jwtProvider.getRefreshTokenExpiration());
        refreshTokenRepository.save(RefreshToken.of(user, hashUtil.sha256(refreshToken), expiresAt));

        return new TokenResponse(accessToken, jwtProvider.getAccessTokenExpiresIn(), refreshToken);
    }

    // 재발급
    @Transactional(noRollbackFor = CustomException.class)
    public TokenResponse rotate(String refreshToken) {
        RefreshToken saved = refreshTokenRepository.findByTokenHash(hashUtil.sha256(refreshToken))
                .orElseThrow(() -> new CustomException(ErrorCode.AUTH_REFRESH_EXPIRED));

        refreshTokenRepository.delete(saved);

        if (saved.isExpired(LocalDateTime.now())) {
            throw new CustomException(ErrorCode.AUTH_REFRESH_EXPIRED);
        }
        return issue(saved.getUser());
    }

    // 로그아웃
    @Transactional
    public void logout(String refreshToken) {
        refreshTokenRepository.deleteByTokenHash(hashUtil.sha256(refreshToken));
    }

    private String newRefreshToken() {
        byte[] bytes = new byte[REFRESH_TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}