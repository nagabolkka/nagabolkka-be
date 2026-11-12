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

class DailyQuestTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 13);
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 13, 10, 0);

    Region region;
    Mission indoor;   // 1칸 실내
    Mission outdoor;  // 2칸 실외
    DailyQuest quest;

    @BeforeEach
    void setUp() {
        region = Region.of(2, "바람의 숲", "문 앞", "바람의 조각", "#9DD49A", "a.png", "b.png");
        indoor = mission(1, "현관문 열고 1분 서 있기", true);
        outdoor = mission(2, "집 앞 골목 한 바퀴, 10분 산책", false);
        quest = DailyQuest.assign(User.createKakaoUser("홍윤지"), TODAY, indoor);
    }

    private Mission mission(int slotNo, String title, boolean isIndoor) {
        Piece piece = Piece.of(region, slotNo, "조각" + slotNo, PieceType.EASY, "복원 문구", "힌트");
        return Mission.of(region, piece, title, "설명", Difficulty.EASY, isIndoor, false, "장소" + slotNo, 5);
    }

    @Test
    @DisplayName("수락 : ASSIGNED → IN_PROGRESS, 수락 시각 저장")
    void accept() {
        quest.accept(NOW);

        assertThat(quest.getStatus()).isEqualTo(QuestStatus.IN_PROGRESS);
        assertThat(quest.getAcceptedAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("수락 두 번 : 에러 없이 그대로 (처음 수락 시각 유지)")
    void accept_twice() {
        quest.accept(NOW);
        quest.accept(NOW.plusMinutes(5));

        assertThat(quest.getStatus()).isEqualTo(QuestStatus.IN_PROGRESS);
        assertThat(quest.getAcceptedAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("쪼개기 : 수락까지 같이 → IN_PROGRESS · isSplit")
    void splitUp() {
        quest.splitUp(NOW);

        assertThat(quest.isSplit()).isTrue();
        assertThat(quest.getStatus()).isEqualTo(QuestStatus.IN_PROGRESS);
    }

    @Test
    @DisplayName("바꾸기 : 새 미션 · ASSIGNED · 쪼개기 초기화 · 남은 횟수 1")
    void change() {
        quest.splitUp(NOW);

        quest.change(outdoor);

        assertThat(quest.getMission()).isEqualTo(outdoor);
        assertThat(quest.getStatus()).isEqualTo(QuestStatus.ASSIGNED);
        assertThat(quest.isSplit()).isFalse();
        assertThat(quest.getAcceptedAt()).isNull();
        assertThat(quest.getPlaceName()).isEqualTo("장소2");
        assertThat(quest.getDistanceM()).isNull(); // 실외 → 거리 모름 (M4 전)
        assertThat(quest.getChangeRemaining()).isEqualTo(1);
    }

    @Test
    @DisplayName("바꾸기 3번째 → 409 QUEST_CHANGE_LIMIT")
    void change_limit() {
        quest.change(outdoor);
        quest.change(indoor);

        assertThatThrownBy(() -> quest.change(outdoor))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.QUEST_CHANGE_LIMIT);
    }

    @Test
    @DisplayName("쉬기 → SKIPPED, 그 뒤 수락 · 쪼개기 · 쉬기 → 409 QUEST_ALREADY_DONE")
    void skip_thenDone() {
        quest.skip();

        assertThat(quest.getStatus()).isEqualTo(QuestStatus.SKIPPED);
        assertThatThrownBy(() -> quest.accept(NOW))
                .extracting("errorCode").isEqualTo(ErrorCode.QUEST_ALREADY_DONE);
        assertThatThrownBy(() -> quest.splitUp(NOW))
                .extracting("errorCode").isEqualTo(ErrorCode.QUEST_ALREADY_DONE);
        assertThatThrownBy(() -> quest.skip())
                .extracting("errorCode").isEqualTo(ErrorCode.QUEST_ALREADY_DONE);
    }

    @Test
    @DisplayName("끝난 퀘스트 바꾸기 → 횟수보다 '이미 끝남' 이 먼저 (QUEST_ALREADY_DONE)")
    void change_afterSkip() {
        quest.change(outdoor);
        quest.change(indoor);
        quest.skip();

        assertThatThrownBy(() -> quest.change(outdoor))
                .extracting("errorCode").isEqualTo(ErrorCode.QUEST_ALREADY_DONE);
    }
}