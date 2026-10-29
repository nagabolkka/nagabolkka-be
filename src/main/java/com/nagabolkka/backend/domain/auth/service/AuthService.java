package com.nagabolkka.backend.domain.auth.service;

import com.nagabolkka.backend.domain.auth.dto.KakaoLoginRequest;
import com.nagabolkka.backend.domain.auth.dto.LoginResponse;
import com.nagabolkka.backend.domain.auth.kakao.KakaoOAuthClient;
import com.nagabolkka.backend.domain.auth.kakao.KakaoProperties;
import com.nagabolkka.backend.domain.auth.kakao.KakaoUserInfo;
import com.nagabolkka.backend.domain.user.entity.User;
import com.nagabolkka.backend.domain.user.repository.UserRepository;
import com.nagabolkka.backend.global.exception.CustomException;
import com.nagabolkka.backend.global.exception.ErrorCode;
import com.nagabolkka.backend.global.security.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final KakaoOAuthClient kakaoOAuthClient;
    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;
    private final KakaoProperties kakaoProperties;

    public LoginResponse kakaoLogin(KakaoLoginRequest request) {
        // 1) redirectUri 허용 목록 검사 (카카오 호출 전에 막는다)
        if (!kakaoProperties.allowedRedirectUris().contains(request.redirectUri())) {
            throw new CustomException(ErrorCode.INVALID_REDIRECT_URI);
        }

        // 2) 인가 코드 → 카카오 사용자 정보 (실패 시 401 KAKAO_LOGIN_FAILED)
        KakaoUserInfo kakaoUser = kakaoOAuthClient.getUserInfo(request.code(), request.redirectUri());

        // 3) 기존 회원이면 로그인, 없으면 가입
        Optional<User> existingUser = userRepository.findByKakaoId(kakaoUser.kakaoId());
        boolean isNewUser = existingUser.isEmpty();
        User user = existingUser.orElseGet(() -> signUp(kakaoUser, request));

        // 4) 마지막 활동 시각 갱신 후 저장 (신규면 INSERT, 기존이면 UPDATE)
        user.updateLastActiveAt(LocalDateTime.now());
        userRepository.save(user);

        // 5) JWT 발급
        String accessToken = jwtProvider.createAccessToken(user.getId(), user.getRole());
        return new LoginResponse(accessToken, isNewUser, user.isOnboardingCompleted(), user.getRole());
    }

    /** 신규 가입 : 필수 약관 동의 확인 → 닉네임 변환해서 생성 (기존 회원은 consents 를 보지 않는다) */
    private User signUp(KakaoUserInfo kakaoUser, KakaoLoginRequest request) {
        if (!request.agreedToRequiredTerms()) {
            throw new CustomException(ErrorCode.TERMS_REQUIRED);
        }
        return User.createKakaoUser(kakaoUser.kakaoId(), kakaoUser.nickname());
    }
}