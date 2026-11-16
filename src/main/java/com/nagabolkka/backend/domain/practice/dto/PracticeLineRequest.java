package com.nagabolkka.backend.domain.practice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PracticeLineRequest(
        @Schema(description = "user · partner", example = "partner")
        @NotNull @Pattern(regexp = "user|partner")
        String role,

        @Schema(example = "네, OO도서관입니다. 무엇을 도와드릴까요?")
        @NotNull @Size(max = 300)
        String text
) {
}