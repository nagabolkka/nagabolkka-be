package com.nagabolkka.backend.domain.goal.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record CheckGoalStepRequest(
        @Schema(description = "true 체크 · false 해제", example = "true")
        @NotNull
        Boolean checked
) {
}