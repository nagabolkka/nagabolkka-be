package com.nagabolkka.backend.domain.push.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FcmTokenRequest(
        @Schema(description = "브라우저에서 받은 FCM 토큰") @NotBlank @Size(max = 512) String token) {
}