package com.nagabolkka.backend.domain.auth.controller;

import com.nagabolkka.backend.domain.auth.dto.KakaoLoginRequest;
import com.nagabolkka.backend.domain.auth.dto.LoginResponse;
import com.nagabolkka.backend.domain.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth", description = "카카오 로그인 API")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(
            summary = "카카오 로그인 및 가입",
            description = """
                    프론트가 받은 카카오 인가 코드로 로그인합니다. 처음이면 가입까지 처리합니다.
                    - 400 INVALID_REDIRECT_URI : 허용 목록에 없는 redirectUri
                    - 400 TERMS_REQUIRED : 신규 가입인데 필수 약관(terms · privacy) 미동의
                    - 401 KAKAO_LOGIN_FAILED : 인가 코드 만료 · 재사용, 카카오 장애 → 로그인 화면에서 다시 시도
                    """
    )
    @SecurityRequirements // 로그인 API 는 토큰 없이 호출
    @PostMapping("/kakao")
    public LoginResponse kakaoLogin(@Valid @RequestBody KakaoLoginRequest request) {
        return authService.kakaoLogin(request);
    }
}