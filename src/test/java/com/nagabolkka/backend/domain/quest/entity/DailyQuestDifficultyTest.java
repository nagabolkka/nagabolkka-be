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

class DailyQuestDifficultyTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 25);
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 25, 10, 0);

    Region region;
    Mission easy;
    Mission normal;
    DailyQuest quest;

    @BeforeEach
    void setUp() {
        region = Region.of(1, "시작의 방", "집 안", "빛의 조각", "#E8C06A", "a.png", "b.png");
        easy = mission(1, PieceType.EASY, Difficulty.EASY, "창문 열고 바깥 공기 3분 마시기");
        normal = mission(2, PieceType.NORMAL, Difficulty.NORMAL, "오전 10시 전에 일어나 물 한 잔");
        quest = DailyQuest.assign(User.createKakaoUser("홍윤지"), TODAY, easy);
    }

    private Mission mission(int slotNo, PieceType type, Difficulty difficulty, String title) {
        Piece piece = Piece.of(region, slotNo, "조각" + slotNo, type, "복원 문구", "힌트");
        return Mission.of(region, piece, title, "설명", difficulty, true, false, "내 방", 5);
    }

    @Test
    @DisplayName("처음 배정 : 난이도 바꾸기 1번 남음")
    void remaining_initial() {
        assertThat(quest.getDifficultyChangeRemaining()).isEqualTo(1);
    }

    @Test
    @DisplayName("난이도 바꾸기 : 새 미션 · ASSIGNED · 난이도 횟수만 줄고 바꾸기 2번은 그대로")
    void changeDifficulty() {
        quest.changeDifficulty(normal);

        assertThat(quest.getMission()).isSameAs(normal);
        assertThat(quest.getStatus()).isEqualTo(QuestStatus.ASSIGNED);
        assertThat(quest.getSource()).isEqualTo(QuestSource.AUTO);
        assertThat(quest.getDifficultyChangeRemaining()).isZero();
        assertThat(quest.getChangeRemaining()).isEqualTo(DailyQuest.MAX_CHANGE);
    }

    @Test
    @DisplayName("두 번째 난이도 바꾸기 → 409 QUEST_DIFFICULTY_LIMIT")
    void changeDifficulty_twice() {
        quest.changeDifficulty(normal);

        assertThatThrownBy(() -> quest.changeDifficulty(easy))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.QUEST_DIFFICULTY_LIMIT);
    }

    @Test
    @DisplayName("난이도를 바꾼 뒤에도 바꾸기 (CHANGE) 는 쓸 수 있고, 난이도 횟수는 0 그대로")
    void change_afterDifficulty() {
        quest.changeDifficulty(normal);
        quest.change(easy);

        assertThat(quest.getChangeRemaining()).isEqualTo(DailyQuest.MAX_CHANGE - 1);
        assertThat(quest.getDifficultyChangeRemaining()).isZero();
    }

    @Test
    @DisplayName("수락한 뒤 → 409 QUEST_DIFFICULTY_LOCKED")
    void locked_afterAccept() {
        quest.accept(NOW);

        assertThatThrownBy(() -> quest.checkDifficultyChangeable())
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.QUEST_DIFFICULTY_LOCKED);
    }

    @Test
    @DisplayName("연습 후 미션 (source PRACTICE) → 409 QUEST_DIFFICULTY_LOCKED")
    void locked_practice() {
        quest.replaceWithPractice(normal, region);

        assertThatThrownBy(() -> quest.checkDifficultyChangeable())
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.QUEST_DIFFICULTY_LOCKED);
    }

    @Test
    @DisplayName("끝난 퀘스트 → 409 QUEST_ALREADY_DONE (LOCKED 보다 먼저)")
    void done() {
        quest.skip();

        assertThatThrownBy(() -> quest.checkDifficultyChangeable())
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.QUEST_ALREADY_DONE);
    }
}