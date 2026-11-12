package com.nagabolkka.backend.domain.quest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record QuestActionRequest(

        @Schema(description = "ACCEPT 수락 · SPLIT 쪼개기 · CHANGE 바꾸기 · SKIP 오늘은 쉬기", example = "ACCEPT")
        @NotNull
        QuestActionType type
) {
}