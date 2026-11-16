package com.nagabolkka.backend.domain.practice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record FinishRequest(
        @Schema(description = "연습 대화 전체 (피드백용 · 1 ~ 7개)")
        @NotNull @Size(min = 1, max = 7)
        List<@Valid PracticeLineRequest> history
) {
}