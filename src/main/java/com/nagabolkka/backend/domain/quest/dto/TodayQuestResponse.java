package com.nagabolkka.backend.domain.quest.dto;

import com.nagabolkka.backend.domain.mission.entity.Mission;
import com.nagabolkka.backend.domain.mission.entity.MissionStep;
import com.nagabolkka.backend.domain.piece.entity.Piece;
import com.nagabolkka.backend.domain.quest.entity.DailyQuest;
import com.nagabolkka.backend.domain.quest.entity.QuestSource;
import com.nagabolkka.backend.domain.quest.entity.QuestStatus;
import com.nagabolkka.backend.global.common.Difficulty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record TodayQuestResponse(

        @Schema(example = "101") Long questId,
        @Schema(example = "2026-11-12") LocalDate questDate,
        @Schema(description = "ASSIGNED · IN_PROGRESS · COMPLETED · SKIPPED", example = "ASSIGNED") QuestStatus status,
        @Schema(description = "AUTO · SAVED_PLACE · PRACTICE", example = "AUTO") QuestSource source,
        @Schema(description = "미션의 지역 (지나온 계단 미션이면 그 계단)") RegionInfo region,
        @Schema(example = "창문 열고 바깥 공기 3분 마시기") String title,
        @Schema(example = "창문을 활짝 열고 바깥 소리를 들어봐요.") String description,
        @Schema(example = "EASY") Difficulty difficulty,
        @Schema(example = "true") boolean isIndoor,
        PlaceInfo place,
        @Schema(example = "3") int estMinutes,
        RewardInfo reward,
        @Schema(example = "false") boolean isSplit,
        @Schema(description = "쪼갠 뒤에만 채워짐 (미션별 고정 단계)") List<StepInfo> steps,
        @Schema(description = "오늘 남은 바꾸기 횟수", example = "2") int changeRemaining,
        @Schema(description = "기록 화면 쉼의 조각 안내 : 1계단 퀘스트 + 쉼의 조각을 아직 안 받음", example = "true") boolean restPieceAvailable
) {

    // 도보 시간 = 직선거리 × 1.3 ÷ 67m/분
    private static final double DETOUR = 1.3;
    private static final double WALK_M_PER_MIN = 67.0;

    public record RegionInfo(
            @Schema(example = "1") int stageNo,
            @Schema(example = "시작의 방") String name
    ) {
    }

    public record PlaceInfo(
            @Schema(example = "내 방 창가") String name,
            @Schema(description = "장소 좌표 (M4 카카오 로컬 연결 후)", nullable = true) BigDecimal lat,
            @Schema(nullable = true) BigDecimal lng,
            @Schema(description = "실내 0 · 모르면 null (M4 전 실외)", example = "0", nullable = true) Integer distanceM,
            @Schema(example = "0", nullable = true) Integer walkMinutes
    ) {
    }

    public record RewardInfo(
            @Schema(example = "1") Long pieceId,
            @Schema(example = "햇살 조각") String name,
            @Schema(example = "하늘에 색이 돌아왔어") String restoreText
    ) {
    }

    public record StepInfo(
            @Schema(example = "1") int stepNo,
            @Schema(example = "커튼 걷기") String content
    ) {
    }

    public static TodayQuestResponse of(DailyQuest quest, List<MissionStep> steps, boolean restPieceAvailable) {
        Mission mission = quest.getMission();
        Piece piece = mission.getRewardPiece();
        Integer distanceM = quest.getDistanceM();
        Integer walkMinutes = distanceM == null ? null
                : (int) Math.round(distanceM * DETOUR / WALK_M_PER_MIN);

        return new TodayQuestResponse(
                quest.getId(),
                quest.getQuestDate(),
                quest.getStatus(),
                quest.getSource(),
                new RegionInfo(quest.getRegion().getStageNo(), quest.getRegion().getName()),
                mission.getTitle(),
                mission.getDescription(),
                mission.getDifficulty(),
                mission.isIndoor(),
                new PlaceInfo(quest.getPlaceName(), null, null, distanceM, walkMinutes),
                mission.getEstMinutes(),
                new RewardInfo(piece.getId(), piece.getName(), piece.getRestoreText()),
                quest.isSplit(),
                steps.stream().map(s -> new StepInfo(s.getStepNo(), s.getContent())).toList(),
                quest.getChangeRemaining(),
                restPieceAvailable
        );
    }
}