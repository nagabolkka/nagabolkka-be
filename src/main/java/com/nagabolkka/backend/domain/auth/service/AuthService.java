package com.nagabolkka.backend.domain.auth.service;

import com.nagabolkka.backend.domain.auth.dto.KakaoLoginRequest;
import com.nagabolkka.backend.domain.auth.dto.LoginResponse;
import com.nagabolkka.backend.domain.auth.dto.TokenResponse;
import com.nagabolkka.backend.domain.auth.kakao.KakaoOAuthClient;
import com.nagabolkka.backend.domain.auth.kakao.KakaoProperties;
import com.nagabolkka.backend.domain.auth.kakao.KakaoUserInfo;
import com.nagabolkka.backend.domain.user.entity.User;
import com.nagabolkka.backend.domain.user.repository.UserRepository;
import com.nagabolkka.backend.global.crypto.HashUtil;
import com.nagabolkka.backend.global.exception.CustomException;
import com.nagabolkka.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final KakaoOAuthClient kakaoOAuthClient;
    private final UserRepository userRepository;
    private final HashUtil hashUtil;
    private final SignUpService signUpService;
    private final TokenService tokenService;
    private final KakaoProperties kakaoProperties;

    public LoginResponse kakaoLogin(KakaoLoginRequest request) {
        // 1) redirectUri 허용 목록 검사 (카카오 호출 전에 막는다)
        if (!kakaoProperties.allowedRedirectUris().contains(request.redirectUri())) {
            throw new CustomException(ErrorCode.INVALID_REDIRECT_URI);
        }

        // 2) 인가 코드 → 카카오 사용자 정보 (실패 시 401 KAKAO_LOGIN_FAILED)
        KakaoUserInfo kakaoUser = kakaoOAuthClient.getUserInfo(request.code(), request.redirectUri());

        // 3) 회원번호 해시로 조회 → 없으면 가입
        String kakaoIdHash = hashUtil.hmac(String.valueOf(kakaoUser.kakaoId()));
        Optional<User> existingUser = userRepository.findByKakaoIdHash(kakaoIdHash);
        boolean isNewUser = existingUser.isEmpty();
        User user = existingUser.orElseGet(() -> signUp(kakaoUser, kakaoIdHash, request));

        // 4) 마지막 활동 시각 갱신
        user.updateLastActiveAt(LocalDateTime.now());
        userRepository.save(user);

        // 5) Access + Refresh 발급
        TokenResponse tokens = tokenService.issue(user);
        return LoginResponse.of(tokens, isNewUser, user);
    }

    public TokenResponse refresh(String refreshToken) {
        return tokenService.rotate(refreshToken);
    }

    public void logout(String refreshToken) {
        tokenService.logout(refreshToken);
    }

    // 신규 가입 : 필수 약관(terms · privacy · location) 확인
    private User signUp(KakaoUserInfo kakaoUser, String kakaoIdHash, KakaoLoginRequest request) {
        if (!request.agreedToRequiredTerms()) {
            throw new CustomException(ErrorCode.TERMS_REQUIRED);
        }
        return signUpService.signUp(kakaoUser, kakaoIdHash);
    }
}