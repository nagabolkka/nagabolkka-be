package com.nagabolkka.backend.domain.safety.dto;

import com.nagabolkka.backend.domain.safety.entity.CrisisSource;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record SafetyEventRequest(
        @Schema(description = "MANUAL 만 허용 (회고 · 연습 위기 감지는 서버가 직접 기록)", example = "MANUAL")
        @NotNull
        CrisisSource source
) {
}