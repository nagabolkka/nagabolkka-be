package com.nagabolkka.backend.domain.practice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record StartPracticeRequest(
        @Schema(example = "1") @NotNull Long scenarioId
) {
}