package com.nagabolkka.backend.domain.practice.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record FinishResponse(
        FeedbackItem feedback,
        PieceItem piece,
        @Schema(description = "연습 후 실제 미션 → 6번 PRACTICE_MISSION", nullable = true) FollowMission followMission
) {

    public record FeedbackItem(
            @Schema(description = "잘한 점 1~2개") List<String> good,
            @Schema(description = "다음에 해볼 말 1개") List<String> next
    ) {
    }

    public record PieceItem(
            @Schema(example = "24") Long pieceId,
            @Schema(example = "자신감 조각") String name,
            @Schema(description = "조각 계단 (색)", example = "5") int stageNo,
            @Schema(example = "새로운 세상") String regionName,
            @Schema(description = "처음 받은 조각 → NEW 뱃지", example = "true") boolean isNew
    ) {
    }

    public record FollowMission(
            @Schema(example = "77") Long missionId,
            @Schema(example = "도서관에 프로그램 문의 전화하기") String title
    ) {
    }
}