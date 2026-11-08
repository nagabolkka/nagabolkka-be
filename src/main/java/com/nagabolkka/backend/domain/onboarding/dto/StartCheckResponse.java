package com.nagabolkka.backend.domain.onboarding.dto;

import com.nagabolkka.backend.domain.goal.entity.Goal;
import com.nagabolkka.backend.domain.region.entity.Region;
import io.swagger.v3.oas.annotations.media.Schema;

public record StartCheckResponse(

        @Schema(description = "시작 체크 점수 0~24", example = "2")
        int score,

        @Schema(description = "출발 지역")
        RegionInfo region,

        @Schema(description = "고른 목표")
        GoalInfo goal
) {

    public record RegionInfo(
            @Schema(example = "1") int stageNo,
            @Schema(example = "시작의 방") String name,
            @Schema(example = "집 안") String realLabel,
            @Schema(example = "빛의 조각") String pieceFamily
    ) {
        static RegionInfo from(Region region) {
            return new RegionInfo(region.getStageNo(), region.getName(), region.getRealLabel(), region.getPieceFamily());
        }
    }

    public record GoalInfo(
            @Schema(example = "3") Long goalId,
            @Schema(example = "모임") String name
    ) {
        static GoalInfo from(Goal goal) {
            return new GoalInfo(goal.getId(), goal.getName());
        }
    }

    public static StartCheckResponse of(int score, Region region, Goal goal) {
        return new StartCheckResponse(score, RegionInfo.from(region), GoalInfo.from(goal));
    }
}