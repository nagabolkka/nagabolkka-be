package com.nagabolkka.backend.domain.auth.service;

import com.nagabolkka.backend.domain.auth.dto.TokenResponse;
import com.nagabolkka.backend.domain.auth.entity.RefreshToken;
import com.nagabolkka.backend.domain.auth.repository.RefreshTokenRepository;
import com.nagabolkka.backend.domain.user.entity.User;
import com.nagabolkka.backend.global.crypto.CryptoProperties;
import com.nagabolkka.backend.global.crypto.HashUtil;
import com.nagabolkka.backend.global.exception.CustomException;
import com.nagabolkka.backend.global.exception.ErrorCode;
import com.nagabolkka.backend.global.security.JwtProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class TokenServiceTest {

    @Mock
    JwtProvider jwtProvider;
    @Mock
    RefreshTokenRepository refreshTokenRepository;

    HashUtil hashUtil = new HashUtil(new CryptoProperties("test-hash-secret-0123456789abcdef", null));
    TokenService tokenService;
    User user = User.createKakaoUser("홍윤지");

    @BeforeEach
    void setUp() {
        tokenService = new TokenService(jwtProvider, refreshTokenRepository, hashUtil);
    }

    private void givenJwt() {
        given(jwtProvider.createAccessToken(any(), any())).willReturn("access-token");
        given(jwtProvider.getAccessTokenExpiresIn()).willReturn(1800L);
        given(jwtProvider.getRefreshTokenExpiration()).willReturn(Duration.ofDays(14));
    }

    @Test
    @DisplayName("발급 → DB 에는 원문이 아니라 해시, 만료 14일 뒤")
    void issue_savesHashOnly() {
        givenJwt();

        TokenResponse response = tokenService.issue(user);

        ArgumentCaptor<RefreshToken> saved = ArgumentCaptor.forClass(RefreshToken.class);
        then(refreshTokenRepository).should().save(saved.capture());
        assertThat(saved.getValue().getTokenHash())
                .hasSize(64)
                .isEqualTo(hashUtil.sha256(response.refreshToken()))
                .isNotEqualTo(response.refreshToken());
        assertThat(saved.getValue().getExpiresAt()).isAfter(LocalDateTime.now().plusDays(13));
        assertThat(response.accessTokenExpiresIn()).isEqualTo(1800);
    }

    @Test
    @DisplayName("재발급 → 기존 행 삭제 + 새 Refresh (rotation)")
    void rotate_success() {
        givenJwt();
        RefreshToken old = RefreshToken.of(user, hashUtil.sha256("old"), LocalDateTime.now().plusDays(1));
        given(refreshTokenRepository.findByTokenHash(hashUtil.sha256("old"))).willReturn(Optional.of(old));

        TokenResponse response = tokenService.rotate("old");

        then(refreshTokenRepository).should().delete(old);
        assertThat(response.refreshToken()).isNotEqualTo("old");
    }

    @Test
    @DisplayName("없는(이미 회전된) Refresh → AUTH_REFRESH_EXPIRED")
    void rotate_notFound() {
        given(refreshTokenRepository.findByTokenHash(any())).willReturn(Optional.empty());

        assertThatThrownBy(() -> tokenService.rotate("used"))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.AUTH_REFRESH_EXPIRED);
    }

    @Test
    @DisplayName("만료된 Refresh → 행 삭제 후 AUTH_REFRESH_EXPIRED, 새로 발급하지 않음")
    void rotate_expired() {
        RefreshToken expired = RefreshToken.of(user, hashUtil.sha256("old"), LocalDateTime.now().minusMinutes(1));
        given(refreshTokenRepository.findByTokenHash(hashUtil.sha256("old"))).willReturn(Optional.of(expired));

        assertThatThrownBy(() -> tokenService.rotate("old"))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.AUTH_REFRESH_EXPIRED);

        then(refreshTokenRepository).should().delete(expired);
        then(jwtProvider).should(never()).createAccessToken(any(), any());
    }
}