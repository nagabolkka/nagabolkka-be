package com.nagabolkka.backend.domain.auth.controller;

import com.nagabolkka.backend.domain.auth.dto.KakaoLoginRequest;
import com.nagabolkka.backend.domain.auth.dto.LoginResponse;
import com.nagabolkka.backend.domain.auth.dto.RefreshTokenRequest;
import com.nagabolkka.backend.domain.auth.dto.TokenResponse;
import com.nagabolkka.backend.domain.auth.service.AuthService;
import com.nagabolkka.backend.global.exception.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "인증", description = "카카오 로그인 · 토큰 재발급 · 로그아웃")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "카카오 로그인 및 가입", description = "카카오 인가 코드로 로그인합니다. 처음이면 가입까지 처리하고 Access (30분) · Refresh (14일) 를 발급합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그인 성공",
                    content = @Content(schema = @Schema(implementation = LoginResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "INVALID_REDIRECT_URI", value = "{\"code\":\"INVALID_REDIRECT_URI\",\"message\":\"허용되지 않은 redirectUri 예요\"}"),
                            @ExampleObject(name = "TERMS_REQUIRED", description = "신규 가입인데 terms · privacy · location 중 미동의",
                                    value = "{\"code\":\"TERMS_REQUIRED\",\"message\":\"필수 약관에 동의해 주세요\"}"),
                            @ExampleObject(name = "INVALID_INPUT", value = "{\"code\":\"INVALID_INPUT\",\"message\":\"입력값을 다시 확인해 주세요\"}")
                    })),
            @ApiResponse(responseCode = "401", description = "카카오 로그인 실패 (인가 코드 만료 · 재사용, 카카오 장애)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples =
                    @ExampleObject(name = "KAKAO_LOGIN_FAILED", value = "{\"code\":\"KAKAO_LOGIN_FAILED\",\"message\":\"카카오 로그인에 실패했어요. 다시 시도해 주세요\"}")))
    })
    @SecurityRequirements
    @PostMapping("/kakao")
    public LoginResponse kakaoLogin(@Valid @RequestBody KakaoLoginRequest request) {
        return authService.kakaoLogin(request);
    }

    @Operation(summary = "토큰 재발급", description = "Access 만료 (401 AUTH_TOKEN_EXPIRED) 시 Refresh 로 새 토큰을 받습니다. 보낸 Refresh 는 폐기됩니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "재발급 성공",
                    content = @Content(schema = @Schema(implementation = TokenResponse.class))),
            @ApiResponse(responseCode = "401", description = "Refresh 없음 · 만료 · 이미 사용됨 → 토큰 삭제 후 로그인 화면",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples =
                    @ExampleObject(name = "AUTH_REFRESH_EXPIRED", value = "{\"code\":\"AUTH_REFRESH_EXPIRED\",\"message\":\"로그인이 만료됐어요. 다시 로그인해 주세요\"}")))
    })
    @SecurityRequirements
    @PostMapping("/refresh")
    public TokenResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return authService.refresh(request.refreshToken());
    }

    @Operation(summary = "로그아웃", description = "이 기기의 Refresh 를 삭제합니다. 없는 토큰이어도 204")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "로그아웃 완료", content = @Content),
            @ApiResponse(responseCode = "400", description = "refreshToken 누락",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples =
                    @ExampleObject(name = "INVALID_INPUT", value = "{\"code\":\"INVALID_INPUT\",\"message\":\"입력값을 다시 확인해 주세요\"}")))
    })
    @SecurityRequirements
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request.refreshToken());
    }
}