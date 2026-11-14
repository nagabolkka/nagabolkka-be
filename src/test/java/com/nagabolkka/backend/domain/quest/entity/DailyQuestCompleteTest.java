package com.nagabolkka.backend.domain.quest.entity;

import com.nagabolkka.backend.domain.mission.entity.Mission;
import com.nagabolkka.backend.domain.piece.entity.Piece;
import com.nagabolkka.backend.domain.piece.entity.PieceType;
import com.nagabolkka.backend.domain.region.entity.Region;
import com.nagabolkka.backend.domain.user.entity.User;
import com.nagabolkka.backend.global.common.Difficulty;
import com.nagabolkka.backend.global.exception.CustomException;
import com.nagabolkka.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DailyQuestCompleteTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 14);
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 14, 15, 0);

    DailyQuest quest;

    @BeforeEach
    void setUp() {
        Region region = Region.of(1, "시작의 방", "집 안", "빛의 조각", "#F5D88E", "a.png", "b.png");
        Piece piece = Piece.of(region, 1, "햇살 조각", PieceType.EASY, "하늘에 색이 돌아왔어", "힌트");
        Mission mission = Mission.of(region, piece, "창문 열고 바깥 공기 3분 마시기", "설명",
                Difficulty.EASY, true, false, "내 방 창가", 3);
        quest = DailyQuest.assign(User.createKakaoUser("홍윤지"), TODAY, mission);
    }

    @Test
    @DisplayName("배정 상태에서 바로 완료 → COMPLETED · 완료 시각")
    void complete_fromAssigned() {
        quest.complete(0, NOW);

        assertThat(quest.getStatus()).isEqualTo(QuestStatus.COMPLETED);
        assertThat(quest.getCompletedAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("진행 중에서 완료")
    void complete_fromInProgress() {
        quest.accept(NOW);

        quest.complete(0, NOW.plusMinutes(10));

        assertThat(quest.getStatus()).isEqualTo(QuestStatus.COMPLETED);
    }

    @Test
    @DisplayName("쪼갠 퀘스트 : 0단계 → 400 SPLIT_STEP_REQUIRED, 2단계 → 완료 · 단계 수 저장")
    void complete_split() {
        quest.splitUp(NOW);

        assertThatThrownBy(() -> quest.complete(0, NOW))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.SPLIT_STEP_REQUIRED);

        quest.complete(2, NOW);
        assertThat(quest.getStatus()).isEqualTo(QuestStatus.COMPLETED);
        assertThat(quest.getSplitDoneSteps()).isEqualTo(2);
    }

    @Test
    @DisplayName("안 쪼갠 퀘스트는 단계 수 무시 (0 저장)")
    void complete_notSplit_ignoresSteps() {
        quest.complete(3, NOW);

        assertThat(quest.getSplitDoneSteps()).isZero();
    }

    @Test
    @DisplayName("완료 두 번 · 쉬기 후 완료 → 409 QUEST_ALREADY_DONE")
    void complete_whenDone() {
        quest.complete(0, NOW);
        assertThatThrownBy(() -> quest.complete(0, NOW))
                .extracting("errorCode").isEqualTo(ErrorCode.QUEST_ALREADY_DONE);

        setUp();
        quest.skip();
        assertThatThrownBy(() -> quest.complete(0, NOW))
                .extracting("errorCode").isEqualTo(ErrorCode.QUEST_ALREADY_DONE);
    }
}