package com.nagabolkka.backend.domain.auth.dto;

import com.nagabolkka.backend.domain.user.entity.Role;
import io.swagger.v3.oas.annotations.media.Schema;

public record LoginResponse(

        @Schema(description = "JWT access 토큰 (7일)")
        String accessToken,

        @Schema(description = "이번 요청으로 처음 가입했는지")
        boolean isNewUser,

        @Schema(description = "시작 체크 완료 여부 → false 면 시작 체크, true 면 홈")
        boolean onboardingCompleted,

        @Schema(description = "USER · ADMIN (ADMIN 이면 관리자 화면)")
        Role role
) {
}