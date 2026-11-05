package com.nagabolkka.backend.domain.auth.dto;

import com.nagabolkka.backend.domain.user.entity.Role;
import com.nagabolkka.backend.domain.user.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;

public record LoginResponse(

        @Schema(description = "JWT Access 토큰 (30분)")
        String accessToken,

        @Schema(description = "Access 유효시간 (초)", example = "1800")
        long accessTokenExpiresIn,

        @Schema(description = "Refresh 토큰 (14일)")
        String refreshToken,

        @Schema(description = "이번 요청으로 처음 가입했는지")
        boolean isNewUser,

        @Schema(description = "시작 체크 완료 여부 → false 면 시작 체크, true 면 홈")
        boolean onboardingCompleted,

        @Schema(description = "USER · ADMIN (ADMIN 이면 관리자 화면)")
        Role role
) {

        public static LoginResponse of(TokenResponse tokens, boolean isNewUser, User user) {
                return new LoginResponse(
                        tokens.accessToken(),
                        tokens.accessTokenExpiresIn(),
                        tokens.refreshToken(),
                        isNewUser,
                        user.isOnboardingCompleted(),
                        user.getRole()
                );
        }
}