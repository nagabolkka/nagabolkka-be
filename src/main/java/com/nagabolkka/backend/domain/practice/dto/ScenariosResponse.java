package com.nagabolkka.backend.domain.practice.dto;

import com.nagabolkka.backend.domain.piece.entity.Piece;
import com.nagabolkka.backend.domain.practice.entity.PracticeScenario;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record ScenariosResponse(
        @Schema(description = "오늘 남은 연습 횟수 (하루 3회 · 시작 기준)", example = "3") int remainingToday,
        List<ScenarioItem> scenarios
) {

    public record ScenarioItem(
            @Schema(example = "1") Long scenarioId,
            @Schema(description = "FIRST_CALL · FIRST_VISIT · INTERVIEW", example = "FIRST_CALL") String code,
            @Schema(example = "첫 전화") String title,
            @Schema(example = "도서관에 프로그램 문의하기") String description,
            @Schema(example = "도서관 안내 직원") String partnerRole,
            @Schema(example = "3") int maxTurns,
            @Schema(example = "자신감 조각", nullable = true) String rewardPieceName,
            @Schema(description = "보상 조각 계단 (조각 색)", example = "5", nullable = true) Integer rewardStageNo,
            @Schema(description = "플러스 전용 → 잠금 표시", example = "false") boolean isPlus
    ) {

        public static ScenarioItem of(PracticeScenario scenario) {
            Piece piece = scenario.getRewardPiece();
            return new ScenarioItem(scenario.getId(), scenario.getCode(), scenario.getTitle(),
                    scenario.getDescription(), scenario.getPartnerRole(), scenario.getMaxTurns(),
                    piece == null ? null : piece.getName(),
                    piece == null || piece.getRegion() == null ? null : piece.getRegion().getStageNo(),
                    scenario.isPlus());
        }
    }
}