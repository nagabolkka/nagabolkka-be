package com.nagabolkka.backend.domain.practice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

// 대화 원문은 DB 에 없으므로 프론트가 지금까지의 대화를 같이 보냄 (턴 수는 서버가 셈)
public record TurnRequest(
        @Schema(description = "방금 한 말 (최대 200자)", example = "북토크 문의하려고 전화했어요.")
        @NotBlank @Size(max = 200)
        String message,

        @Schema(description = "지금까지의 대화 (첫 대사 포함 · 최대 7개)")
        @NotNull @Size(max = 7)
        List<@Valid PracticeLineRequest> history
) {
}