package com.nagabolkka.backend.domain.notice.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record NoticePushResponse(
        @Schema(description = "보낸 기기 수", example = "42") int sent,
        @Schema(description = "실패한 기기 수 (만료 토큰 포함)", example = "1") int failed
) {
}