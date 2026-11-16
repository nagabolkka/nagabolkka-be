package com.nagabolkka.backend.domain.practice.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record StartPracticeResponse(
        @Schema(example = "55") Long sessionId,
        @Schema(description = "상대역 첫 대사", example = "네, OO도서관입니다. 무엇을 도와드릴까요?") String opening,
        @Schema(description = "선택지 2개") List<String> choices,
        @Schema(example = "3") int maxTurns
) {
}