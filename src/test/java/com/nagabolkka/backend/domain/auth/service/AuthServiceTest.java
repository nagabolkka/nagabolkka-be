package com.nagabolkka.backend.domain.auth.service;

import com.nagabolkka.backend.domain.auth.dto.KakaoLoginRequest;
import com.nagabolkka.backend.domain.auth.dto.KakaoLoginRequest.Consents;
import com.nagabolkka.backend.domain.auth.dto.LoginResponse;
import com.nagabolkka.backend.domain.auth.kakao.KakaoOAuthClient;
import com.nagabolkka.backend.domain.auth.kakao.KakaoProperties;
import com.nagabolkka.backend.domain.auth.kakao.KakaoUserInfo;
import com.nagabolkka.backend.domain.user.entity.Role;
import com.nagabolkka.backend.domain.user.entity.User;
import com.nagabolkka.backend.domain.user.repository.UserRepository;
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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String REDIRECT_URI = "http://localhost:5173/oauth/kakao";
    private static final Long KAKAO_ID = 1234567890L;

    @Mock
    KakaoOAuthClient kakaoOAuthClient;
    @Mock
    UserRepository userRepository;
    @Mock
    JwtProvider jwtProvider;

    AuthService authService;

    @BeforeEach
    void setUp() {
        KakaoProperties kakaoProperties = new KakaoProperties("rest-key", "", List.of(REDIRECT_URI));
        authService = new AuthService(kakaoOAuthClient, userRepository, jwtProvider, kakaoProperties);
    }

    private KakaoLoginRequest request(String redirectUri, Consents consents) {
        return new KakaoLoginRequest("auth-code", redirectUri, consents);
    }

    @Test
    @DisplayName("허용 목록에 없는 redirectUri → INVALID_REDIRECT_URI, 카카오는 호출하지 않음")
    void invalidRedirectUri() {
        assertThatThrownBy(() -> authService.kakaoLogin(request("https://evil.com/oauth/kakao", null)))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_REDIRECT_URI);

        then(kakaoOAuthClient).should(never()).getUserInfo(any(), any());
    }

    @Test
    @DisplayName("신규 가입인데 필수 약관 미동의 → TERMS_REQUIRED, 저장하지 않음")
    void newUser_withoutConsents() {
        given(kakaoOAuthClient.getUserInfo("auth-code", REDIRECT_URI)).willReturn(new KakaoUserInfo(KAKAO_ID, "홍윤지"));
        given(userRepository.findByKakaoId(KAKAO_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> authService.kakaoLogin(request(REDIRECT_URI, new Consents(true, false))))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.TERMS_REQUIRED);

        then(userRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("신규 가입 → 닉네임 변환해서 저장, isNewUser = true, onboardingCompleted = false")
    void newUser_signUp() {
        given(kakaoOAuthClient.getUserInfo("auth-code", REDIRECT_URI)).willReturn(new KakaoUserInfo(KAKAO_ID, "홍윤지"));
        given(userRepository.findByKakaoId(KAKAO_ID)).willReturn(Optional.empty());
        given(jwtProvider.createAccessToken(any(), any())).willReturn("jwt-token");

        LoginResponse response = authService.kakaoLogin(request(REDIRECT_URI, new Consents(true, true)));

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        then(userRepository).should().save(saved.capture());
        assertThat(saved.getValue().getNickname()).isEqualTo("윤지");
        assertThat(saved.getValue().getLastActiveAt()).isNotNull();

        assertThat(response.accessToken()).isEqualTo("jwt-token");
        assertThat(response.isNewUser()).isTrue();
        assertThat(response.onboardingCompleted()).isFalse();
        assertThat(response.role()).isEqualTo(Role.USER);
    }

    @Test
    @DisplayName("기존 회원 → consents 없어도 로그인, 닉네임 덮어쓰지 않음")
    void existingUser_login() {
        User existing = User.createKakaoUser(KAKAO_ID, "윤지"); // 사용자가 바꿔 둔 이름이라고 가정
        given(kakaoOAuthClient.getUserInfo("auth-code", REDIRECT_URI)).willReturn(new KakaoUserInfo(KAKAO_ID, "홍윤지"));
        given(userRepository.findByKakaoId(KAKAO_ID)).willReturn(Optional.of(existing));
        given(jwtProvider.createAccessToken(any(), any())).willReturn("jwt-token");

        LoginResponse response = authService.kakaoLogin(request(REDIRECT_URI, null));

        assertThat(response.isNewUser()).isFalse();
        assertThat(existing.getNickname()).isEqualTo("윤지");
    }

    @Test
    @DisplayName("카카오 호출 실패 → KAKAO_LOGIN_FAILED 그대로 전달")
    void kakaoFailed() {
        given(kakaoOAuthClient.getUserInfo("auth-code", REDIRECT_URI))
                .willThrow(new CustomException(ErrorCode.KAKAO_LOGIN_FAILED));

        assertThatThrownBy(() -> authService.kakaoLogin(request(REDIRECT_URI, new Consents(true, true))))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.KAKAO_LOGIN_FAILED);
    }
}