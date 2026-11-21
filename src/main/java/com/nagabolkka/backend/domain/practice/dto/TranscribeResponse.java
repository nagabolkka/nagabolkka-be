package com.nagabolkka.backend.domain.practice.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record TranscribeResponse(
        @Schema(description = "말한 내용 (못 알아들었거나 AI 실패면 null → 다시 말하기 · 직접 입력)",
                example = "네, 신청할게요. 몇 시까지 가면 될까요?", nullable = true)
        String text
) {
}