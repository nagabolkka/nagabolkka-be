package com.nagabolkka.backend.domain.auth.service;

import com.nagabolkka.backend.domain.auth.dto.KakaoLoginRequest;
import com.nagabolkka.backend.domain.auth.dto.KakaoLoginRequest.Consents;
import com.nagabolkka.backend.domain.auth.dto.LoginResponse;
import com.nagabolkka.backend.domain.auth.dto.TokenResponse;
import com.nagabolkka.backend.domain.auth.kakao.KakaoOAuthClient;
import com.nagabolkka.backend.domain.auth.kakao.KakaoProperties;
import com.nagabolkka.backend.domain.auth.kakao.KakaoUserInfo;
import com.nagabolkka.backend.domain.user.entity.Role;
import com.nagabolkka.backend.domain.user.entity.User;
import com.nagabolkka.backend.domain.user.repository.UserRepository;
import com.nagabolkka.backend.global.crypto.HashUtil;
import com.nagabolkka.backend.global.exception.CustomException;
import com.nagabolkka.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
    private static final String KAKAO_ID_HASH = "a".repeat(64);
    private static final KakaoUserInfo KAKAO_USER = new KakaoUserInfo(KAKAO_ID, "홍윤지");
    private static final TokenResponse TOKENS = new TokenResponse("access-token", 1800, "refresh-token");

    @Mock
    KakaoOAuthClient kakaoOAuthClient;
    @Mock
    UserRepository userRepository;
    @Mock
    HashUtil hashUtil;
    @Mock
    SignUpService signUpService;
    @Mock
    TokenService tokenService;

    AuthService authService;

    @BeforeEach
    void setUp() {
        KakaoProperties kakaoProperties = new KakaoProperties("rest-key", "", List.of(REDIRECT_URI));
        authService = new AuthService(kakaoOAuthClient, userRepository, hashUtil, signUpService, tokenService, kakaoProperties);
    }

    private KakaoLoginRequest request(String redirectUri, Consents consents) {
        return new KakaoLoginRequest("auth-code", redirectUri, consents);
    }

    private void givenKakaoUser(Optional<User> existing) {
        given(kakaoOAuthClient.getUserInfo("auth-code", REDIRECT_URI)).willReturn(KAKAO_USER);
        given(hashUtil.hmac(String.valueOf(KAKAO_ID))).willReturn(KAKAO_ID_HASH);
        given(userRepository.findByKakaoIdHash(KAKAO_ID_HASH)).willReturn(existing);
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
    @DisplayName("신규 가입인데 위치 약관 미동의 → TERMS_REQUIRED, 가입하지 않음")
    void newUser_withoutLocationConsent() {
        givenKakaoUser(Optional.empty());

        assertThatThrownBy(() -> authService.kakaoLogin(request(REDIRECT_URI, new Consents(true, true, false))))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.TERMS_REQUIRED);

        then(signUpService).should(never()).signUp(any(), any());
        then(tokenService).should(never()).issue(any());
    }

    @Test
    @DisplayName("신규 가입 → 해시로 가입, Access · Refresh 응답, isNewUser = true")
    void newUser_signUp() {
        givenKakaoUser(Optional.empty());
        User newUser = User.createKakaoUser("홍윤지");
        given(signUpService.signUp(KAKAO_USER, KAKAO_ID_HASH)).willReturn(newUser);
        given(tokenService.issue(newUser)).willReturn(TOKENS);

        LoginResponse response = authService.kakaoLogin(request(REDIRECT_URI, new Consents(true, true, true)));

        then(signUpService).should().signUp(KAKAO_USER, KAKAO_ID_HASH);
        assertThat(newUser.getLastActiveAt()).isNotNull();

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.accessTokenExpiresIn()).isEqualTo(1800);
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        assertThat(response.isNewUser()).isTrue();
        assertThat(response.onboardingCompleted()).isFalse();
        assertThat(response.role()).isEqualTo(Role.USER);
    }

    @Test
    @DisplayName("기존 회원 → consents 없어도 로그인, 가입하지 않음, 닉네임 덮어쓰지 않음")
    void existingUser_login() {
        User existing = User.createKakaoUser("윤지"); // 사용자가 바꿔 둔 이름이라고 가정
        givenKakaoUser(Optional.of(existing));
        given(tokenService.issue(existing)).willReturn(TOKENS);

        LoginResponse response = authService.kakaoLogin(request(REDIRECT_URI, null));

        then(signUpService).should(never()).signUp(any(), any());
        assertThat(response.isNewUser()).isFalse();
        assertThat(existing.getNickname()).isEqualTo("윤지");
    }

    @Test
    @DisplayName("카카오 호출 실패 → KAKAO_LOGIN_FAILED 그대로 전달")
    void kakaoFailed() {
        given(kakaoOAuthClient.getUserInfo("auth-code", REDIRECT_URI))
                .willThrow(new CustomException(ErrorCode.KAKAO_LOGIN_FAILED));

        assertThatThrownBy(() -> authService.kakaoLogin(request(REDIRECT_URI, new Consents(true, true, true))))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.KAKAO_LOGIN_FAILED);
    }
}