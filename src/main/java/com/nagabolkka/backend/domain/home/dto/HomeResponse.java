package com.nagabolkka.backend.domain.home.dto;

import com.nagabolkka.backend.domain.goal.entity.Goal;
import com.nagabolkka.backend.domain.mission.entity.Mission;
import com.nagabolkka.backend.domain.quest.entity.DailyQuest;
import com.nagabolkka.backend.domain.quest.entity.QuestSource;
import com.nagabolkka.backend.domain.quest.entity.QuestStatus;
import com.nagabolkka.backend.domain.region.entity.Region;
import com.nagabolkka.backend.domain.region.entity.RegionStatus;
import com.nagabolkka.backend.domain.region.entity.UserRegion;
import com.nagabolkka.backend.global.common.Difficulty;
import com.nagabolkka.backend.global.weather.Weather;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record HomeResponse(

        @Schema(description = "날씨 (기상청 초단기실황 · 내 구 · 1시간 캐시, 실패하면 맑음)") Weather weather,
        CurrentRegion currentRegion,
        @Schema(description = "지금까지 모은 조각 수 (◆ 배지)", example = "8") long pieceCount,
        @Schema(description = "루미 한 줄 · 버튼 문구 분기 (DEFAULT · RAINY · SAVED_PLACE · IN_PROGRESS · COMPLETED · SKIPPED · WELCOME_BACK)",
                example = "DEFAULT") HomeState homeState,
        TodayQuest todayQuest,
        @Schema(description = "모험의 길 5칸 (PASSED = 지나온 곳)") List<RegionItem> regions,
        @Schema(description = "사회 연결 목표 (없으면 null)", nullable = true) GoalProgress goal,
        @Schema(description = "다음 지역 제안 (복원도 100% + 제안 주기일 때만, 아니면 null)", nullable = true)
        NextRegion nextRegionSuggestion
) {

    public record CurrentRegion(
            @Schema(example = "3") int stageNo,
            @Schema(example = "발견의 마을") String name,
            @Schema(example = "동네") String realLabel,
            @Schema(example = "#7FCDE6") String color,
            @Schema(description = "복원도 0~100 (완료 1회당 20)", example = "40") int restoration,
            @Schema(example = "/images/regions/3-discovery-village.png") String imageUrl,
            @Schema(example = "/images/regions/3-discovery-village-rain.png") String rainyImageUrl
    ) {
        static CurrentRegion from(UserRegion userRegion) {
            Region r = userRegion.getRegion();
            return new CurrentRegion(r.getStageNo(), r.getName(), r.getRealLabel(), r.getColor(),
                    userRegion.getRestoration(), r.getImageUrl(), r.getRainyImageUrl());
        }
    }

    public record TodayQuest(
            @Schema(example = "101") Long questId,
            @Schema(example = "ASSIGNED") QuestStatus status,
            @Schema(example = "AUTO") QuestSource source,
            @Schema(description = "미션의 계단 (지나온 계단 미션이면 현재보다 낮음) → 카드 조각 색", example = "1") int stageNo,
            @Schema(example = "창문 열고 바깥 공기 3분 마시기") String title,
            @Schema(example = "EASY") Difficulty difficulty,
            @Schema(example = "내 방 창가") String placeName,
            @Schema(description = "실내 0 · 모르면 null", example = "0", nullable = true) Integer distanceM,
            @Schema(example = "3") int estMinutes,
            @Schema(description = "진행 중이면 쪼개기 화면 / 진행 화면 분기", example = "false") boolean isSplit,
            Reward reward
    ) {
        static TodayQuest from(DailyQuest quest) {
            Mission mission = quest.getMission();
            return new TodayQuest(quest.getId(), quest.getStatus(), quest.getSource(),
                    quest.getRegion().getStageNo(), mission.getTitle(), mission.getDifficulty(),
                    quest.getPlaceName(), quest.getDistanceM(), mission.getEstMinutes(), quest.isSplit(),
                    new Reward(mission.getRewardPiece().getId(), mission.getRewardPiece().getName()));
        }
    }

    public record Reward(
            @Schema(example = "1") Long pieceId,
            @Schema(example = "햇살 조각") String name
    ) {
    }

    public record RegionItem(
            @Schema(example = "1") int stageNo,
            @Schema(example = "시작의 방") String name,
            @Schema(description = "LOCKED · ACTIVE · COMPLETED · PASSED", example = "PASSED") RegionStatus status,
            @Schema(example = "#E8C06A") String color,
            @Schema(example = "0") int restoration
    ) {
        static RegionItem from(UserRegion userRegion) {
            Region r = userRegion.getRegion();
            return new RegionItem(r.getStageNo(), r.getName(), userRegion.getStatus(), r.getColor(),
                    userRegion.getRestoration());
        }
    }

    public record GoalProgress(
            @Schema(example = "취업") String name,
            @Schema(description = "체크한 단계 수 (목표 체크 M5 전까지 0)", example = "0") int checked,
            @Schema(example = "4") int total
    ) {
        static GoalProgress of(Goal goal, int checked, int total) {
            return goal == null ? null : new GoalProgress(goal.getName(), checked, total);
        }
    }

    public record NextRegion(
            @Schema(example = "4") int stageNo,
            @Schema(example = "만남의 정원") String name
    ) {
    }

    public static HomeResponse of(Weather weather, UserRegion current, long pieceCount, HomeState homeState,
                                  DailyQuest quest, List<UserRegion> userRegions, Goal goal,
                                  int goalChecked, int goalTotal, Region nextRegion) {
        return new HomeResponse(
                weather,
                CurrentRegion.from(current),
                pieceCount,
                homeState,
                TodayQuest.from(quest),
                userRegions.stream().map(RegionItem::from).toList(),
                GoalProgress.of(goal, goalChecked, goalTotal),
                nextRegion == null ? null : new NextRegion(nextRegion.getStageNo(), nextRegion.getName())
        );
    }
}