package com.nagabolkka.backend.domain.user.dto;

import com.nagabolkka.backend.domain.goal.entity.Goal;
import com.nagabolkka.backend.domain.region.entity.Region;
import com.nagabolkka.backend.domain.user.entity.Role;
import com.nagabolkka.backend.domain.user.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record MyInfoResponse(

        @Schema(description = "표시 이름", example = "윤지")
        String nickname,

        @Schema(description = "현재 지역 (시작 체크 전이면 null)", nullable = true)
        CurrentRegionInfo currentRegion,

        @Schema(description = "모은 조각 수 (조각 기능 전까지 0)", example = "0")
        int pieceCount,

        @Schema(description = "도감 진행 (찾은 칸 / 전체 25칸)", example = "0/25")
        String collection,

        @Schema(description = "사회 연결 목표 (시작 체크 전이면 null)", nullable = true)
        GoalInfo goal,

        @Schema(description = "구 id (시작 체크 전이면 null)", example = "1", nullable = true)
        Long districtId,

        @Schema(description = "기관 공유 동의 (값만 저장, 공유 기능은 확장 범위)", example = "false")
        boolean shareWithAgency,

        @Schema(description = "루미 플러스 설문 응답 (응답 전 · 플러스 기능 전까지 null)", nullable = true)
        PlusInterestInfo plusInterest,

        @Schema(description = "권한", example = "USER")
        Role role
) {

    // 도감 전체 칸 수 (5지역 × 5칸)
    private static final int TOTAL_SLOTS = 25;

    public record CurrentRegionInfo(
            @Schema(example = "1") int stageNo,
            @Schema(example = "집 안") String realLabel
    ) {
        static CurrentRegionInfo from(Region region) {
            return region == null ? null : new CurrentRegionInfo(region.getStageNo(), region.getRealLabel());
        }
    }

    public record GoalInfo(
            @Schema(example = "3") Long goalId,
            @Schema(example = "모임") String name
    ) {
        static GoalInfo from(Goal goal) {
            return goal == null ? null : new GoalInfo(goal.getId(), goal.getName());
        }
    }

    public record PlusInterestInfo(
            @Schema(example = "PRICE_1900") String priceOption,
            @Schema(example = "null", nullable = true) Integer customPrice,
            @Schema(example = "2026-11-01T14:00:00") LocalDateTime respondedAt
    ) {
    }

    public static MyInfoResponse from(User user) {
        int pieceCount = 0;
        int foundSlots = 0;

        return new MyInfoResponse(
                user.getNickname(),
                CurrentRegionInfo.from(user.getCurrentRegion()),
                pieceCount,
                foundSlots + "/" + TOTAL_SLOTS,
                GoalInfo.from(user.getCurrentGoal()),
                user.getDistrict() == null ? null : user.getDistrict().getId(),
                user.isShareWithAgency(),
                null,
                user.getRole()
        );
    }
}