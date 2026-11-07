package com.nagabolkka.backend.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequest(

        @Schema(description = "로그인 · 재발급 때 받은 Refresh 토큰")
        @NotBlank
        String refreshToken
) {
}