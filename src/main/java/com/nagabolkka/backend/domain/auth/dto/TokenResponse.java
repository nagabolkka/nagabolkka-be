package com.nagabolkka.backend.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record TokenResponse(

        @Schema(description = "JWT Access 토큰 (30분)")
        String accessToken,

        @Schema(description = "Access 유효시간 (초)", example = "1800")
        long accessTokenExpiresIn,

        @Schema(description = "Refresh 토큰 (14일, 재발급할 때마다 새 값으로 바뀜)")
        String refreshToken
) {
}