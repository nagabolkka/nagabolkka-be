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

class QuestAssignerChangeTest {

    private final QuestAssigner assigner = new QuestAssigner();

    private Region stage3;
    private List<Mission> candidates;

    @BeforeEach
    void setUp() {
        stage3 = Region.of(3, "발견의 마을", "동네", "발견의 조각", "#7FCDE6", "a.png", "b.png");
        candidates = List.of(
                mission(31, Difficulty.EASY, PieceType.EASY),
                mission(32, Difficulty.EASY, PieceType.EASY),
                mission(33, Difficulty.EASY, PieceType.EASY),
                mission(41, Difficulty.NORMAL, PieceType.NORMAL),
                mission(42, Difficulty.NORMAL, PieceType.NORMAL),
                mission(39, Difficulty.EASY, PieceType.RAINY)); // 빗소리 (쉬움)
    }

    private Mission mission(long id, Difficulty difficulty, PieceType type) {
        Piece piece = Piece.of(stage3, 1, "조각" + id, type, null, null);
        ReflectionTestUtils.setField(piece, "id", id);
        Mission mission = Mission.of(stage3, piece, "미션" + id, null, difficulty, true,
                type == PieceType.RAINY, "장소", 5);
        ReflectionTestUtils.setField(mission, "id", id);
        return mission;
    }

    private Mission byId(long id) {
        return candidates.stream().filter(m -> m.getId() == id).findFirst().orElseThrow();
    }

    @Test
    @DisplayName("같은 난이도 안에서 다음 미션 : 31 → 32 → 33")
    void next() {
        assertThat(assigner.pickChange(byId(31), candidates).getId()).isEqualTo(32L);
        assertThat(assigner.pickChange(byId(32), candidates).getId()).isEqualTo(33L);
    }

    @Test
    @DisplayName("마지막이면 처음으로 (빗소리 39 는 건너뜀) : 33 → 31")
    void wrap_skipRainy() {
        assertThat(assigner.pickChange(byId(33), candidates).getId()).isEqualTo(31L);
    }

    @Test
    @DisplayName("보통이면 보통끼리 : 41 → 42 → 41")
    void sameDifficulty() {
        assertThat(assigner.pickChange(byId(41), candidates).getDifficulty()).isEqualTo(Difficulty.NORMAL);
        assertThat(assigner.pickChange(byId(41), candidates).getId()).isEqualTo(42L);
        assertThat(assigner.pickChange(byId(42), candidates).getId()).isEqualTo(41L);
    }

    @Test
    @DisplayName("빗소리 미션을 바꾸면 같은 난이도 평소 미션")
    void fromRainy() {
        Mission changed = assigner.pickChange(byId(39), candidates);

        assertThat(changed.isRainyOnly()).isFalse();
        assertThat(changed.getDifficulty()).isEqualTo(Difficulty.EASY);
    }

    @Test
    @DisplayName("바꿀 미션이 없으면 시드 누락 → 예외")
    void noOption() {
        Mission only = byId(41);

        assertThatThrownBy(() -> assigner.pickChange(only, List.of(only)))
                .isInstanceOf(IllegalStateException.class);
    }
}