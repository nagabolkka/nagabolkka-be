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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QuestAssignerDifficultyTest {

    private final QuestAssigner assigner = new QuestAssigner();

    private Region stage1;
    private Region stage2;
    private List<Mission> candidates;

    @BeforeEach
    void setUp() {
        stage1 = Region.of(1, "시작의 방", "집 안", "빛의 조각", "#E8C06A", "a.png", "b.png");
        stage2 = Region.of(2, "바람의 숲", "문 앞", "바람의 조각", "#9DD49A", "a.png", "b.png");
        candidates = List.of(
                mission(11, stage1, Difficulty.EASY, PieceType.EASY),
                mission(12, stage1, Difficulty.NORMAL, PieceType.NORMAL),
                mission(13, stage1, Difficulty.NORMAL, PieceType.NORMAL),
                mission(14, stage1, Difficulty.CHALLENGE, PieceType.CHALLENGE),
                mission(15, stage1, Difficulty.EASY, PieceType.RAINY),      // 빗소리 (쉬움)
                mission(21, stage2, Difficulty.NORMAL, PieceType.NORMAL));  // 다른 계단
    }

    private Mission mission(long id, Region region, Difficulty difficulty, PieceType type) {
        Piece piece = Piece.of(region, 1, "조각" + id, type, null, null);
        ReflectionTestUtils.setField(piece, "id", id);
        Mission mission = Mission.of(region, piece, "미션" + id, null, difficulty, true,
                type == PieceType.RAINY, "장소", 5);
        ReflectionTestUtils.setField(mission, "id", id);
        return mission;
    }

    @Test
    @DisplayName("고른 난이도 · 같은 계단 미션만 (다른 계단 제외), 한 적 없으면 id 순")
    void pick_target() {
        Mission picked = assigner.pickDifficulty(1, Difficulty.NORMAL, candidates, List.of());

        assertThat(picked.getId()).isEqualTo(12L);
    }

    @Test
    @DisplayName("최근에 한 미션은 뒤로 (12 를 어제 했으면 13)")
    void pick_leastRecent() {
        Mission picked = assigner.pickDifficulty(1, Difficulty.NORMAL, candidates, List.of(12L));

        assertThat(picked.getId()).isEqualTo(13L);
    }

    @Test
    @DisplayName("쉬움으로 바꿀 때 빗소리 미션은 고르지 않음")
    void pick_easy_noRainy() {
        Mission picked = assigner.pickDifficulty(1, Difficulty.EASY, candidates, List.of(11L));

        assertThat(picked.getId()).isEqualTo(11L); // 15 (빗소리) 는 후보 아님 → 최근에 했어도 11
    }

    @Test
    @DisplayName("그 계단에 그 난이도 미션이 없으면 시드 버그 → IllegalStateException")
    void pick_noSeed() {
        assertThatThrownBy(() -> assigner.pickDifficulty(2, Difficulty.CHALLENGE, candidates, List.of()))
                .isInstanceOf(IllegalStateException.class);
    }
}