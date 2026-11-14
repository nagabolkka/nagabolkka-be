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
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class QuestAssignerRecentTest {

    QuestAssigner assigner = new QuestAssigner();
    List<Mission> missions;

    @BeforeEach
    void setUp() {
        Region stage1 = Region.of(1, "시작의 방", "집 안", "빛의 조각", "#E8C06A", "a.png", "b.png");
        ReflectionTestUtils.setField(stage1, "id", 1L);
        Piece sun = Piece.of(stage1, 1, "햇살 조각", PieceType.EASY, "복원 문구", "힌트");
        Piece morning = Piece.of(stage1, 2, "아침 조각", PieceType.NORMAL, "복원 문구", "힌트");

        // 쉬움 3개는 모두 햇살 조각 (같은 칸 공유)
        missions = List.of(
                mission(101L, stage1, sun, "쉬움 1", Difficulty.EASY),
                mission(102L, stage1, sun, "쉬움 2", Difficulty.EASY),
                mission(103L, stage1, sun, "쉬움 3", Difficulty.EASY),
                mission(201L, stage1, morning, "보통 1", Difficulty.NORMAL));
    }

    private Mission mission(Long id, Region region, Piece piece, String title, Difficulty difficulty) {
        Mission mission = Mission.of(region, piece, title, "설명", difficulty, true, false, "장소", 5);
        ReflectionTestUtils.setField(mission, "id", id);
        return mission;
    }

    private Mission pick(Difficulty difficulty, List<Long> recent) {
        return assigner.pick(1, difficulty, false, missions, Set.of(), Set.of(), recent);
    }

    @Test
    @DisplayName("최근 기록 없음 → id 순 첫 번째 (기존과 같음)")
    void noRecent() {
        assertThat(pick(Difficulty.EASY, List.of()).getId()).isEqualTo(101L);
    }

    @Test
    @DisplayName("어제 쉬움 1 → 쉬움 2, 그다음 쉬움 3 (한 적 없는 미션 먼저)")
    void notDoneFirst() {
        assertThat(pick(Difficulty.EASY, List.of(101L)).getId()).isEqualTo(102L);
        assertThat(pick(Difficulty.EASY, List.of(102L, 101L)).getId()).isEqualTo(103L);
    }

    @Test
    @DisplayName("셋 다 했으면 → 가장 오래전에 한 미션")
    void allDone_oldestFirst() {
        // 0번 = 가장 최근 (어제 103 · 그제 102 · 사흘 전 101)
        assertThat(pick(Difficulty.EASY, List.of(103L, 102L, 101L)).getId()).isEqualTo(101L);
        assertThat(pick(Difficulty.EASY, List.of(101L, 103L, 102L)).getId()).isEqualTo(102L);
    }

    @Test
    @DisplayName("오늘 난이도가 보통 → 보통 미션 (최근 기록과 상관없이 난이도가 먼저)")
    void difficultyFirst() {
        assertThat(pick(Difficulty.NORMAL, List.of(201L)).getId()).isEqualTo(201L);
    }
}