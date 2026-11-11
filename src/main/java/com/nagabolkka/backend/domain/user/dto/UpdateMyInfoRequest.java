package com.nagabolkka.backend.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

// 마이페이지에서 바꿀 수 있는 건 목표 하나
public record UpdateMyInfoRequest(

        @Schema(description = "목표 id (1 취업 · 2 알바 · 3 모임 · 4 봉사 · 5 학업 복귀)", example = "3")
        @NotNull
        Long goalId
) {
}