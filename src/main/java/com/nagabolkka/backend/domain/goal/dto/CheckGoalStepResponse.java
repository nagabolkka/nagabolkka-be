package com.nagabolkka.backend.domain.goal.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record CheckGoalStepResponse(
        @Schema(description = "체크한 단계 수", example = "4") int checked,
        @Schema(description = "전체 단계 수", example = "4") int total,
        @Schema(description = "이 목표를 처음 4/4 완성했을 때만, 아니면 null", nullable = true) HopePiece hopePiece
) {

    public record HopePiece(
            @Schema(example = "25") Long pieceId,
            @Schema(example = "희망 조각") String name,
            @Schema(description = "처음 받은 조각 → NEW 뱃지 (다른 목표로 이미 받았으면 false)", example = "true") boolean isNew
    ) {
    }
}