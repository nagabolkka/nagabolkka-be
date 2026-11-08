package com.nagabolkka.backend.domain.onboarding.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record StartCheckRequest(

        @ArraySchema(schema = @Schema(type = "integer", minimum = "0", maximum = "3"),
                minItems = 8, maxItems = 8,
                arraySchema = @Schema(description = "8문항 답 (각 문항 선택지 순서 0~3)",
                        example = "[0, 0, 1, 0, 0, 1, 0, 0]"))
        @NotNull
        @Size(min = 8, max = 8)
        List<@NotNull @Min(0) @Max(3) Integer> answers,

        @Schema(description = "구 id (프론트가 GPS 로 판별, MVP 는 성북구 1)", example = "1")
        @NotNull
        Long districtId,

        @Schema(description = "결과 화면에서 고른 목표 id (1 취업 · 2 알바 · 3 모임 · 4 봉사 · 5 학업 복귀)", example = "3")
        @NotNull
        Long goalId
) {
}