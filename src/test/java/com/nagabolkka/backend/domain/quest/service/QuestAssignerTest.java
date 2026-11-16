package com.nagabolkka.backend.domain.quest.service;

import com.nagabolkka.backend.domain.mission.entity.Mission;
import com.nagabolkka.backend.domain.piece.entity.Piece;
import com.nagabolkka.backend.domain.piece.entity.PieceType;
import com.nagabolkka.backend.domain.region.entity.Region;
import com.nagabolkka.backend.global.common.Difficulty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class QuestAssignerTest {

    private final QuestAssigner assigner = new QuestAssigner();

    // 계단마다 쉬움 · 보통 · 도전 · 빗소리 미션 (5계단은 도전이 비 오는 날 미션)
    List<Mission> missions;

    @BeforeEach
    void setUp() {
        missions = new ArrayList<>();
        long pieceId = 1;
        for (int stage = 1; stage <= 5; stage++) {
            Region region = Region.of(stage, "지역" + stage, "라벨", "조각", "#000000", "a.png", "b.png");
            missions.add(mission(region, pieceId++, PieceType.EASY, Difficulty.EASY, false));
            missions.add(mission(region, pieceId++, PieceType.NORMAL, Difficulty.NORMAL, false));
            missions.add(mission(region, pieceId++, PieceType.CHALLENGE, Difficulty.CHALLENGE, stage == 5));
            if (stage <= 4) {
                missions.add(mission(region, pieceId++, PieceType.RAINY, Difficulty.EASY, true));
            }
        }
    }

    private Mission mission(Region region, long pieceId, PieceType type, Difficulty difficulty, boolean rainyOk) {
        Piece piece = Piece.of(region, null, region.getStageNo() + "-" + type, type, null, null);
        ReflectionTestUtils.setField(piece, "id", pieceId);
        return Mission.of(region, piece, region.getStageNo() + "계단 " + type, null,
                difficulty, true, rainyOk, "장소", 5);
    }

    private Piece pieceOf(int stage, PieceType type) {
        return missions.stream()
                .map(Mission::getRewardPiece)
                .filter(p -> p.getRegion().getStageNo() == stage && p.getType() == type)
                .findFirst().orElseThrow();
    }

    private List<Mission> stage(int stageNo) {
        return missions.stream().filter(m -> m.getRegion().getStageNo() == stageNo).toList();
    }

    @Test
    @DisplayName("보통 날 : 현재 계단(3)의 보통 미션")
    void normalDay() {
        Mission picked = assigner.pick(3, Difficulty.NORMAL, false, missions);

        assertThat(picked.getRegion().getStageNo()).isEqualTo(3);
        assertThat(picked.getDifficulty()).isEqualTo(Difficulty.NORMAL);
    }

    @Test
    @DisplayName("쉬운 날 : 지나온 계단이 있어도 현재 계단(3) 쉬움 미션")
    void easyDay_currentStage() {
        Mission picked = assigner.pick(3, Difficulty.EASY, false, missions);

        assertThat(picked.getRewardPiece()).isSameAs(pieceOf(3, PieceType.EASY));
    }

    @Test
    @DisplayName("맑은 날에는 빗소리 미션이 배정되지 않음")
    void clearDay_noRainyOnly() {
        for (Difficulty difficulty : Difficulty.values()) {
            assertThat(assigner.pick(3, difficulty, false, missions).isRainyOnly()).isFalse();
        }
    }

    @Test
    @DisplayName("비 오는 날 : 현재 계단 빗소리 미션 · 5계단은 도전 미션")
    void rainyDay() {
        assertThat(assigner.pick(3, Difficulty.NORMAL, true, missions).getRewardPiece())
                .isSameAs(pieceOf(3, PieceType.RAINY));
        assertThat(assigner.pick(5, Difficulty.EASY, true, missions).getRewardPiece())
                .isSameAs(pieceOf(5, PieceType.CHALLENGE));
    }

    @Test
    @DisplayName("지나온 계단 미션 받기 : 못 찾은 조각 중 가장 쉬운 난이도 (처음엔 쉬움)")
    void revisit_easiestUnfound() {
        Mission picked = assigner.pickRevisit(stage(1), Set.of(), List.of());

        assertThat(picked.getRewardPiece()).isSameAs(pieceOf(1, PieceType.EASY));
    }

    @Test
    @DisplayName("지나온 계단 미션 받기 : 쉬움 조각을 찾았으면 보통")
    void revisit_skipFound() {
        Mission picked = assigner.pickRevisit(stage(1), Set.of(pieceOf(1, PieceType.EASY).getId()), List.of());

        assertThat(picked.getRewardPiece()).isSameAs(pieceOf(1, PieceType.NORMAL));
    }

    @Test
    @DisplayName("지나온 계단 미션 받기 : 다 찾았으면 쉬움 · 빗소리 미션은 안 나옴")
    void revisit_allFound() {
        Set<Long> found = Set.of(pieceOf(1, PieceType.EASY).getId(), pieceOf(1, PieceType.NORMAL).getId(),
                pieceOf(1, PieceType.CHALLENGE).getId());

        Mission picked = assigner.pickRevisit(stage(1), found, List.of());

        assertThat(picked.getDifficulty()).isEqualTo(Difficulty.EASY);
        assertThat(picked.isRainyOnly()).isFalse();
    }
}