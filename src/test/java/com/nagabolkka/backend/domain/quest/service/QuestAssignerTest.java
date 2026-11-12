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

    @Test
    @DisplayName("보통 날 : 현재 계단(3)의 보통 미션")
    void normalDay() {
        Mission picked = assigner.pick(3, Difficulty.NORMAL, false, missions, Set.of(1, 2), Set.of());

        assertThat(picked.getRegion().getStageNo()).isEqualTo(3);
        assertThat(picked.getDifficulty()).isEqualTo(Difficulty.NORMAL);
    }

    @Test
    @DisplayName("쉬운 날 + 지나온 계단 1 · 2 : 1계단 쉬움 미션부터")
    void easyDay_passedFirst() {
        Mission picked = assigner.pick(3, Difficulty.EASY, false, missions, Set.of(1, 2), Set.of());

        assertThat(picked.getRegion().getStageNo()).isEqualTo(1);
        assertThat(picked.getRewardPiece().getType()).isEqualTo(PieceType.EASY);
    }

    @Test
    @DisplayName("쉬운 날 : 이미 찾은 조각은 건너뛰고 다음 조각 (1계단 보통)")
    void easyDay_skipFound() {
        Set<Long> found = Set.of(pieceOf(1, PieceType.EASY).getId());

        Mission picked = assigner.pick(3, Difficulty.EASY, false, missions, Set.of(1, 2), found);

        assertThat(picked.getRewardPiece()).isSameAs(pieceOf(1, PieceType.NORMAL));
    }

    @Test
    @DisplayName("쉬운 날 : 지나온 계단 조각을 다 찾았으면 현재 계단 쉬움")
    void easyDay_allPassedFound() {
        Set<Long> found = Set.of(
                pieceOf(1, PieceType.EASY).getId(), pieceOf(1, PieceType.NORMAL).getId(),
                pieceOf(1, PieceType.CHALLENGE).getId(),
                pieceOf(2, PieceType.EASY).getId(), pieceOf(2, PieceType.NORMAL).getId(),
                pieceOf(2, PieceType.CHALLENGE).getId());

        Mission picked = assigner.pick(3, Difficulty.EASY, false, missions, Set.of(1, 2), found);

        assertThat(picked.getRewardPiece()).isSameAs(pieceOf(3, PieceType.EASY));
    }

    @Test
    @DisplayName("쉬운 날 + 지나온 계단 없음(1계단 출발) : 1계단 쉬움")
    void easyDay_noPassed() {
        Mission picked = assigner.pick(1, Difficulty.EASY, false, missions, Set.of(), Set.of());

        assertThat(picked.getRewardPiece()).isSameAs(pieceOf(1, PieceType.EASY));
    }

    @Test
    @DisplayName("맑은 날에는 빗소리 미션이 배정되지 않음 (지나온 계단 포함)")
    void clearDay_noRainyOnly() {
        for (Difficulty difficulty : Difficulty.values()) {
            Mission picked = assigner.pick(3, difficulty, false, missions, Set.of(1, 2), Set.of());
            assertThat(picked.isRainyOnly()).isFalse();
        }
    }

    @Test
    @DisplayName("비 오는 날 : 현재 계단 빗소리 미션 · 5계단은 도전 미션")
    void rainyDay() {
        assertThat(assigner.pick(3, Difficulty.NORMAL, true, missions, Set.of(1, 2), Set.of()).getRewardPiece())
                .isSameAs(pieceOf(3, PieceType.RAINY));
        assertThat(assigner.pick(5, Difficulty.EASY, true, missions, Set.of(), Set.of()).getRewardPiece())
                .isSameAs(pieceOf(5, PieceType.CHALLENGE));
    }
}