package com.nagabolkka.backend.domain.quest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CompleteQuestRequest(

        @Schema(description = "기분 1 힘들었어요 ~ 5 좋았어요", example = "4")
        @NotNull @Min(1) @Max(5)
        Integer mood,

        @Schema(description = "한 줄 회고 (선택, 최대 100자)", example = "오랜만에 나갔는데 바람이 시원했다", nullable = true)
        @Size(max = 100)
        String reflection,

        @Schema(description = "쪼갠 단계 중 해낸 수 (쪼갠 퀘스트면 1 이상, 아니면 무시)", example = "0")
        @Min(0) @Max(3)
        Integer doneSteps
) {

    public int doneStepsOrZero() {
        return doneSteps == null ? 0 : doneSteps;
    }

    // 공백만 쓴 회고 = 회고 없음
    public String reflectionOrNull() {
        return reflection == null || reflection.isBlank() ? null : reflection.strip();
    }
}