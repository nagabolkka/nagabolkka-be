package com.nagabolkka.backend.domain.quest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record QuestActionRequest(

        @Schema(description = "ACCEPT 수락 · SPLIT 쪼개기 · CHANGE 바꾸기 · SKIP 오늘은 쉬기 · REVISIT 지나온 계단 미션 받기",
                example = "ACCEPT")
        @NotNull
        QuestActionType type,

        @Schema(description = "REVISIT 일 때만 : 지나온 계단 번호", example = "1", nullable = true)
        @Min(1) @Max(5)
        Integer stageNo
) {

        // stageNo 없는 동작 (기존 코드 · 테스트 그대로)
        public QuestActionRequest(QuestActionType type) {
                this(type, null);
        }
}